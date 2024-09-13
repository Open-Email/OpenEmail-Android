package com.mercata.pingworks.db.contacts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mercata.pingworks.db.messages.DBAttachment
import com.mercata.pingworks.db.messages.DBMessage

@Dao
interface AttachmentsDao {

    @Query("SELECT * FROM dbattachment")
    suspend fun getAll(): List<DBAttachment>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(attachments: List<DBAttachment>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg attachment: DBAttachment)

    @Update
    suspend fun update(attachment: DBAttachment)
}