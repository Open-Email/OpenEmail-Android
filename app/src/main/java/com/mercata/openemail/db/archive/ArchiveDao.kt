package com.mercata.openemail.db.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mercata.openemail.db.drafts.DBDraftWithReaders
import com.mercata.openemail.db.messages.DBMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveDao {

    @Transaction
    @Query("SELECT * FROM dbarchivedmessage ORDER BY timestamp DESC")
    fun getAllAsFlow(): Flow<List<DBArchiveWitAttachments>>

    @Transaction
    @Query("SELECT * FROM dbarchivedmessage WHERE archive_id = :archiveId")
    suspend fun getById(archiveId: String): DBArchiveWitAttachments?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: DBArchivedMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<DBArchivedMessage>)

    @Query("DELETE FROM dbarchivedmessage WHERE archive_id = :messageId")
    suspend fun delete(messageId: String)

    @Query("DELETE FROM dbarchivedmessage")
    suspend fun deleteAll()
}