package com.ejpapas.cobysmartremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ejpapas.cobysmartremote.data.Method
import com.ejpapas.cobysmartremote.data.Settings
import com.ejpapas.cobysmartremote.model.RemoteKey

@Composable
fun SetupScreen(ui: UiState, s: Settings, vm: RemoteViewModel) {
    // "Start here" key first, then the rest in enum order.
    val ordered = listOf(RemoteKey.VOLUME_UP) + RemoteKey.entries.filter { it != RemoteKey.VOLUME_UP }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Device setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Method: ${s.method.label}   ·   TV: ${s.tvIp}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (s.method == Method.WIFI_ADB) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TV-side setup (Wi-Fi / ADB)", fontWeight = FontWeight.Bold)
                        Text(
                            "1. On the TV open Settings → About and select \"Android Version\"/\"Build number\" repeatedly (usually 7 times) to unlock Developer options.\n" +
                                "2. In Developer options turn on USB debugging and, if present, network/ADB debugging.\n" +
                                "3. Keep the phone and TV on the same Wi-Fi. Press Connect on Home.\n" +
                                "4. When the TV asks \"Allow USB debugging?\" for this phone, tick \"Always allow\" and accept.\n\n" +
                                "If the TV has no Developer options or no network debugging, Wi-Fi control is not possible on this firmware; use Infrared.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedButton(onClick = { vm.runProbe() }, enabled = !ui.probing) {
                            Text(if (ui.probing) "Probing…" else "Check which ports the TV answers on")
                        }
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Infrared profile", fontWeight = FontWeight.Bold)
                        Text(
                            "This app ships with no IR codes. Enter the TV's NEC address and command in hex as ADDR:CMD (for example 00:1A), " +
                                "taken from a source you trust for this exact TV, then press Test and confirm what the TV did. " +
                                "A phone cannot learn codes from your physical remote because it has no IR receiver.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            when (ui.irAvailable) {
                                true -> "IR emitter: available to apps"
                                false -> "IR emitter: NOT available to apps on this phone"
                                null -> "IR emitter: not checked"
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        item { Text("Test each command", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "Connect on the Home tab first. Start with Volume up. Buttons on the Remote screen unlock only after a test is confirmed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(ordered, key = { it.name }) { key ->
            val verified = key.name in s.verified
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(key.label + if (key == RemoteKey.VOLUME_UP) "  (start here)" else "", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (verified) "Verified on your TV" else "Unverified",
                                color = if (verified) CobyGreen else CobyAmber,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Button(onClick = { vm.testKey(key) }, enabled = !ui.testing && ui.connection == ConnState.CONNECTED) {
                            Text("Test")
                        }
                        if (verified) {
                            TextButton(onClick = { vm.setVerified(key, false) }) { Text("Reset") }
                        }
                    }
                    ui.lastTest[key.name]?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    if (s.method == Method.IR) {
                        var code by remember(s.irProfile[key.name]) { mutableStateOf(s.irProfile[key.name] ?: "") }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("ADDR:CMD (hex)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { vm.saveIrCode(key, code) }) { Text("Save") }
                        }
                    }
                }
            }
        }
    }
}
