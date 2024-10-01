package com.mercata.pingworks.models

data class MessageFilePartInfo(
    val urlInfo: URLInfo,
    val messageId: String,
    val part: Int,
    val size: Long,
    val checksum: String? = null,
    val offset: Long? = null,
    val totalParts: Int,
)
