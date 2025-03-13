package com.mercata.openemail.models

enum class MessageCategory {
    personal,
    chat,
    transitory,
    notification,
    transaction,
    promotion,
    letter,
    file,
    informational,
    pass,
    funds,
    encryptionKey,
    signingKey;

    companion object {
        fun getByName(name: String?) = MessageCategory.entries.firstOrNull { it.name == name } ?: personal
    }
}