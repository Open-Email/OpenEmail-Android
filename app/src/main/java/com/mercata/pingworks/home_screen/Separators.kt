@file:OptIn(ExperimentalUuidApi::class)

package com.mercata.pingworks.home_screen

import android.content.Context
import com.mercata.pingworks.R
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.models.PublicUserData
import kotlin.uuid.ExperimentalUuidApi

abstract class Separator: HomeItem {
    abstract fun getSeparatorTitle(context: Context): String

    override fun getContacts(): List<PublicUserData> = listOf()

    override fun getAddressValue(): String? = null

    override fun getTitle(): String = ""

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