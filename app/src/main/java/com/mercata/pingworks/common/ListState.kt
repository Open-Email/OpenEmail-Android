package com.mercata.pingworks.common

import com.mercata.pingworks.models.Message

interface ListState {
    val messages: List<Message>
}