package com.mercata.pingworks.models

import com.mercata.pingworks.utils.Address
import java.time.ZonedDateTime


data class PublicUserData(
    val fullName: String,
    val address: Address,
    val lastSeenPublic: Boolean,
    val lastSeen: ZonedDateTime?,
    val updated: ZonedDateTime?,
    val encryptionKeyId: String,
    val encryptionKeyAlgorithm: String,
    val signingKeyAlgorithm: String,
    val publicEncryptionKey: String,
    val publicSigningKey: String,
)
