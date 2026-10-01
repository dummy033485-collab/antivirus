package com.safeshield.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.safeshield.app.data.local.entity.LinkCheckEntity
import com.safeshield.app.data.local.entity.LockedAppEntity
import com.safeshield.app.data.local.entity.MalwareSignatureEntity
import com.safeshield.app.data.local.entity.ScanRecordEntity
import com.safeshield.app.data.local.entity.ThreatRecordEntity
import com.safeshield.app.data.local.entity.WhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(signatures: List<MalwareSignatureEntity>)

    @Query("SELECT * FROM malware_signatures WHERE sha256 = :hash LIMIT 1")
    suspend fun findByHash(hash: String): MalwareSignatureEntity?

    @Query("SELECT * FROM malware_signatures WHERE sha256 IN (:hashes)")
    suspend fun findByHashes(hashes: List<String>): List<MalwareSignatureEntity>

    @Query("SELECT COUNT(*) FROM malware_signatures")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM malware_signatures")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM malware_signatures")
    suspend fun clear()
}

@Dao
interface ScanHistoryDao {
    @Insert suspend fun insertScan(record: ScanRecordEntity): Long
    @Insert suspend fun insertThreats(threats: List<ThreatRecordEntity>)

    @Transaction
    suspend fun saveScan(record: ScanRecordEntity, threats: (Long) -> List<ThreatRecordEntity>): Long {
        val id = insertScan(record)
        insertThreats(threats(id))
        return id
    }

    @Query("SELECT * FROM scan_records ORDER BY finishedAt DESC")
    fun observeHistory(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records ORDER BY finishedAt DESC LIMIT 1")
    fun observeLastScan(): Flow<ScanRecordEntity?>

    @Query("SELECT * FROM scan_records WHERE id = :scanId")
    suspend fun getScan(scanId: Long): ScanRecordEntity?

    @Query("SELECT * FROM threat_records WHERE scanId = :scanId ORDER BY severity DESC")
    suspend fun getThreatsForScan(scanId: Long): List<ThreatRecordEntity>

    @Query("SELECT * FROM threat_records WHERE scanId = :scanId ORDER BY detectedAt DESC")
    fun observeThreatsForScan(scanId: Long): Flow<List<ThreatRecordEntity>>

    @Query("SELECT * FROM threat_records WHERE resolved = 0 ORDER BY severity DESC")
    fun observeActiveThreats(): Flow<List<ThreatRecordEntity>>

    @Query("UPDATE threat_records SET resolved = 1 WHERE id = :threatId")
    suspend fun markResolved(threatId: Long)

    @Query("UPDATE threat_records SET resolved = 1 WHERE packageName = :packageName")
    suspend fun markResolvedByPackage(packageName: String)

    @Query("DELETE FROM scan_records")
    suspend fun clearHistory()
}

@Dao
interface WhitelistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: WhitelistEntity)

    @Query("DELETE FROM whitelist WHERE packageName = :packageName")
    suspend fun remove(packageName: String)

    @Query("SELECT * FROM whitelist ORDER BY label")
    fun observeAll(): Flow<List<WhitelistEntity>>

    @Query("SELECT packageName FROM whitelist")
    suspend fun allPackages(): List<String>

    @Query("DELETE FROM whitelist")
    suspend fun clear()
}

@Dao
interface LockedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: LockedAppEntity)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun remove(packageName: String)

    @Query("SELECT * FROM locked_apps ORDER BY label")
    fun observeAll(): Flow<List<LockedAppEntity>>

    @Query("SELECT packageName FROM locked_apps")
    suspend fun allPackages(): List<String>

    @Query("DELETE FROM locked_apps")
    suspend fun clear()
}

@Dao
interface LinkCheckDao {
    @Insert suspend fun insert(entity: LinkCheckEntity): Long

    @Query("SELECT * FROM link_checks ORDER BY checkedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<LinkCheckEntity>>

    @Query("DELETE FROM link_checks")
    suspend fun clear()
}
