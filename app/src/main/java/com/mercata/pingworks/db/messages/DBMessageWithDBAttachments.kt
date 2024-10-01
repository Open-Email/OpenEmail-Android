package com.mercata.pingworks.db.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.attachments.DBAttachment
import com.mercata.pingworks.models.toPublicUserData

data class DBMessageWithDBAttachments(
    @Embedded val message: MessageWithAuthor,
    @Relation(
        parentColumn = "message_id",
        entityColumn = "parent_id"
    )
    val attachments: List<DBAttachment>
) : HomeItem {
    override fun getContacts() = message.author?.toPublicUserData()?.let { listOf(it) } ?: listOf()
    override fun getSubject() = message.message.subject
    override fun getTextBody() = message.message.textBody
    override fun getMessageId() = message.message.messageId
}
