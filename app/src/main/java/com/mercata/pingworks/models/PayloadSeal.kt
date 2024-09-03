package com.mercata.pingworks.models

data class PayloadSeal(
    val algorithm: String,
    val stream: Boolean,
    val chunkSize: Int,
    val originalHeaderValue: String
)