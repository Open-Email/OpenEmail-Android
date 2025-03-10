package com.mercata.openemail.models

data class Attachment(
    val id: String,
    val parentMessageId: String,
    val fileMessageIds: List<String>,
    val filename: String,
    val size: Long,
    val mimeType: String
)

