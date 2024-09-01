package com.mercata.pingworks.models

import com.mercata.pingworks.db.contacts.DBContact
import java.time.ZonedDateTime

data class BroadcastMessage(
    override val id: String,
    override val subject: String,
    override val body: String,
    override val person: DBContact,
    override val date: ZonedDateTime,
) : Message
