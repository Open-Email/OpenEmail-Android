package com.mercata.pingworks.db.pending.messages

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mercata.pingworks.db.pending.DBPendingMessage

@Dao
interface PendingMessagesDao {

    @Query("SELECT * FROM dbpendingrootmessage")
    suspend fun getAll(): List<DBPendingMessage>

    @Query("SELECT * FROM dbpendingrootmessage WHERE message_id = :id")
    suspend fun getById(id: String): DBPendingMessage

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<DBPendingRootMessage>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg message: DBPendingRootMessage)

    @Query("DELETE FROM dbpendingrootmessage WHERE message_id = :messageId")
    suspend fun delete(messageId: String)

    @Delete
    suspend fun deleteList(messages: List<DBPendingRootMessage>)

    @Query("DELETE FROM dbpendingrootmessage")
    suspend fun deleteAll()

    @Update
    suspend fun update(message: DBPendingRootMessage)
}