package com.mercata.pingworks.models

import com.mercata.pingworks.db.contacts.DBContact
import java.time.ZonedDateTime

interface Message {
    val id: String
    val subject: String
    val body: String
    val person: DBContact?
    val date: ZonedDateTime
}