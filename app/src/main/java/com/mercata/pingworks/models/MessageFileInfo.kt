package com.mercata.pingworks.models

import java.time.Instant

data class MessageFileInfo(
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedAt: Instant,
    val messageIds: List<String>,
    val complete: Boolean,
)
