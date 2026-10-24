package com.ejpapas.cobysmartremote.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ejpapas.cobysmartremote.data.Settings
import com.ejpapas.cobysmartremote.util.DiagLog
import java.net.Inet4Address

private fun networkSummary(ctx: Context): String {
    val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return "Unknown"
    val n = cm.activeNetwork ?: return "No active network"
    val caps = cm.getNetworkCapabilities(n)
    val kind = when {
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile data (the TV is not reachable this way)"
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
        else -> "Other"
    }
    val ip = cm.getLinkProperties(n)?.linkAddresses?.firstOrNull { it.address is Inet4Address }?.address?.hostAddress
    return "$kind, phone address ${ip ?: "unknown"}"
}

@Composable
fun DiagnosticsScreen(ui: UiState, s: Settings, vm: RemoteViewModel) {
    val ctx = LocalContext.current
    val net = remember(ui.connection, ui.probe) { networkSummary(ctx) }
    val logs by DiagLog.lines.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Diagnostics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Infrared", fontWeight = FontWeight.SemiBold)
                Text(
                    when (ui.irAvailable) {
                        true -> "Emitter available to apps. Carrier ranges: ${ui.irRanges}"
                        false -> "No IR emitter is exposed to apps on this phone, so IR control is not possible here."
                        null -> "Not checked"
                    }
                )
                OutlinedButton(onClick = { vm.refreshIr() }) { Text("Re-check IR") }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Network", fontWeight = FontWeight.SemiBold)
                Text(net)
                Text("TV address: ${s.tvIp}")
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Wi-Fi service check (only your TV's address)", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { vm.runProbe() }, enabled = !ui.probing) {
                    Text(if (ui.probing) "Probing…" else "Probe ports")
                }
                if (ui.probe.isEmpty()) {
                    Text("No results yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    ui.probe.forEach { r ->
                        Row {
                            Text(
                                "${r.port}  ${r.service}: ${if (r.open) "OPEN" else "closed"}",
                                color = if (r.open) CobyGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "Open only means something is listening. It does not prove the TV will accept commands.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pairing / authorization", fontWeight = FontWeight.SemiBold)
                Text("${statusLabel(ui.connection)}: ${ui.connectionDetail}")
                Text("Verified buttons: ${s.verified.size} of ${com.ejpapas.cobysmartremote.model.RemoteKey.entries.size}")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                OutlinedButton(onClick = { DiagLog.clear() }) { Text("Clear") }
            }
        }
        items(logs.reversed()) { line ->
            Text(line, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
