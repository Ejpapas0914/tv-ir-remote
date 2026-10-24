package com.ejpapas.cobysmartremote.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.ejpapas.cobysmartremote.util.DiagLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume

data class DiscoveredDevice(val source: String, val name: String, val host: String, val detail: String)

/**
 * Passive discovery only: listens for mDNS announcements and SSDP replies on the local network.
 * It does not port-scan anything. A generic Hi3751 TV may announce nothing at all, in which case
 * the list is simply empty and you enter the IP by hand (check your router's device list).
 */
@Suppress("DEPRECATION")
class Discovery(private val context: Context) {
    private val serviceTypes = listOf(
        "_androidtvremote2._tcp.",
        "_googlecast._tcp.",
        "_adb._tcp.",
        "_adb-tls-connect._tcp.",
    )

    suspend fun run(durationMs: Long = 4000): List<DiscoveredDevice> = coroutineScope {
        val a = async { mdns(durationMs) }
        val b = async { ssdp(durationMs) }
        (a.await() + b.await()).distinctBy { "${it.source}|${it.host}|${it.name}" }
    }

    private suspend fun mdns(durationMs: Long): List<DiscoveredDevice> {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val found = CopyOnWriteArrayList<DiscoveredDevice>()
        val resolveLock = Mutex()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val listeners = serviceTypes.map { type ->
            val l = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    scope.launch {
                        resolveLock.withLock {
                            val r = resolve(nsd, info)
                            val host = r?.host?.hostAddress
                            if (r != null && host != null) {
                                found += DiscoveredDevice("mDNS", r.serviceName, host, "$type port ${r.port}")
                            }
                        }
                    }
                }
                override fun onServiceLost(info: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    DiagLog.log("mDNS start failed for $serviceType ($errorCode)")
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l) }
            l
        }
        delay(durationMs)
        listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
        scope.cancel()
        return found.toList()
    }

    private suspend fun resolve(nsd: NsdManager, info: NsdServiceInfo): NsdServiceInfo? =
        withTimeoutOrNull(2500) {
            suspendCancellableCoroutine { cont ->
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        if (cont.isActive) cont.resume(serviceInfo)
                    }
                })
            }
        }

    private suspend fun ssdp(durationMs: Long): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val out = mutableListOf<DiscoveredDevice>()
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("coby-ssdp").apply { setReferenceCounted(false); acquire() }
        try {
            DatagramSocket().use { sock ->
                sock.soTimeout = 500
                val msg = ("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n").toByteArray()
                sock.send(DatagramPacket(msg, msg.size, InetAddress.getByName("239.255.255.250"), 1900))
                val end = System.currentTimeMillis() + durationMs
                val buf = ByteArray(2048)
                while (System.currentTimeMillis() < end) {
                    try {
                        val p = DatagramPacket(buf, buf.size)
                        sock.receive(p)
                        val headers = String(p.data, 0, p.length).lines()
                            .mapNotNull { l -> l.indexOf(':').takeIf { it > 0 }?.let { l.substring(0, it).trim().uppercase() to l.substring(it + 1).trim() } }
                            .toMap()
                        val host = p.address.hostAddress ?: continue
                        out += DiscoveredDevice("SSDP", headers["SERVER"] ?: headers["ST"] ?: "UPnP device", host, headers["LOCATION"] ?: "")
                    } catch (_: SocketTimeoutException) {
                    }
                }
            }
        } catch (e: Exception) {
            DiagLog.log("SSDP failed: ${e.message}")
        } finally {
            lock.release()
        }
        out
    }
}
