package com.mercata.openemail.db

import com.mercata.openemail.models.PublicUserData

interface HomeItem {
    fun getContacts(): List<PublicUserData>
    fun getAddressValue(): String?
    fun getTitle(): String
    fun getSubtitle(): String?
    fun getTextBody(): String
    fun getMessageId(): String
    fun getAttachmentsAmount(): Int?
    fun isUnread(): Boolean
    fun getTimestamp(): Long?
}