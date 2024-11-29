package com.mercata.pingworks.db.pending.readers

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PendingReadersDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<DBPendingReaderPublicData>)

    @Delete
    suspend fun deleteList(messages: List<DBPendingReaderPublicData>)

    @Query("DELETE FROM dbpendingreaderpublicdata")
    suspend fun deleteAll()

}