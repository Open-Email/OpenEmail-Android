package com.mercata.openemail.db.archive

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.db.archive.archive_attachments.DBArchivedAttachment
import com.mercata.openemail.db.attachments.DBAttachment
import com.mercata.openemail.db.contacts.DBContact
import com.mercata.openemail.db.contacts.toPublicUserData
import com.mercata.openemail.db.messages.DBMessageWithDBAttachments
import com.mercata.openemail.db.messages.FusedAttachment
import com.mercata.openemail.models.PublicUserData

data class DBArchiveWitAttachments(
    @Embedded val archive: DBArchivedMessage,

    @Relation(
        parentColumn = "archive_id",
        entityColumn = "parent_id"
    )
    val attachments: List<DBArchivedAttachment>,

    @Relation(
        parentColumn = "author_address",
        entityColumn = "address"
    )
    val author: DBContact?
) : HomeItem {

    override suspend fun getContacts(): List<PublicUserData> {
        return author?.let { listOf(it.toPublicUserData()) } ?: archive.getAuthorPublicData()
            ?.let { listOf(it) } ?: listOf()
    }

    override suspend fun getTitle(): String = getContacts().firstOrNull()?.fullName ?: ""

    override fun getAuthorAddressValue(): String {
        return archive.readerAddresses?.split(",")?.firstOrNull()
            .takeIf { it?.isNotBlank() == true } ?: archive.authorAddress
    }

    override fun getSubject(): String = archive.subject

    override fun getTextBody(): String = archive.textBody

    override fun getMessageId(): String = archive.archiveId

    override fun getAttachmentsAmount(): Int = getFusedAttachments().size

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long = archive.timestamp

    fun getFusedAttachments(): List<FusedAttachment> =
        attachments.groupBy { dbAttachment -> dbAttachment.name }.map { multipart ->
            FusedAttachment(
                multipart.key,
                multipart.value.first().fileSize,
                multipart.value.first().type,
                multipart.value.map { it.fromArchive() })
        }
}


fun DBMessageWithDBAttachments.toArchive(): DBArchivedMessage =
    DBArchivedMessage(
        archiveId = message.messageId,
        authorAddress = message.authorAddress,
        subject = message.subject,
        textBody = message.textBody,
        isBroadcast = message.isBroadcast,
        isUnread = message.isUnread,
        timestamp = message.timestamp,
        readerAddresses = message.readerAddresses,
    )

fun DBAttachment.toArchive(): DBArchivedAttachment = DBArchivedAttachment(
    attachmentMessageId = attachmentMessageId,
    accessKey = accessKey,
    authorAddress = authorAddress,
    parentId = parentId,
    name = name,
    type = type,
    fileSize = fileSize,
    partSize = partSize,
    partIndex = partIndex,
    partsAmount = partsAmount,
    createdTimestamp = createdTimestamp,
)


fun DBArchivedAttachment.fromArchive(): DBAttachment = DBAttachment(
    attachmentMessageId = attachmentMessageId,
    accessKey = accessKey,
    authorAddress = authorAddress,
    parentId = parentId,
    name = name,
    type = type,
    fileSize = fileSize,
    partSize = partSize,
    partIndex = partIndex,
    partsAmount = partsAmount,
    createdTimestamp = createdTimestamp,
)