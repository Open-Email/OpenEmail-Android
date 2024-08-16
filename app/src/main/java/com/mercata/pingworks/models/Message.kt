package com.mercata.pingworks.models

import java.time.LocalDateTime

interface Message {
    val id: String
    val subject: String
    val body: String
    val person: Person?
    val date: LocalDateTime
}