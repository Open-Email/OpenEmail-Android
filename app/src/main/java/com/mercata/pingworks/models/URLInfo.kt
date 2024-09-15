package com.mercata.pingworks.models

import android.net.Uri

data class URLInfo(
    val uri: Uri?,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedAt: String,
)
