package com.mercata.pingworks.db.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveDao {

    @Query("SELECT * FROM dbarchivedmessage ORDER BY timestamp DESC")
    fun getAllAsFlow(): Flow<List<DBArchiveWithReaders>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<DBArchivedMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: DBArchivedMessage)

    @Query("DELETE FROM dbarchivedmessage WHERE archive_id = :messageId")
    suspend fun delete(messageId: String)

    @Query("DELETE FROM dbarchivedmessage")
    suspend fun deleteAll()
}