package com.safeshield.app.data.local

import androidx.room.TypeConverter
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.domain.model.Severity
import com.safeshield.app.domain.model.ThreatSource

class Converters {
    @TypeConverter fun severityToString(value: Severity): String = value.name
    @TypeConverter fun stringToSeverity(value: String): Severity =
        runCatching { Severity.valueOf(value) }.getOrDefault(Severity.LOW)

    @TypeConverter fun scanTypeToString(value: ScanType): String = value.name
    @TypeConverter fun stringToScanType(value: String): ScanType =
        runCatching { ScanType.valueOf(value) }.getOrDefault(ScanType.QUICK)

    @TypeConverter fun sourceToString(value: ThreatSource): String = value.name
    @TypeConverter fun stringToSource(value: String): ThreatSource =
        runCatching { ThreatSource.valueOf(value) }.getOrDefault(ThreatSource.HEURISTIC)

    /** Reasons are free text; "\u001F" (unit separator) can never appear in them. */
    @TypeConverter fun listToString(value: List<String>): String = value.joinToString("\u001F")
    @TypeConverter fun stringToList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("\u001F")
}
