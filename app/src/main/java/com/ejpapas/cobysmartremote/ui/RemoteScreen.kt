package com.ejpapas.cobysmartremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejpapas.cobysmartremote.data.Settings
import com.ejpapas.cobysmartremote.model.RemoteKey

@Composable
private fun KeyButton(
    key: RemoteKey,
    verified: Boolean,
    haptics: Boolean,
    onPress: (RemoteKey) -> Unit,
    icon: ImageVector? = null,
    text: String? = null,
    accent: Boolean = false,
    width: Dp = 64.dp,
    height: Dp = 64.dp,
    shape: Shape = CircleShape,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onPress(key)
        },
        modifier = Modifier
            .size(width, height)
            .alpha(if (verified) 1f else 0.4f)
            .semantics { contentDescription = key.label },
        shape = shape,
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, contentDescription = null)
            else Text(text ?: key.label, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
fun RemoteScreen(ui: UiState, s: Settings, vm: RemoteViewModel) {
    // Local helper so each call site stays short.
    @Composable
    fun Btn(
        key: RemoteKey,
        icon: ImageVector? = null,
        text: String? = null,
        accent: Boolean = false,
        width: Dp = 64.dp,
        height: Dp = 64.dp,
        shape: Shape = CircleShape,
    ) = KeyButton(
        key = key,
        verified = key.name in s.verified,
        haptics = s.haptics,
        onPress = vm::press,
        icon = icon, text = text, accent = accent, width = width, height = height, shape = shape,
    )

    val pill = RoundedCornerShape(32.dp)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = statusColor(ui.connection), modifier = Modifier.size(10.dp)) {}
            Text(statusLabel(ui.connection), style = MaterialTheme.typography.labelLarge)
        }
        if (s.verified.isEmpty()) {
            Text(
                "No buttons are verified yet. Dimmed buttons are untested: open Setup and test them on your TV.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Btn(RemoteKey.POWER, Icons.Default.PowerSettingsNew, accent = true)
            Btn(RemoteKey.SOURCE, Icons.AutoMirrored.Filled.Input)
            Btn(RemoteKey.SETTINGS, Icons.Default.Settings)
        }

        // D-pad
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Btn(RemoteKey.DPAD_UP, Icons.Default.KeyboardArrowUp, width = 84.dp, height = 64.dp, shape = pill)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Btn(RemoteKey.DPAD_LEFT, Icons.Default.KeyboardArrowLeft, width = 64.dp, height = 84.dp, shape = pill)
                Btn(RemoteKey.OK, text = "OK", accent = true, width = 84.dp, height = 84.dp)
                Btn(RemoteKey.DPAD_RIGHT, Icons.Default.KeyboardArrowRight, width = 64.dp, height = 84.dp, shape = pill)
            }
            Btn(RemoteKey.DPAD_DOWN, Icons.Default.KeyboardArrowDown, width = 84.dp, height = 64.dp, shape = pill)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Btn(RemoteKey.BACK, Icons.AutoMirrored.Filled.ArrowBack)
            Btn(RemoteKey.HOME, Icons.Default.Home)
            Btn(RemoteKey.MENU, Icons.Default.Menu)
        }

        // Volume and channel rockers
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(RemoteKey.VOLUME_UP, Icons.Default.VolumeUp, width = 72.dp, height = 64.dp, shape = pill)
                Btn(RemoteKey.MUTE, Icons.Default.VolumeOff, width = 72.dp, height = 56.dp, shape = pill)
                Btn(RemoteKey.VOLUME_DOWN, Icons.Default.VolumeDown, width = 72.dp, height = 64.dp, shape = pill)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(RemoteKey.CHANNEL_UP, Icons.Default.ExpandLess, width = 72.dp, height = 64.dp, shape = pill)
                Text("CH", style = MaterialTheme.typography.labelLarge)
                Btn(RemoteKey.CHANNEL_DOWN, Icons.Default.ExpandMore, width = 72.dp, height = 64.dp, shape = pill)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Btn(RemoteKey.REWIND, Icons.Default.FastRewind, width = 80.dp, height = 56.dp, shape = pill)
            Btn(RemoteKey.PLAY_PAUSE, Icons.Default.PlayArrow, width = 80.dp, height = 56.dp, shape = pill, accent = true)
            Btn(RemoteKey.FAST_FORWARD, Icons.Default.FastForward, width = 80.dp, height = 56.dp, shape = pill)
        }

        // Number pad
        val rows = listOf(
            listOf(RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3),
            listOf(RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6),
            listOf(RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            rows.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    r.forEach { k -> Btn(k, text = k.label, width = 76.dp, height = 56.dp, shape = pill) }
                }
            }
            Btn(RemoteKey.NUM_0, text = "0", width = 76.dp, height = 56.dp, shape = pill)
        }
        Spacer(Modifier.height(12.dp))
    }
}
