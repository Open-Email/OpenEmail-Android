package com.mercata.openemail.db.notifications

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mercata.openemail.utils.Address
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationsDao {
    @Query("SELECT * FROM dbnotification ORDER BY received_on_timestamp DESC")
    fun getAllAsFlow(): Flow<List<DBNotification>>

    @Query("SELECT * FROM dbnotification ORDER BY received_on_timestamp DESC")
    suspend fun getAll(): List<DBNotification>

    @Query("SELECT * FROM dbnotification WHERE address = :address")
    suspend fun getByAddress(address: Address): DBNotification?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(notifications: List<DBNotification>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg notification: DBNotification)

    @Delete
    suspend fun deleteList(notifications: List<DBNotification>)

    @Delete
    suspend fun delete(notification: DBNotification)

    @Query("DELETE FROM dbnotification")
    suspend fun deleteAll()

    @Update
    suspend fun update(notification: DBNotification)
}