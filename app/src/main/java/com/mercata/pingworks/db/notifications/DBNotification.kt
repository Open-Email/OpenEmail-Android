package com.mercata.pingworks.db.notifications

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mercata.pingworks.utils.Address

const val SEVEN_DAYS_MILLIS = 1000 * 60 * 60 * 24 * 7

@Entity
data class DBNotification(
    @PrimaryKey @ColumnInfo("notification_id") val id: String,
    @ColumnInfo("received_on_timestamp") val receivedOnTimestamp: Long,
    @ColumnInfo("link") val link: String,
    @ColumnInfo("full_name") val fullName: String,
    @ColumnInfo("address") val address: Address,
    @ColumnInfo("dismissed") val dismissed: Boolean,
) {
    fun isExpired(): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        return currentTimestamp - SEVEN_DAYS_MILLIS <= receivedOnTimestamp
    }
}
