package com.mercata.pingworks.models

import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.pending.readers.DBPendingReaderPublicData
import com.mercata.pingworks.utils.Address
import java.time.Instant
import java.util.UUID


data class PublicUserData(
    val fullName: String,
    val address: Address,
    val lastSeenPublic: Boolean,
    val lastSeen: Instant?,
    val updated: Instant?,
    val encryptionKeyId: String,
    val encryptionKeyAlgorithm: String,
    val signingKeyAlgorithm: String,
    val publicEncryptionKey: String,
    val publicSigningKey: String,
)

fun PublicUserData.toDBPendingReaderPublicData(messageId: String) = DBPendingReaderPublicData(
    uuid = UUID.randomUUID().toString(),
    fullName = this.fullName,
    address = this.address,
    lastSeenPublic = this.lastSeenPublic,
    lastSeenTimestamp = this.lastSeen?.toEpochMilli(),
    updatedTimestamp = this.updated?.toEpochMilli(),
    encryptionKeyId = this.encryptionKeyId,
    encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
    signingKeyAlgorithm = this.signingKeyAlgorithm,
    publicEncryptionKey = this.publicEncryptionKey,
    publicSigningKey = this.publicSigningKey,
    messageId = messageId
)

fun DBContact.toPublicUserData(): PublicUserData =
    PublicUserData(
        fullName = this.name ?: "",
        address = this.address,
        lastSeenPublic = this.lastSeenPublic,
        lastSeen = Instant.parse(this.lastSeen),
        updated = Instant.parse(this.lastSeen),
        encryptionKeyId = this.publicEncryptionKeyId,
        encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
        signingKeyAlgorithm = this.signingKeyAlgorithm,
        publicEncryptionKey = this.publicEncryptionKey,
        publicSigningKey = this.publicSigningKey
    )