package com.mercata.pingworks.models

import com.mercata.pingworks.DEFAULT_MAIL_SUBDOMAIN
import com.mercata.pingworks.getHost
import java.time.LocalDateTime

data class Person(
    val name: String?,
    val createdAt: LocalDateTime,
    val imageUrl: String?,
    val address: Address,
    val receiveBroadcasts: Boolean,
    val encryptionKeyAlgorithm: String,
    val signingKeyAlgorithm: String,
    val publicEncryptionKey: String,
    val publicSigningKey: String,
) {
    fun getMailHost() = "$DEFAULT_MAIL_SUBDOMAIN.${address.getHost()}"
}

typealias Address = String