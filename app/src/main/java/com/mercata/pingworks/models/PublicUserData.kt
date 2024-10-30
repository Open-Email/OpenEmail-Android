package com.mercata.pingworks.models

import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.drafts.draft_reader.DBDraftReader
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

fun PublicUserData.toDBContact() = DBContact(
    address = this.address,
    lastSeenPublic = this.lastSeenPublic,
    encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
    signingKeyAlgorithm = this.signingKeyAlgorithm,
    publicEncryptionKey = this.publicEncryptionKey,
    publicSigningKey = this.publicSigningKey,
    updated = this.updated?.toString(),
    lastSeen = this.lastSeen?.toString(),
    name = this.fullName,
    receiveBroadcasts = true,
    imageUrl = null, //TODO
    publicEncryptionKeyId = this.encryptionKeyId
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

fun PublicUserData.toDBDraftReader(draftId: String) = DBDraftReader(
    fullName = fullName,
    imageUrl = imageUrl,
    address = address,
    lastSeenPublic = lastSeenPublic,
    lastSeenTimestamp = lastSeen?.toEpochMilli(),
    updatedTimestamp = updated?.toEpochMilli(),
    encryptionKeyId = encryptionKeyId,
    encryptionKeyAlgorithm = encryptionKeyAlgorithm,
    signingKeyAlgorithm = signingKeyAlgorithm,
    publicEncryptionKey = publicEncryptionKey,
    publicSigningKey = publicSigningKey,
    draftId = draftId
)