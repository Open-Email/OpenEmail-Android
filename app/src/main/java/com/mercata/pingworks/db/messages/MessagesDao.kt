package com.mercata.pingworks.db.messages

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessagesDao {
    @Query("SELECT * FROM dbmessage")
    fun getAllAsFlow(): Flow<List<DBMessage>>

    @Query("SELECT * FROM dbmessage")
    fun getAllAsFlowWithAttachments(): Flow<List<DBMessageWithDBAttachments>>

    @Query("SELECT * FROM dbmessage")
    suspend fun getAll(): List<DBMessage>

    @Query("SELECT * FROM dbmessage")
    suspend fun getAllWithAttachments(): List<DBMessageWithDBAttachments>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<DBMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg message: DBMessage)

    @Delete
    suspend fun delete(message: DBMessage)

    @Update
    suspend fun update(message: DBMessage)
}