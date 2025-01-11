package com.mercata.pingworks.db

import com.mercata.pingworks.models.PublicUserData

interface HomeItem {
    fun getContacts(): List<PublicUserData>
    fun getSubject(): String
    fun getTextBody(): String
    fun getMessageId(): String
    fun getAttachmentsAmount(): Int?
    fun isUnread(): Boolean
    fun getTimestamp(): Long?
}