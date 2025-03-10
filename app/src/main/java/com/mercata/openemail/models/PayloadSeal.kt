package com.mercata.openemail.models

data class PayloadSeal(
    val algorithm: String,
    val stream: Boolean,
    val chunkSize: Int,
    val originalHeaderValue: String
)