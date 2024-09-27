package com.mercata.pingworks.db.pending.readers

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PendingReadersDao {

    @Query("SELECT * FROM dbpendingreaderpublicdata")
    suspend fun getAll(): List<DBPendingReaderPublicData>

    @Query("SELECT * FROM dbpendingreaderpublicdata WHERE message_id = :id")
    suspend fun getById(id: String): DBPendingReaderPublicData

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<DBPendingReaderPublicData>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg message: DBPendingReaderPublicData)

    @Delete
    suspend fun deleteList(messages: List<DBPendingReaderPublicData>)

    @Delete
    suspend fun delete(message: DBPendingReaderPublicData)

    @Query("DELETE FROM dbpendingreaderpublicdata")
    suspend fun deleteAll()

    @Update
    suspend fun update(message: DBPendingReaderPublicData)
}