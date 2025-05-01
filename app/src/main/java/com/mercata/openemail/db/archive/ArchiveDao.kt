package com.mercata.openemail.db.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveDao {

    @Transaction
    @Query("SELECT * FROM dbarchivedmessage ORDER BY timestamp DESC")
    fun getAllAsFlow(): Flow<List<DBArchiveWitAttachments>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: DBArchivedMessage)

    @Query("DELETE FROM dbarchivedmessage WHERE archive_id = :messageId")
    suspend fun delete(messageId: String)

    @Query("DELETE FROM dbarchivedmessage")
    suspend fun deleteAll()
}