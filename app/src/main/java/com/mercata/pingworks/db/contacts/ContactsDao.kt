package com.mercata.pingworks.db.contacts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactsDao {

    @Query("SELECT * FROM dbcontact")
    fun getAll(): Flow<List<DBContact>>

    @Query("SELECT * FROM dbcontact WHERE address IN (:userAddresses)")
    suspend fun loadAllByIds(userAddresses: List<String>): List<DBContact>

    @Query("SELECT * FROM DBContact WHERE name LIKE :name LIMIT 1")
    suspend fun findByName(name: String): DBContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg contact: DBContact)

    @Delete
    suspend fun delete(contact: DBContact)

    @Update
    suspend fun update(contact: DBContact)
}