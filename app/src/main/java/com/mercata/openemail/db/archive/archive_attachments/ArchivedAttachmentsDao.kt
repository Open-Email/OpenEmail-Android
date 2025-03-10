package com.mercata.openemail.db.archive.archive_attachments

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ArchivedAttachmentsDao {

    @Query("SELECT * FROM dbarchivedattachment")
    suspend fun getAll(): List<DBArchivedAttachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<DBArchivedAttachment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg attachment: DBArchivedAttachment)

    @Update
    suspend fun update(attachment: DBArchivedAttachment)

    @Query("DELETE FROM dbarchivedattachment")
    suspend fun deleteAll()
}