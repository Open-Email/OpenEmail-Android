package com.mercata.pingworks.db.pending.messages

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.db.pending.DBPendingMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMessagesDao {

    @Query("SELECT * FROM dbpendingrootmessage")
    suspend fun getAll(): List<DBPendingMessage>

    @Query("SELECT * FROM dbpendingrootmessage ORDER BY timestamp DESC")
    fun getAllAsFlowWithAttachments(): Flow<List<DBPendingMessage>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg message: DBPendingRootMessage)

    @Query("DELETE FROM dbpendingrootmessage WHERE message_id = :messageId")
    suspend fun delete(messageId: String)

    @Delete
    suspend fun deleteList(messages: List<DBPendingRootMessage>)
}