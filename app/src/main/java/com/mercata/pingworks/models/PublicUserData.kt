package com.mercata.pingworks.models

import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.utils.Address
import java.time.Instant


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