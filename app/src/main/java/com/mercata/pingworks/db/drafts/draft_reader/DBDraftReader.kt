package com.mercata.pingworks.db.drafts.draft_reader

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mercata.pingworks.db.drafts.DBDraft
import com.mercata.pingworks.models.PublicUserData
import java.time.Instant

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DBDraft::class,
            parentColumns = ["draft_id"],
            childColumns = ["draft_id"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)
data class DBDraftReader(
    @ColumnInfo("draft_id") val draftId: String,
    @ColumnInfo("address") @PrimaryKey val address: String,
    @ColumnInfo("image_url") val imageUrl: String?,
    @ColumnInfo("full_name") val fullName: String,
    @ColumnInfo("last_seen_public") val lastSeenPublic: Boolean,
    @ColumnInfo("last_seen") val lastSeenTimestamp: Long?,
    @ColumnInfo("updated") val updatedTimestamp: Long?,
    @ColumnInfo("encryption_key_id") val encryptionKeyId: String,
    @ColumnInfo("encryption_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo("signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo("public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo("public_signing_key") val publicSigningKey: String,
    @ColumnInfo("last_signing_key") val lastSigningKey: String?,
    @ColumnInfo("last_signing_key_algorithm") val lastSigningKeyAlgorithm: String?,
)

fun DBDraftReader.toPublicUserData() = PublicUserData(
    fullName = fullName,
    imageUrl = imageUrl,
    address = address,
    lastSeenPublic = lastSeenPublic,
    lastSeen = lastSeenTimestamp?.let { Instant.ofEpochMilli(it) },
    updated = updatedTimestamp?.let { Instant.ofEpochMilli(it) },
    encryptionKeyId = encryptionKeyId,
    encryptionKeyAlgorithm = encryptionKeyAlgorithm,
    signingKeyAlgorithm = signingKeyAlgorithm,
    publicEncryptionKey = publicEncryptionKey,
    publicSigningKey = publicSigningKey,
    lastSigningKey = lastSigningKey,
    lastSigningKeyAlgorithm = lastSigningKeyAlgorithm,
)