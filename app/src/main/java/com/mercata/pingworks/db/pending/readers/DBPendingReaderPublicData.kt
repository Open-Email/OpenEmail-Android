package com.mercata.pingworks.db.pending.readers

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DBPendingReaderPublicData(
    @ColumnInfo("uuid") @PrimaryKey val uuid: String,
    @ColumnInfo("address") val address: String,
    @ColumnInfo("message_id", index = true) val messageId: String,
    @ColumnInfo("full_name") val fullName: String,
    @ColumnInfo("last_seen_public") val lastSeenPublic: Boolean,
    @ColumnInfo("last_seen") val lastSeenTimestamp: Long?,
    @ColumnInfo("updated") val updatedTimestamp: Long?,
    @ColumnInfo("encryption_key_id") val encryptionKeyId: String,
    @ColumnInfo("encryption_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo("signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo("public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo("public_signing_key") val publicSigningKey: String,
)