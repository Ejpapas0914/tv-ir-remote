package com.ejpapas.cobysmartremote.util

object NetUtil {
    /** True only for RFC1918 private IPv4 addresses, so the app never talks to public hosts. */
    fun isPrivateIpv4(host: String): Boolean {
        val parts = host.trim().split(".")
        if (parts.size != 4) return false
        val n = parts.map { it.toIntOrNull() ?: return false }
        if (n.any { it !in 0..255 }) return false
        return n[0] == 10 ||
            (n[0] == 172 && n[1] in 16..31) ||
            (n[0] == 192 && n[1] == 168)
    }
}
