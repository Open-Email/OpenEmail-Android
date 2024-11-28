package com.mercata.pingworks.db.contacts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.mercata.pingworks.contacts_screen.ContactItem
import com.mercata.pingworks.models.PublicUserData
import java.time.Instant

@Entity
data class DBContact(
    @ColumnInfo(name = "updated") val updated: String?,
    @ColumnInfo(name = "uploaded") val uploaded: Boolean,
    @ColumnInfo(name = "marked_to_delete") val markedToDelete: Boolean,
    @ColumnInfo(name = "last_seen") val lastSeen: String?,
    @PrimaryKey @ColumnInfo(name = "address") override val address: String,
    @ColumnInfo(name = "name") override val name: String?,
    @ColumnInfo(name = "last_seen_public") val lastSeenPublic: Boolean,
    @ColumnInfo(name = "receive_broadcasts") val receiveBroadcasts: Boolean,
    @ColumnInfo(name = "signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo(name = "encryption_key_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo(name = "public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo(name = "public_encryption_key_id") val publicEncryptionKeyId: String,
    @ColumnInfo(name = "public_signing_key") val publicSigningKey: String,
    @ColumnInfo(name = "last_signing_key") val lastSigningKey: String?,
    @ColumnInfo(name = "last_signing_key_algorithm") val lastSigningKeyAlgorithm: String?,
): ContactItem {

    @Ignore
    override val key: String = address
}

fun DBContact.toPublicUserData(): PublicUserData =
    PublicUserData(
        fullName = this.name ?: "",
        address = this.address,
        lastSeenPublic = this.lastSeenPublic,
        lastSeen = this.lastSeen?.let { Instant.parse(it) },
        updated = this.lastSeen?.let { Instant.parse(it) },
        encryptionKeyId = this.publicEncryptionKeyId,
        encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
        signingKeyAlgorithm = this.signingKeyAlgorithm,
        publicEncryptionKey = this.publicEncryptionKey,
        publicSigningKey = this.publicSigningKey,
        lastSigningKey = this.lastSigningKey,
        lastSigningKeyAlgorithm = this.lastSigningKeyAlgorithm
    )