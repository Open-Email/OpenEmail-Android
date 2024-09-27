package com.mercata.pingworks.models

import android.net.Uri
import java.time.Instant

data class URLInfo(
    val uri: Uri? = null,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedAt: Instant,
)
