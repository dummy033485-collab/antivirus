package com.safeshield.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.safeshield.app.data.local.dao.LinkCheckDao
import com.safeshield.app.data.local.dao.LockedAppDao
import com.safeshield.app.data.local.dao.ScanHistoryDao
import com.safeshield.app.data.local.dao.SignatureDao
import com.safeshield.app.data.local.dao.WhitelistDao
import com.safeshield.app.data.local.entity.LinkCheckEntity
import com.safeshield.app.data.local.entity.LockedAppEntity
import com.safeshield.app.data.local.entity.MalwareSignatureEntity
import com.safeshield.app.data.local.entity.ScanRecordEntity
import com.safeshield.app.data.local.entity.ThreatRecordEntity
import com.safeshield.app.data.local.entity.WhitelistEntity

@Database(
    entities = [
        MalwareSignatureEntity::class,
        ScanRecordEntity::class,
        ThreatRecordEntity::class,
        WhitelistEntity::class,
        LockedAppEntity::class,
        LinkCheckEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SafeShieldDatabase : RoomDatabase() {
    abstract fun signatureDao(): SignatureDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun linkCheckDao(): LinkCheckDao
}
