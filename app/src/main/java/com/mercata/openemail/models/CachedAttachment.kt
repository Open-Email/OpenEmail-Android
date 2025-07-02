package com.mercata.openemail.models

import android.net.Uri
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.registration.UserData

data class CachedAttachment(val uri: Uri, val name: String, val type: String?) : HomeItem {

    override suspend fun getContacts(): List<PublicUserData> = listOf()

    override fun getAuthorAddressValue(): String? = null

    override suspend fun getTitle(): String = name

    override fun getSubject(): String? = null

    override fun getTextBody(): String = type ?: ""

    override fun getMessageId(): String = uri.toString()

    override fun getAttachmentsAmount(): Int? = null

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long? = null

    override fun matchedSearchQuery(query: String, currentUserData: UserData): Boolean = name.contains(query, true)
}