package com.mercata.pingworks.db.notifications

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.mercata.pingworks.contacts_screen.ContactItem
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.utils.Address
import java.time.Instant

const val SEVEN_DAYS_MILLIS = 1000 * 60 * 60 * 24 * 7

@Entity
data class DBNotification(
    @PrimaryKey @ColumnInfo("notification_id") val notificationId: String,
    @ColumnInfo("received_on_timestamp") val receivedOnTimestamp: Long,
    @ColumnInfo("link") val link: String,
    @ColumnInfo("full_name") override val name: String,
    @ColumnInfo("address") override val address: Address,
    @ColumnInfo("dismissed") val dismissed: Boolean,
    @ColumnInfo("last_seen_public") val lastSeenPublic: Boolean,
    @ColumnInfo("last_seen") val lastSeen: Long?,
    @ColumnInfo("updated") val updated: Long?,
    @ColumnInfo("encryption_key_id") val encryptionKeyId: String,
    @ColumnInfo("encryption_key_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo("signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo("public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo("public_signing_key") val publicSigningKey: String,
    @ColumnInfo("last_signing_key") val lastSigningKey: String?,
    @ColumnInfo("last_signing_key_algorithm") val lastSigningKeyAlgorithm: String?,
) : ContactItem {

    @Ignore
    override val key: String = notificationId

    fun isExpired(): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        return currentTimestamp - SEVEN_DAYS_MILLIS > receivedOnTimestamp
    }
}

fun DBNotification.toPublicUserData(): PublicUserData {
    return PublicUserData(
        fullName = this.name,
        address = this.address,
        lastSeenPublic = this.lastSeenPublic,
        lastSeen = this.lastSeen?.let { Instant.ofEpochMilli(it) },
        updated = this.updated?.let { Instant.ofEpochMilli(it) },
        encryptionKeyId = this.encryptionKeyId,
        encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
        signingKeyAlgorithm = this.signingKeyAlgorithm,
        publicEncryptionKey = this.publicEncryptionKey,
        publicSigningKey = this.publicSigningKey,
        lastSigningKey = this.lastSigningKey,
        lastSigningKeyAlgorithm = this.lastSigningKeyAlgorithm,
    )
}
