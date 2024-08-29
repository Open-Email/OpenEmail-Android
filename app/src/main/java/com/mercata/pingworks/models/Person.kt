package com.mercata.pingworks.models

import java.time.LocalDateTime

data class Person(
    val id: String,
    val name: String?,
    val createdAt: LocalDateTime,
    val imageUrl: String?,
    val address: String,
    val receiveBroadcasts: Boolean,
    val encryptionKeyAlgorithm: String,
    val signingKeyAlgorithm: String,
    val publicEncryptionKey: String,
    val publicSigningKey: String,
)
