package com.mercata.openemail.db.attachments

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AttachmentsDao {

    @Query("SELECT * FROM dbattachment")
    suspend fun getAll(): List<DBAttachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<DBAttachment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg attachment: DBAttachment)

    @Update
    suspend fun update(attachment: DBAttachment)

    @Query("DELETE FROM dbattachment")
    suspend fun deleteAll()
}