package com.safeshield.app.core

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

object Formatters {

    fun bytes(size: Long): String {
        if (size < 1024) return "$size B"
        val exp = (ln(size.toDouble()) / ln(1024.0)).toInt().coerceAtMost(4)
        val unit = "KMGTP"[exp - 1]
        return String.format(Locale.US, "%.1f %sB", size / 1024.0.pow(exp.toDouble()), unit)
    }

    /** Short human duration, e.g. "8s", "1m 20s". */
    fun duration(millis: Long): String {
        val total = abs(millis) / 1000
        val minutes = total / 60
        val seconds = total % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    fun dateTime(epochMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(epochMillis))
}
