package com.mercata.pingworks.db.contacts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContactsDao {

    @Query("SELECT * FROM dbcontact")
    suspend fun getAll(): List<DBContact>

    @Query("SELECT * FROM dbcontact WHERE id IN (:userIds)")
    suspend fun loadAllByIds(userIds: List<String>): List<DBContact>

    @Query("SELECT * FROM DBContact WHERE name LIKE :name LIMIT 1")
    suspend fun findByName(name: String): DBContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg contact: DBContact)

    @Delete
    suspend fun delete(contact: DBContact)

    @Update
    suspend fun update(contact: DBContact)
}