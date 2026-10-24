package com.ejpapas.cobysmartremote.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "coby_settings")

enum class Method(val label: String) {
    WIFI_ADB("Wi-Fi (ADB)"),
    IR("Infrared")
}

data class Settings(
    val tvIp: String = "192.168.100.227",
    val method: Method = Method.WIFI_ADB,
    val haptics: Boolean = true,
    /** Names of RemoteKey entries the user has verified on the real TV. */
    val verified: Set<String> = emptySet(),
    /** RemoteKey name -> "ADDR:CMD" (hex NEC code the user entered/verified). */
    val irProfile: Map<String, String> = emptyMap(),
)

class SettingsStore(private val context: Context) {
    private object K {
        val IP = stringPreferencesKey("tv_ip")
        val METHOD = stringPreferencesKey("method")
        val HAPTICS = booleanPreferencesKey("haptics")
        val VERIFIED = stringSetPreferencesKey("verified_keys")
        val IR = stringPreferencesKey("ir_profile")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            tvIp = p[K.IP] ?: d.tvIp,
            method = runCatching { Method.valueOf(p[K.METHOD] ?: "") }.getOrDefault(d.method),
            haptics = p[K.HAPTICS] ?: d.haptics,
            verified = p[K.VERIFIED] ?: emptySet(),
            irProfile = decodeIr(p[K.IR].orEmpty()),
        )
    }

    suspend fun setIp(ip: String) = context.dataStore.edit { it[K.IP] = ip }
    suspend fun setMethod(m: Method) = context.dataStore.edit { it[K.METHOD] = m.name }
    suspend fun setHaptics(on: Boolean) = context.dataStore.edit { it[K.HAPTICS] = on }

    suspend fun setVerified(keyName: String, on: Boolean) = context.dataStore.edit {
        val cur = it[K.VERIFIED] ?: emptySet()
        it[K.VERIFIED] = if (on) cur + keyName else cur - keyName
    }

    suspend fun setIrCode(keyName: String, code: String) = context.dataStore.edit {
        val map = decodeIr(it[K.IR].orEmpty()).toMutableMap()
        if (code.isBlank()) map.remove(keyName) else map[keyName] = code.trim()
        it[K.IR] = map.entries.joinToString("\n") { e -> "${e.key}=${e.value}" }
    }

    private fun decodeIr(raw: String): Map<String, String> =
        raw.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }.toMap()
}
