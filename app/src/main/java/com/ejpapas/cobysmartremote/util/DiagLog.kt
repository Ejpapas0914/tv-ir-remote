package com.ejpapas.cobysmartremote.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-memory diagnostic log (last 300 lines). Never put keys, tokens or key-file contents here.
 * The ADB private key stays in the app's private storage and is never logged or displayed.
 */
object DiagLog {
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun log(msg: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _lines.update { (it + "$stamp  $msg").takeLast(300) }
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
