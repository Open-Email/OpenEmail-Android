package com.mercata.pingworks.models

import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.pending.readers.DBPendingReaderPublicData
import com.mercata.pingworks.utils.Address
import java.time.Instant
import java.util.UUID


data class PublicUserData(
    val fullName: String,
    val imageUrl: String?,
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
    imageUrl = this.imageUrl,
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

fun DBPendingReaderPublicData.toPublicUserData() = PublicUserData(
    fullName = this.fullName,
    imageUrl = this.imageUrl,
    address = this.address,
    lastSeenPublic = this.lastSeenPublic,
    lastSeen = this.lastSeenTimestamp?.let { Instant.ofEpochMilli(it) },
    updated = this.updatedTimestamp?.let { Instant.ofEpochMilli(it) },
    encryptionKeyId = this.encryptionKeyId,
    encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
    signingKeyAlgorithm = this.signingKeyAlgorithm,
    publicEncryptionKey = this.publicEncryptionKey,
    publicSigningKey = this.publicSigningKey
)

fun DBContact.toPublicUserData(): PublicUserData =
    PublicUserData(
        fullName = this.name ?: "",
        imageUrl = this.imageUrl,
        address = this.address,
        lastSeenPublic = this.lastSeenPublic,
        lastSeen = this.lastSeen?.let { Instant.parse(it) },
        updated = this.lastSeen?.let { Instant.parse(it) },
        encryptionKeyId = this.publicEncryptionKeyId,
        encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
        signingKeyAlgorithm = this.signingKeyAlgorithm,
        publicEncryptionKey = this.publicEncryptionKey,
        publicSigningKey = this.publicSigningKey
    )