package com.mercata.openemail.db.messages

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.db.attachments.DBAttachment
import com.mercata.openemail.db.contacts.DBContact
import com.mercata.openemail.db.contacts.toPublicUserData
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall

data class DBMessageWithDBAttachments(
    @Embedded val message: DBMessage,
    @Relation(
        parentColumn = "message_id",
        entityColumn = "parent_id"
    )
    val attachmentParts: List<DBAttachment>,
    @Relation(
        parentColumn = "author_address",
        entityColumn = "address"
    )
    val author: DBContact?
) : HomeItem {

    override suspend fun getContacts(): List<PublicUserData> {
        return author?.let { listOf(it.toPublicUserData()) } ?: message.getAuthorPublicData()?.let { listOf(it) } ?: listOf()
    }

    override suspend fun getTitle(): String = getContacts().firstOrNull()?.fullName ?: ""
    override fun getAuthorAddressValue(): String = message.authorAddress
    override fun getSubject() = message.subject
    override fun getTextBody() = message.textBody
    override fun getMessageId() = message.messageId
    override fun getAttachmentsAmount(): Int = getFusedAttachments().size
    override fun isUnread(): Boolean = message.isUnread
    override fun getTimestamp(): Long = message.timestamp

    override fun matchedSearchQuery(query: String, currentUserData: UserData): Boolean {
        val authored = author?.address == currentUserData.address

        val predicate = if (authored) {
            message.readerAddresses?.split(",")
                ?.any { readerAddress -> readerAddress.contains(query, true) } ?: false
        } else {
            author?.name?.contains(query, true) ?: false ||
                    author?.address?.contains(query, true) ?: false
        }

        return message.subject.contains(query, true) ||
                message.textBody.contains(query, true) ||
                predicate
    }

    fun getFusedAttachments(): List<FusedAttachment> =
        attachmentParts.groupBy { dbAttachment -> dbAttachment.name }.map { multipart ->
            FusedAttachment(
                multipart.key,
                multipart.value.first().fileSize,
                multipart.value.first().type,
                multipart.value
            )
        }
}

data class FusedAttachment(
    val name: String,
    val fileSize: Long,
    val fileType: String,
    val parts: List<DBAttachment>
)