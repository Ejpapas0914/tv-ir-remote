package com.ejpapas.cobysmartremote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejpapas.cobysmartremote.data.Method
import com.ejpapas.cobysmartremote.data.Settings
import com.ejpapas.cobysmartremote.data.SettingsStore
import com.ejpapas.cobysmartremote.ir.IrController
import com.ejpapas.cobysmartremote.model.RemoteKey
import com.ejpapas.cobysmartremote.net.AdbController
import com.ejpapas.cobysmartremote.net.AdbController.KeyResult
import com.ejpapas.cobysmartremote.net.Discovery
import com.ejpapas.cobysmartremote.net.DiscoveredDevice
import com.ejpapas.cobysmartremote.net.PortProbe
import com.ejpapas.cobysmartremote.net.PortResult
import com.ejpapas.cobysmartremote.util.DiagLog
import com.ejpapas.cobysmartremote.util.NetUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

data class UiState(
    val connection: ConnState = ConnState.DISCONNECTED,
    val connectionDetail: String = "Not connected",
    val probing: Boolean = false,
    val probe: List<PortResult> = emptyList(),
    val discovering: Boolean = false,
    val discovered: List<DiscoveredDevice> = emptyList(),
    val irAvailable: Boolean? = null,
    val irRanges: String = "",
    val testing: Boolean = false,
    val pendingConfirm: RemoteKey? = null,
    val lastTest: Map<String, String> = emptyMap(),
)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val adb = AdbController(app)
    private val ir = IrController(app)
    private val discovery = Discovery(app)

    val settings: StateFlow<Settings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refreshIr()
    }

    private fun say(msg: String) {
        _messages.tryEmit(msg)
    }

    // ---- settings ----
    fun setMethod(m: Method) = viewModelScope.launch {
        disconnectInternal()
        store.setMethod(m)
    }

    fun setHaptics(on: Boolean) = viewModelScope.launch { store.setHaptics(on) }
    fun setIp(ip: String) = viewModelScope.launch { store.setIp(ip.trim()) }
    fun setVerified(key: RemoteKey, on: Boolean) = viewModelScope.launch { store.setVerified(key.name, on) }
    fun saveIrCode(key: RemoteKey, text: String) = viewModelScope.launch {
        if (text.isNotBlank() && IrController.parse(text) == null) {
            say("Use hex ADDR:CMD, e.g. 00:1A")
        } else {
            store.setIrCode(key.name, text)
            // A changed code is no longer verified.
            store.setVerified(key.name, false)
        }
    }

    // ---- IR diagnostics ----
    fun refreshIr() {
        val has = ir.hasEmitter()
        _ui.update { it.copy(irAvailable = has, irRanges = ir.rangesText()) }
        DiagLog.log("IR emitter available to apps: $has")
    }

    // ---- connection ----
    fun connect(ip: String) = viewModelScope.launch {
        val s = settings.value
        store.setIp(ip.trim())
        when (s.method) {
            Method.IR -> {
                refreshIr()
                if (ir.hasEmitter()) {
                    _ui.update { it.copy(connection = ConnState.CONNECTED, connectionDetail = "IR emitter ready. IR is one-way, so TV state is unknown.") }
                } else {
                    _ui.update { it.copy(connection = ConnState.FAILED, connectionDetail = "This phone does not expose an IR emitter to apps.") }
                }
            }
            Method.WIFI_ADB -> {
                if (!NetUtil.isPrivateIpv4(ip.trim())) {
                    say("Enter a private IPv4 address of the TV (192.168.x.x, 10.x.x.x or 172.16-31.x.x)")
                    return@launch
                }
                _ui.update { it.copy(connection = ConnState.CONNECTING, connectionDetail = "Connecting. Accept the prompt on the TV if it appears.") }
                adb.connect(ip.trim()).fold(
                    onSuccess = { m -> _ui.update { it.copy(connection = ConnState.CONNECTED, connectionDetail = m) } },
                    onFailure = { e -> _ui.update { it.copy(connection = ConnState.FAILED, connectionDetail = e.message ?: "Failed") } },
                )
            }
        }
    }

    fun disconnect() = viewModelScope.launch { disconnectInternal() }

    private suspend fun disconnectInternal() {
        adb.close()
        _ui.update { it.copy(connection = ConnState.DISCONNECTED, connectionDetail = "Not connected") }
    }

    private fun markLost(detail: String) {
        _ui.update { it.copy(connection = ConnState.FAILED, connectionDetail = detail) }
    }

    // ---- discovery / probe ----
    fun runDiscovery() = viewModelScope.launch {
        _ui.update { it.copy(discovering = true) }
        val found = discovery.run()
        DiagLog.log("Discovery found ${found.size} service announcement(s)")
        _ui.update { it.copy(discovering = false, discovered = found) }
    }

    fun runProbe() = viewModelScope.launch {
        val ip = settings.value.tvIp
        if (!NetUtil.isPrivateIpv4(ip)) {
            say("Set a valid private TV IP first")
            return@launch
        }
        _ui.update { it.copy(probing = true) }
        val r = PortProbe.probe(ip)
        DiagLog.log("Probe $ip: open = ${r.filter { it.open }.joinToString { it.port.toString() }.ifEmpty { "none" }}")
        _ui.update { it.copy(probing = false, probe = r) }
    }

    // ---- commands ----
    private suspend fun dispatch(key: RemoteKey): KeyResult = when (settings.value.method) {
        Method.WIFI_ADB -> adb.sendKey(key.androidKeyCode)
        Method.IR -> {
            val mapped = settings.value.irProfile[key.name]
            val parsed = mapped?.let { IrController.parse(it) }
            if (parsed == null) KeyResult(false, "No IR code saved for ${key.label}. Add one in Device Setup.")
            else ir.transmitNec(parsed.first, parsed.second).fold(
                onSuccess = { KeyResult(true, "IR signal transmitted. The TV gives no feedback over IR.") },
                onFailure = { KeyResult(false, "IR transmit failed: ${it.message ?: it.javaClass.simpleName}") },
            )
        }
    }

    /** Remote-screen press: only keys verified on the real TV are allowed. */
    fun press(key: RemoteKey) {
        viewModelScope.launch {
            if (key.name !in settings.value.verified) {
                say("${key.label} isn't verified yet. Test it in Device Setup first.")
                return@launch
            }
            if (ui.value.connection != ConnState.CONNECTED) {
                say("Not connected. Open the Home tab and connect.")
                return@launch
            }
            val r = dispatch(key)
            if (r.lost) markLost(r.detail)
            if (!r.sent) say(r.detail)
        }
    }

    /** Device-setup test: sends the key regardless of verification, then gathers evidence. */
    fun testKey(key: RemoteKey) {
        viewModelScope.launch {
            if (ui.value.connection != ConnState.CONNECTED) {
                say("Connect first (Home tab).")
                return@launch
            }
            _ui.update { it.copy(testing = true) }
            val isVolume = key == RemoteKey.VOLUME_UP || key == RemoteKey.VOLUME_DOWN
            val canMeasure = isVolume && settings.value.method == Method.WIFI_ADB
            val before = if (canMeasure) adb.readMusicVolume() else null

            val res = dispatch(key)
            if (res.lost) markLost(res.detail)

            var note = res.detail
            var measured = false
            if (res.sent && canMeasure) {
                delay(700)
                val after = adb.readMusicVolume()
                note += when {
                    before == null || after == null -> " Volume could not be read back."
                    after != before -> { measured = true; " Volume measured $before -> $after." }
                    else -> " Volume unchanged at $after (limit reached, or key ignored)."
                }
            }
            _ui.update {
                it.copy(
                    testing = false,
                    lastTest = it.lastTest + (key.name to note),
                    pendingConfirm = if (res.sent && !measured) key else null,
                )
            }
            if (measured) store.setVerified(key.name, true)
            DiagLog.log("Test ${key.name}: sent=${res.sent} measured=$measured")
        }
    }

    fun confirmTest(key: RemoteKey, worked: Boolean) {
        viewModelScope.launch {
            store.setVerified(key.name, worked)
            _ui.update { it.copy(pendingConfirm = null) }
            DiagLog.log("User marked ${key.name} as ${if (worked) "verified" else "NOT working"}")
        }
    }

    fun dismissConfirm() = _ui.update { it.copy(pendingConfirm = null) }

    override fun onCleared() {
        // Release the ADB socket when the app is torn down (battery/connection hygiene).
        viewModelScope.launch { adb.close() }
        super.onCleared()
    }
}
