package com.ejpapas.cobysmartremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ejpapas.cobysmartremote.data.Method
import com.ejpapas.cobysmartremote.data.Settings
import com.ejpapas.cobysmartremote.util.NetUtil

@Composable
fun statusColor(state: ConnState): Color = when (state) {
    ConnState.CONNECTED -> CobyGreen
    ConnState.CONNECTING -> CobyAmber
    ConnState.FAILED -> CobyRed
    ConnState.DISCONNECTED -> Color.Gray
}

fun statusLabel(state: ConnState): String = when (state) {
    ConnState.CONNECTED -> "Connected"
    ConnState.CONNECTING -> "Connecting…"
    ConnState.FAILED -> "Connection problem"
    ConnState.DISCONNECTED -> "Disconnected"
}

@Composable
fun HomeScreen(ui: UiState, s: Settings, vm: RemoteViewModel) {
    var ipText by remember(s.tvIp) { mutableStateOf(s.tvIp) }
    val valid = NetUtil.isPrivateIpv4(ipText)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Coby Smart Remote", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(statusLabel(ui.connection), color = statusColor(ui.connection), fontWeight = FontWeight.Bold)
                Text(ui.connectionDetail, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Connection method", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Method.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = s.method == m,
                    onClick = { vm.setMethod(m) },
                    shape = SegmentedButtonDefaults.itemShape(i, Method.entries.size),
                ) { Text(m.label) }
            }
        }

        OutlinedTextField(
            value = ipText,
            onValueChange = { ipText = it },
            label = { Text("TV IP address") },
            isError = !valid,
            supportingText = {
                Text(
                    if (valid) "Local address. The TV's IP can change; update it if connecting fails."
                    else "Enter a private IPv4 address, e.g. 192.168.x.x"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val connected = ui.connection == ConnState.CONNECTED || ui.connection == ConnState.CONNECTING
            if (connected) {
                Button(
                    onClick = { vm.disconnect() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) { Text("Disconnect") }
            } else {
                Button(
                    onClick = { vm.connect(ipText) },
                    enabled = valid || s.method == Method.IR,
                    modifier = Modifier.weight(1f),
                ) { Text("Connect") }
            }
            OutlinedButton(onClick = { vm.runDiscovery() }, enabled = !ui.discovering, modifier = Modifier.weight(1f)) {
                Text(if (ui.discovering) "Listening…" else "Find TV")
            }
        }

        if (ui.discovered.isNotEmpty()) {
            Text("Announcements heard on your network (tap to use its address)", style = MaterialTheme.typography.labelLarge)
            ui.discovered.forEach { d ->
                Card(
                    Modifier.fillMaxWidth().clickable { ipText = d.host },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${d.name}  ·  ${d.host}", fontWeight = FontWeight.SemiBold)
                        Text("${d.source}  ${d.detail}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else if (!ui.discovering) {
            Text(
                "Many generic TVs announce nothing. If \"Find TV\" shows nothing, read the TV's IP from your router's device list.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Haptic feedback", modifier = Modifier.weight(1f))
            Switch(checked = s.haptics, onCheckedChange = { vm.setHaptics(it) })
        }
    }
}
