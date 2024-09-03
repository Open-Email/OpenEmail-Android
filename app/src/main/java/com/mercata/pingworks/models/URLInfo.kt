package com.mercata.pingworks.models

import java.net.URL
import java.time.ZonedDateTime

data class URLInfo(
    val url: URL?,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedAt: ZonedDateTime,
)
