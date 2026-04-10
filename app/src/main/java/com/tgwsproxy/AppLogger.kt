package com.tgwsproxy

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object AppLogger {
    private const val LIMIT = 200
    private val logs = CopyOnWriteArrayList<String>()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun info(message: String) = log("INFO", message)
    fun error(message: String) = log("ERROR", message)

    private fun log(level: String, message: String) {
        val safe = message.replace(Regex("(?i)(password|token|secret)=[^\")]*(,|$)"), "$1=***$2")
        logs.add("${formatter.format(Date())} [$level] $safe")
        while (logs.size > LIMIT) {
            logs.removeAt(0)
        }
    }

    fun dump(): String = logs.joinToString("\n")

    fun clear() = logs.clear()
}
