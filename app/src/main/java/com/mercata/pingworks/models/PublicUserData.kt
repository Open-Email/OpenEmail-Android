package com.mercata.pingworks.models

import java.time.ZonedDateTime


data class PublicUserData(
    val fullName: String,
    val lastSeenPublic: Boolean,
    val lastSeen: ZonedDateTime?,
    val updated: ZonedDateTime?,
    val encryptionKeyId: String,
    val encryptionKeyAlgorithm: String,
    val signingKeyAlgorithm: String,
    val publicEncryptionKey: String,
    val publicSigningKey: String,
)