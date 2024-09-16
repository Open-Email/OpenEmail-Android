package com.mercata.pingworks.models

import android.net.Uri

data class ComposingData(
    val recipients: List<PublicUserData>,
    val subject: String,
    val body: String,
    val attachments: List<Uri>
)
