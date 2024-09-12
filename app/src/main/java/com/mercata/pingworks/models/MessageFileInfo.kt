package com.mercata.pingworks.models

import java.time.ZonedDateTime

data class MessageFileInfo(
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedAt: String,
    val messageIds: List<String>,
    val complete: Boolean,
)
