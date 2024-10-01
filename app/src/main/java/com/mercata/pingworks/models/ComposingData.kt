package com.mercata.pingworks.models

import java.io.File

data class ComposingData(
    val recipients: List<PublicUserData>,
    val subject: String,
    val body: String,
    val attachments: List<File>
)
