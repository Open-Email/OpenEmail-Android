package com.mercata.openemail.models

import android.net.Uri
import com.mercata.openemail.db.HomeItem

data class CachedAttachment(val uri: Uri, val name: String, val type: String?) : HomeItem {

    override fun getContacts(): List<PublicUserData> = listOf()

    override fun getAddressValue(): String? = null

    override fun getTitle(): String = name

    override fun getSubtitle(): String? = null

    override fun getTextBody(): String = type ?: ""

    override fun getMessageId(): String = uri.toString()

    override fun getAttachmentsAmount(): Int? = null

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long? = null
}