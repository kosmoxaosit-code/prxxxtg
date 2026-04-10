package com.tgwsproxy

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalSocksProxy(private val config: ProxyConfig) {
    private val running = AtomicBoolean(false)
    private val clientPool = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        val bind = InetAddress.getByName(config.bindAddress)
        serverSocket = ServerSocket(config.port, 50, bind)
        AppLogger.info("SOCKS5 started at ${config.bindAddress}:${config.port}")

        while (running.get()) {
            val client = serverSocket?.accept() ?: break
            clientPool.execute { handleClient(client) }
        }
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        clientPool.shutdownNow()
        AppLogger.info("SOCKS5 stopped")
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            try {
                val input = socket.getInputStream()
                val output = socket.getOutputStream()

                val version = input.read()
                if (version != 0x05) return
                val methods = input.read()
                if (methods <= 0) return
                repeat(methods) { input.read() }
                output.write(byteArrayOf(0x05, 0x00))
                output.flush()

                val reqVersion = input.read()
                val cmd = input.read()
                input.read() // reserved
                val atyp = input.read()
                if (reqVersion != 0x05 || cmd != 0x01) {
                    output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                    output.flush()
                    return
                }

                val targetHost = when (atyp) {
                    0x01 -> ByteArray(4).also { input.readFully(it) }.joinToString(".") { (it.toInt() and 0xff).toString() }
                    0x03 -> {
                        val len = input.read()
                        ByteArray(len).also { input.readFully(it) }.toString(Charsets.UTF_8)
                    }
                    else -> {
                        output.write(byteArrayOf(0x05, 0x08, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                        output.flush()
                        return
                    }
                }

                val portHigh = input.read()
                val portLow = input.read()
                val targetPort = (portHigh shl 8) or portLow

                val remote = Socket()
                remote.connect(InetSocketAddress(targetHost, targetPort), 10000)

                output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 127, 0, 0, 1, 0x04, 0x38))
                output.flush()

                tunnel(socket.getInputStream(), remote.getOutputStream(), remote)
                tunnel(remote.getInputStream(), socket.getOutputStream(), socket)
            } catch (e: Exception) {
                AppLogger.error("Client handling error: ${e.message}")
            }
        }
    }

    private fun tunnel(input: InputStream, output: OutputStream, peer: Socket) {
        clientPool.execute {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            try {
                while (running.get()) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    output.flush()
                }
            } catch (_: Exception) {
            } finally {
                runCatching { peer.close() }
            }
        }
    }

    private fun InputStream.readFully(bytes: ByteArray) {
        var read = 0
        while (read < bytes.size) {
            val current = read(bytes, read, bytes.size - read)
            if (current <= 0) throw IllegalStateException("Unexpected EOF")
            read += current
        }
    }
}
