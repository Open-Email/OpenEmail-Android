package com.mercata.pingworks.db.archive.archive_readers

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mercata.pingworks.db.drafts.draft_reader.DBDraftReader
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveReaderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reader: DBArchivedReader)

    @Query("DELETE FROM dbarchivedreader WHERE address = :address")
    suspend fun delete(address: String)

    @Query("SELECT * FROM dbarchivedreader WHERE archive_id == :archiveId ORDER BY address DESC")
    fun getAllAsFlow(archiveId: String): Flow<List<DBArchivedReader>>

    @Query("DELETE FROM dbarchivedreader")
    suspend fun deleteAll()
}