package com.mercata.openemail.db

import com.mercata.openemail.home_screen.HomeScreen
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.registration.UserData

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
    fun matchedSearchQuery(query: String, currentUserData: UserData): Boolean
}