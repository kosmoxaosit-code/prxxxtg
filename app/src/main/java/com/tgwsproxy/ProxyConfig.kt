package com.tgwsproxy

data class ProxyConfig(
    val bindAddress: String = "127.0.0.1",
    val port: Int = 1080,
)
