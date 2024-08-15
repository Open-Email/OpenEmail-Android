package com.mercata.pingworks.models

import java.time.LocalDateTime

data class BroadcastMessage(
    override val subject: String,
    override val body: String,
    override val person: Person,
    override val date: LocalDateTime
) : Message
