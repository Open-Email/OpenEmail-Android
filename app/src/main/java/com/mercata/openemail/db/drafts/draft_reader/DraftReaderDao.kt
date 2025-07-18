package com.mercata.openemail.db.drafts.draft_reader

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftReaderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reader: DBDraftReader)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readers: List<DBDraftReader>)

    @Query("DELETE FROM dbdraftreader WHERE address = :address")
    suspend fun delete(address: String)

    @Query("SELECT * FROM dbdraftreader WHERE draft_id == :draftId ORDER BY address DESC")
    fun getAllAsFlow(draftId: String): Flow<List<DBDraftReader>>

    @Query("DELETE FROM dbdraftreader")
    suspend fun deleteAll()
}