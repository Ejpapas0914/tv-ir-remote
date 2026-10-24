package com.ejpapas.cobysmartremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import com.ejpapas.cobysmartremote.util.DiagLog

/**
 * Infrared transmitter wrapper.
 *
 * This class contains NO TV-specific codes. It only knows how to encode the NEC protocol
 * from an address/command pair that YOU supply and verify against the Star Coby TV.
 * IR is one-way: the phone cannot learn whether the TV received a signal.
 */
class IrController(context: Context) {
    private val mgr: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasEmitter(): Boolean = try {
        mgr?.hasIrEmitter() == true
    } catch (e: Exception) {
        DiagLog.log("IR: hasIrEmitter failed: ${e.javaClass.simpleName}")
        false
    }

    fun rangesText(): String = try {
        val r = mgr?.carrierFrequencies
        if (r.isNullOrEmpty()) "none reported"
        else r.joinToString { "${it.minFrequency / 1000}-${it.maxFrequency / 1000} kHz" }
    } catch (e: Exception) {
        "unavailable"
    }

    /** Transmits a standard (8-bit address) or extended (16-bit address) NEC frame at 38 kHz. */
    fun transmitNec(address: Int, command: Int): Result<Unit> {
        val m = mgr ?: return Result.failure(IllegalStateException("No IR service on this phone"))
        if (!hasEmitter()) return Result.failure(IllegalStateException("No IR emitter exposed to apps"))
        return try {
            m.transmit(38000, necPattern(address, command))
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /** Parses "ADDR:CMD" in hex, e.g. "00:1A" or "7F80:0B". Returns null if invalid. */
        fun parse(text: String): Pair<Int, Int>? {
            val parts = text.trim().split(":")
            if (parts.size != 2) return null
            val a = parts[0].trim().takeIf { it.length in 1..4 }?.toIntOrNull(16) ?: return null
            val c = parts[1].trim().takeIf { it.length in 1..2 }?.toIntOrNull(16) ?: return null
            return a to c
        }

        /** NEC timing in microseconds: 9000/4500 leader, 560 mark, 560 or 1690 space, final mark. */
        fun necPattern(address: Int, command: Int): IntArray {
            val bits = ArrayList<Int>()
            fun addByte(b: Int) { for (i in 0 until 8) bits.add((b shr i) and 1) } // LSB first
            if (address <= 0xFF) {
                addByte(address); addByte(address.inv() and 0xFF)
            } else {
                addByte(address and 0xFF); addByte((address shr 8) and 0xFF)
            }
            addByte(command); addByte(command.inv() and 0xFF)

            val out = ArrayList<Int>()
            out.add(9000); out.add(4500)
            for (b in bits) { out.add(560); out.add(if (b == 1) 1690 else 560) }
            out.add(560)
            return out.toIntArray()
        }
    }
}
