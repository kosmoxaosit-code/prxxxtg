package com.tgwsproxy

import android.content.Context

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("proxy_settings", Context.MODE_PRIVATE)

    fun getConfig(): ProxyConfig {
        val bindAddress = prefs.getString(KEY_BIND_ADDRESS, "127.0.0.1") ?: "127.0.0.1"
        val port = prefs.getInt(KEY_PORT, 1080)
        return ProxyConfig(bindAddress = bindAddress, port = port)
    }

    fun saveConfig(config: ProxyConfig) {
        prefs.edit()
            .putString(KEY_BIND_ADDRESS, config.bindAddress)
            .putInt(KEY_PORT, config.port)
            .apply()
    }

    fun resetDefaults() = saveConfig(ProxyConfig())

    companion object {
        private const val KEY_BIND_ADDRESS = "bind_address"
        private const val KEY_PORT = "port"
    }
}
