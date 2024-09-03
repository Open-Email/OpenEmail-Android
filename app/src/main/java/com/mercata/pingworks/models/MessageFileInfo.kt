package com.mercata.pingworks.models

import java.time.ZonedDateTime

data class MessageFileInfo(
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedAt: ZonedDateTime,
    val messageIds: List<String>,
    val complete: Boolean,
)
