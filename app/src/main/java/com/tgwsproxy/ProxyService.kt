package com.tgwsproxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.net.BindException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

class ProxyService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private var proxyFuture: Future<*>? = null
    private val proxyRef = AtomicReference<LocalSocksProxy?>(null)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProxy()
            ACTION_STOP -> stopProxy()
            ACTION_RESTART -> {
                stopProxy()
                startProxy()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopProxy()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startProxy() {
        if (proxyFuture?.isDone == false) {
            AppLogger.info("Start ignored: already running")
            return
        }

        updateStatus(ProxyStatus.STARTING, "Starting")
        startForeground(NOTIFICATION_ID, buildNotification("Starting"))

        proxyFuture = executor.submit {
            try {
                val config = SettingsRepository(this).getConfig()
                require(config.port in 1..65535) { "Invalid port ${config.port}" }
                require(config.bindAddress == "127.0.0.1" || config.bindAddress == "::1") {
                    "Bind address must be localhost only"
                }

                val proxy = LocalSocksProxy(config)
                proxyRef.set(proxy)
                updateStatus(ProxyStatus.RUNNING, "Running on ${config.bindAddress}:${config.port}")
                startForeground(NOTIFICATION_ID, buildNotification("Running ${config.bindAddress}:${config.port}"))
                proxy.start()
                updateStatus(ProxyStatus.STOPPED, "Stopped")
            } catch (e: BindException) {
                onError("Port busy: ${e.message}")
            } catch (e: Exception) {
                onError("Start failed: ${e.message}")
            }
        }
    }

    private fun stopProxy() {
        proxyRef.getAndSet(null)?.stop()
        proxyFuture?.cancel(true)
        updateStatus(ProxyStatus.STOPPED, "Stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onError(message: String) {
        AppLogger.error(message)
        updateStatus(ProxyStatus.ERROR, message)
        startForeground(NOTIFICATION_ID, buildNotification("Error"))
    }

    private fun updateStatus(status: ProxyStatus, message: String) {
        AppLogger.info(message)
        val statusIntent = Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_STATUS, status.name)
            putExtra(EXTRA_MESSAGE, message)
        }
        sendBroadcast(statusIntent)
    }

    private fun buildNotification(content: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Proxy", NotificationManager.IMPORTANCE_LOW)
            )
        }

        val openIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, ProxyService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val restartIntent = PendingIntent.getService(
            this,
            102,
            Intent(this, ProxyService::class.java).setAction(ACTION_RESTART),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("TG WS Proxy")
            .setContentText(content)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .addAction(0, "Restart", restartIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.tgwsproxy.action.START"
        const val ACTION_STOP = "com.tgwsproxy.action.STOP"
        const val ACTION_RESTART = "com.tgwsproxy.action.RESTART"
        const val ACTION_STATUS = "com.tgwsproxy.action.STATUS"
        const val EXTRA_STATUS = "status"
        const val EXTRA_MESSAGE = "message"

        private const val CHANNEL_ID = "proxy_channel"
        private const val NOTIFICATION_ID = 70
    }
}
