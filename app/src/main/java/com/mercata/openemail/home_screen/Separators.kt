@file:OptIn(ExperimentalUuidApi::class)

package com.mercata.openemail.home_screen

import android.content.Context
import com.mercata.openemail.R
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.models.PublicUserData
import kotlin.uuid.ExperimentalUuidApi

abstract class Separator: HomeItem {
    abstract fun getSeparatorTitle(context: Context): String

    override suspend fun getContacts(): List<PublicUserData> = listOf()

    override fun getAddressValue(): String? = null

    override suspend fun getTitle(): String = ""

    override fun getSubtitle(): String? = null

    override fun getTextBody(): String = ""

    override fun getMessageId(): String = (hashCode() * 42).toString()

    override fun getAttachmentsAmount(): Int? = null

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long? = null
}

class NotificationSeparator(private val amount: Int): Separator() {
    override fun getSeparatorTitle(context: Context) = String.format(context.getString(R.string.contact_requests), amount)
}

class ContactsSeparator(private val amount: Int): Separator() {
    override fun getSeparatorTitle(context: Context) = String.format(context.getString(R.string.address_book), amount)
}