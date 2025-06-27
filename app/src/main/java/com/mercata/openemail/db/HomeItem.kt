package com.mercata.openemail.db

import com.mercata.openemail.models.PublicUserData

interface HomeItem {
    suspend fun getContacts(): List<PublicUserData>
    fun getAuthorAddressValue(): String?
    suspend fun getTitle(): String
    fun getSubject(): String?
    fun getTextBody(): String
    fun getMessageId(): String
    fun getAttachmentsAmount(): Int?
    fun isUnread(): Boolean
    fun getTimestamp(): Long?
}