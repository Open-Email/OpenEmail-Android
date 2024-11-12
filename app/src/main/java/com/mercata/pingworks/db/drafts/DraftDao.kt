package com.mercata.pingworks.db.drafts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg draft: DBDraft)

    @Update
    suspend fun update(draft: DBDraft)

    @Query("SELECT * FROM dbdraft WHERE draft_id = :draftId")
    suspend fun getById(draftId: String): DBDraftWithReaders?

    @Query("SELECT * FROM dbdraft ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DBDraftWithReaders>>

    @Query("SELECT * FROM dbdraft WHERE draft_id = :draftId")
    fun getByIdFlow(draftId: String): Flow<DBDraftWithReaders?>

    @Query("DELETE FROM dbdraft WHERE draft_id = :draftId")
    suspend fun delete(draftId: String)
}