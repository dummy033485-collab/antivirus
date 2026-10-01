package com.safeshield.app.core

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Streaming SHA-256 helper. Never loads a whole APK into memory – APKs can be
 * hundreds of megabytes and antivirus scanning must stay inside a modest heap.
 */
object Hashing {

    private const val BUFFER_SIZE = 1 shl 16 // 64 KiB

    fun sha256(file: File): String? = runCatching {
        file.inputStream().use { sha256(it) }
    }.getOrNull()

    fun sha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHex()
    }

    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).toHex()

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
