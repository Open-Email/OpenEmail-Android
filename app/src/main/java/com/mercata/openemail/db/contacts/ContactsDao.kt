package com.mercata.openemail.db.contacts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mercata.openemail.utils.Address
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactsDao {

    @Query("SELECT * FROM dbcontact ORDER BY address DESC")
    fun getAllAsFlow(): Flow<List<DBContact>>

    @Query("SELECT * FROM dbcontact ORDER BY address DESC")
    suspend fun getAll(): List<DBContact>

    @Query("SELECT * FROM DBContact WHERE address LIKE :address LIMIT 1")
    suspend fun findByAddress(address: Address): DBContact?

    @Query("SELECT * FROM DBContact WHERE address LIKE :address LIMIT 1")
    fun findByAddressFlow(address: Address): Flow<DBContact?>

    @Query("SELECT * FROM DBContact WHERE name LIKE :name LIMIT 1")
    suspend fun findByName(name: String): DBContact?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contacts: List<DBContact>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg contact: DBContact)

    @Delete
    suspend fun delete(contact: DBContact)

    @Delete
    suspend fun deleteList(contact: List<DBContact>)

    @Query("DELETE FROM dbcontact")
    suspend fun deleteAll()

    @Update
    suspend fun update(contact: DBContact)
}