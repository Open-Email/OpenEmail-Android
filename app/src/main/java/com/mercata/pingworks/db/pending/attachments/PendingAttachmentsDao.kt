package com.mercata.pingworks.db.pending.attachments

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingAttachmentsDao {

    @Query("SELECT * FROM dbpendingattachment ORDER BY modified_at_timestamp DESC")
    suspend fun getAll(): List<DBPendingAttachment>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<DBPendingAttachment>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg message: DBPendingAttachment)

    @Query("DELETE FROM dbpendingattachment WHERE message_id = :messageId")
    suspend fun delete(messageId: String)

    @Query("DELETE FROM dbpendingattachment")
    suspend fun deleteAll()

    @Delete
    suspend fun deleteList(messages: List<DBPendingAttachment>)

}