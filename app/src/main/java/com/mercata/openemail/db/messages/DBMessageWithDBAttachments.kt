package com.mercata.openemail.db.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.db.attachments.DBAttachment
import com.mercata.openemail.db.contacts.toPublicUserData

data class DBMessageWithDBAttachments(
    @Embedded val message: MessageWithAuthor,
    @Relation(
        parentColumn = "message_id",
        entityColumn = "parent_id"
    )
    val attachmentParts: List<DBAttachment>
) : HomeItem {
    override fun getContacts() = message.author?.toPublicUserData()?.let { listOf(it) } ?: listOf()
    override fun getTitle(): String = getContacts().first().fullName
    override fun getAddressValue(): String? = message.author?.address
    override fun getSubtitle() = message.message.subject
    override fun getTextBody() = message.message.textBody
    override fun getMessageId() = message.message.messageId
    override fun getAttachmentsAmount(): Int = getFusedAttachments().size
    override fun isUnread(): Boolean = message.message.isUnread
    override fun getTimestamp(): Long = message.message.timestamp

    fun getFusedAttachments(): List<FusedAttachment> =
        attachmentParts.groupBy { dbAttachment -> dbAttachment.name }.map { multipart ->
            FusedAttachment(multipart.key, multipart.value.first().fileSize, multipart.value.first().type, multipart.value)
        }
}

data class FusedAttachment(val name: String, val fileSize: Long, val fileType: String, val parts: List<DBAttachment>)