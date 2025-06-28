package com.mercata.openemail.db.messages

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessagesDao {
    @Query("SELECT * FROM dbmessage ORDER BY timestamp DESC")
    fun getAllAsFlow(): Flow<List<DBMessage>>

    @Transaction
    @Query("SELECT * FROM dbmessage ORDER BY timestamp DESC")
    fun getAllAsFlowWithAttachments(): Flow<List<DBMessageWithDBAttachments>>

    @Query("SELECT * FROM dbmessage ORDER BY timestamp DESC")
    suspend fun getAll(): List<DBMessage>

    @Transaction
    @Query("SELECT * FROM dbmessage ORDER BY timestamp DESC")
    suspend fun getAllWithAttachments(): List<DBMessageWithDBAttachments>

    @Transaction
    @Query("SELECT * FROM dbmessage WHERE marked_to_delete = 1 ORDER BY timestamp DESC")
    suspend fun getAllMarkedToDelete(): List<DBMessageWithDBAttachments>

    @Transaction
    @Query("SELECT * FROM dbmessage WHERE message_id = :id")
    suspend fun getById(id: String): DBMessageWithDBAttachments?

    @Transaction
    @Query("SELECT * FROM dbmessage WHERE author_address = :address")
    suspend fun getAllForContactAddress(address: String): List<DBMessageWithDBAttachments>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<DBMessage>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg message: DBMessage)

    @Delete
    suspend fun delete(message: DBMessage)

    @Delete
    suspend fun deleteList(message: List<DBMessage>)

    @Query("DELETE FROM dbmessage")
    suspend fun deleteAll()

    @Update
    suspend fun update(message: DBMessage)
}