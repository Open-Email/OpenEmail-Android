package com.mercata.pingworks.models

import android.net.Uri
import com.mercata.pingworks.db.HomeItem

data class CachedAttachment(val uri: Uri, val name: String, val type: String?) : HomeItem {
    override fun getContacts(): List<PublicUserData> = listOf()

    override fun getSubject(): String = name

    override fun getTextBody(): String = type ?: ""

    override fun getMessageId(): String = uri.toString()

    override fun hasAttachments(): Boolean = false
}