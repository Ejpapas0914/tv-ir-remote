package com.ejpapas.cobysmartremote.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Default.Home),
    Tab("Remote", Icons.Default.SettingsRemote),
    Tab("Setup", Icons.Default.Tune),
    Tab("Diagnostics", Icons.Default.BugReport),
)

@Composable
fun CobyApp(vm: RemoteViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> HomeScreen(ui, settings, vm)
                1 -> RemoteScreen(ui, settings, vm)
                2 -> SetupScreen(ui, settings, vm)
                else -> DiagnosticsScreen(ui, settings, vm)
            }
        }
    }

    ui.pendingConfirm?.let { key ->
        AlertDialog(
            onDismissRequest = { vm.dismissConfirm() },
            title = { Text("Did the TV react?") },
            text = { Text("Watch the TV: did \"${key.label}\" do what you expected? Only say yes if you saw it happen.") },
            confirmButton = { TextButton(onClick = { vm.confirmTest(key, true) }) { Text("Yes, it worked") } },
            dismissButton = { TextButton(onClick = { vm.confirmTest(key, false) }) { Text("No") } },
        )
    }
}
