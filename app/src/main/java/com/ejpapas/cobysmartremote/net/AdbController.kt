package com.ejpapas.cobysmartremote.net

import android.content.Context
import com.ejpapas.cobysmartremote.util.DiagLog
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Controls the TV through Android's own debug bridge (ADB over TCP, default port 5555).
 *
 * Requirements on the TV: Developer options -> USB/Network debugging ON. The first time, the TV
 * shows an "Allow debugging?" dialog for this phone's key; you must accept it on the TV. This is
 * the normal ADB authorization flow, nothing is bypassed. The key pair is generated on this
 * phone, kept in app-private storage and never logged.
 *
 * "sent" means the TV's shell accepted `input keyevent N` (exit code 0). It does NOT prove the
 * TV acted on it; use the before/after checks and the manual confirmation in Device Setup.
 */
class AdbController(private val context: Context) {
    data class KeyResult(val sent: Boolean, val detail: String, val lost: Boolean = false)

    private val mutex = Mutex()
    private var dadb: Dadb? = null

    private fun keyPair(): AdbKeyPair {
        val priv = File(context.filesDir, "adbkey")
        val pub = File(context.filesDir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
        return AdbKeyPair.read(priv, pub)
    }

    suspend fun connect(host: String, port: Int = 5555): Result<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            closeInternal()
            DiagLog.log("ADB: connecting to $host:$port (accept the prompt on the TV if shown)")
            val d = Dadb.create(host, port, keyPair())
            val future = CompletableFuture.supplyAsync { d.shell("echo coby_ok") }
            try {
                val r = future.get(30, TimeUnit.SECONDS)
                if (r.exitCode == 0 && r.output.contains("coby_ok")) {
                    dadb = d
                    DiagLog.log("ADB: authorised and shell reachable")
                    Result.success("ADB session authorised")
                } else {
                    runCatching { d.close() }
                    DiagLog.log("ADB: unexpected shell reply (exit ${r.exitCode})")
                    Result.failure<String>(IllegalStateException("Unexpected reply from TV shell"))
                }
            } catch (e: TimeoutException) {
                runCatching { d.close() }
                DiagLog.log("ADB: timed out waiting for authorisation")
                Result.failure<String>(IllegalStateException("No answer in 30 s. Enable network debugging on the TV and tap Allow on its prompt."))
            } catch (e: ExecutionException) {
                runCatching { d.close() }
                val c = e.cause ?: e
                DiagLog.log("ADB: failed (${c.javaClass.simpleName})")
                val msg = if (c is ConnectException) "Connection refused: nothing is listening on port $port. Network debugging is probably off on the TV, or the IP changed."
                else "ADB failed: ${c.message ?: c.javaClass.simpleName}"
                Result.failure<String>(IllegalStateException(msg))
            }
        }
    }

    suspend fun sendKey(code: Int): KeyResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext KeyResult(false, "Not connected", lost = true)
            try {
                val r = d.shell("input keyevent $code")
                if (r.exitCode == 0) {
                    DiagLog.log("ADB: keyevent $code accepted by TV shell")
                    KeyResult(true, "Sent. The TV shell accepted keyevent $code.")
                } else {
                    DiagLog.log("ADB: keyevent $code rejected (exit ${r.exitCode})")
                    KeyResult(false, "TV rejected the command: ${r.allOutput.trim().take(120)}")
                }
            } catch (e: Exception) {
                closeInternal()
                DiagLog.log("ADB: connection lost (${e.javaClass.simpleName})")
                KeyResult(false, "Connection lost. Reconnect from the Home tab.", lost = true)
            }
        }
    }

    /** Best-effort read of the music stream volume; null if the TV does not report it. */
    suspend fun readMusicVolume(): Int? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext null
            try {
                val r = d.shell("cmd media_session volume --stream 3 --get")
                Regex("volume is (\\d+)").find(r.allOutput)?.groupValues?.get(1)?.toIntOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun close() = mutex.withLock { withContext(Dispatchers.IO) { closeInternal() } }

    private fun closeInternal() {
        runCatching { dadb?.close() }
        dadb = null
    }
}
