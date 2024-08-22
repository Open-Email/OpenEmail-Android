package com.mercata.pingworks.models

import java.time.ZonedDateTime

data class BroadcastMessage(
    override val id: String,
    override val subject: String,
    override val body: String,
    override val person: Person,
    override val date: ZonedDateTime,
) : Message
