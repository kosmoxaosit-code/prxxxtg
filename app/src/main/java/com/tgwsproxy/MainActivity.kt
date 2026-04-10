package com.tgwsproxy

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var lastErrorText: TextView
    private lateinit var logsText: TextView
    private lateinit var bindAddressEdit: EditText
    private lateinit var portEdit: EditText

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ProxyService.ACTION_STATUS) return
            val status = intent.getStringExtra(ProxyService.EXTRA_STATUS) ?: ProxyStatus.STOPPED.name
            val message = intent.getStringExtra(ProxyService.EXTRA_MESSAGE) ?: ""
            statusText.text = status.lowercase()
            if (status == ProxyStatus.ERROR.name) {
                lastErrorText.text = message
            }
            logsText.text = AppLogger.dump()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        lastErrorText = findViewById(R.id.lastErrorText)
        logsText = findViewById(R.id.logsText)
        bindAddressEdit = findViewById(R.id.bindAddressEdit)
        portEdit = findViewById(R.id.portEdit)

        val settings = SettingsRepository(this)
        val config = settings.getConfig()
        bindAddressEdit.setText(config.bindAddress)
        portEdit.setText(config.port.toString())

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            if (!saveConfigIfValid(settings)) return@setOnClickListener
            sendServiceAction(ProxyService.ACTION_START)
        }
        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            sendServiceAction(ProxyService.ACTION_STOP)
        }
        findViewById<Button>(R.id.restartBtn).setOnClickListener {
            if (!saveConfigIfValid(settings)) return@setOnClickListener
            sendServiceAction(ProxyService.ACTION_RESTART)
        }

        findViewById<Button>(R.id.applyTelegramBtn).setOnClickListener {
            openTelegram(settings.getConfig())
        }
        findViewById<Button>(R.id.copyBtn).setOnClickListener {
            val cfg = settings.getConfig()
            val text = "SOCKS5 ${cfg.bindAddress}:${cfg.port}"
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("proxy", text))
            toast("Copied: $text")
        }
        findViewById<Button>(R.id.clearLogsBtn).setOnClickListener {
            AppLogger.clear()
            logsText.text = ""
        }
        findViewById<Button>(R.id.resetDefaultsBtn).setOnClickListener {
            settings.resetDefaults()
            val c = settings.getConfig()
            bindAddressEdit.setText(c.bindAddress)
            portEdit.setText(c.port.toString())
            toast("Defaults restored")
        }

        logsText.text = AppLogger.dump()
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, IntentFilter(ProxyService.ACTION_STATUS), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, IntentFilter(ProxyService.ACTION_STATUS))
        }
    }

    override fun onStop() {
        unregisterReceiver(statusReceiver)
        super.onStop()
    }

    private fun saveConfigIfValid(settings: SettingsRepository): Boolean {
        val host = bindAddressEdit.text.toString().trim()
        val port = portEdit.text.toString().toIntOrNull()
        if (host != "127.0.0.1" && host != "::1") {
            lastErrorText.text = "Bind address must stay localhost-only"
            return false
        }
        if (port == null || port !in 1..65535) {
            lastErrorText.text = "Invalid port"
            return false
        }
        settings.saveConfig(ProxyConfig(bindAddress = host, port = port))
        return true
    }

    private fun sendServiceAction(action: String) {
        startForegroundService(Intent(this, ProxyService::class.java).setAction(action))
    }

    private fun openTelegram(config: ProxyConfig) {
        val uri = Uri.parse("tg://socks?server=${config.bindAddress}&port=${config.port}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val canOpen = packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        if (canOpen) {
            runCatching { startActivity(intent) }
                .onFailure { toast("Deep-link failed. Use manual setup.") }
        } else {
            toast("Telegram not found. Use manual setup.")
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
