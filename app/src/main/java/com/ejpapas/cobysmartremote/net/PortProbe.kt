package com.ejpapas.cobysmartremote.net

import com.ejpapas.cobysmartremote.util.DiagLog
import com.ejpapas.cobysmartremote.util.NetUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class PortResult(val port: Int, val service: String, val open: Boolean)

/**
 * Checks a short, fixed list of well-known remote-control ports on ONE host: the TV address
 * you entered. It never sweeps a subnet and refuses non-private addresses.
 * An open port only means something is listening; it does not prove the TV accepts commands.
 */
object PortProbe {
    private val PORTS = listOf(
        5555 to "ADB over network",
        6466 to "Android TV Remote v2 (pairing)",
        6467 to "Android TV Remote v2 (control)",
        8008 to "Google Cast HTTP",
        8009 to "Google Cast",
        8060 to "Roku-style ECP",
        8001 to "Samsung-style WebSocket",
        3000 to "LG-style WebSocket",
        80 to "HTTP",
        8080 to "HTTP (alt)",
    )

    suspend fun probe(host: String, timeoutMs: Int = 900): List<PortResult> {
        if (!NetUtil.isPrivateIpv4(host)) {
            DiagLog.log("Probe refused: $host is not a private IPv4 address")
            return emptyList()
        }
        return withContext(Dispatchers.IO) {
            coroutineScope {
                PORTS.map { (port, name) ->
                    async {
                        val open = try {
                            Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs); true }
                        } catch (e: Exception) {
                            false
                        }
                        PortResult(port, name, open)
                    }
                }.awaitAll()
            }
        }
    }
}
