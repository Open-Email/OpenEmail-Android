package com.mercata.openemail.db.pending

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.db.pending.attachments.DBPendingAttachment
import com.mercata.openemail.db.pending.messages.DBPendingRootMessage
import com.mercata.openemail.db.pending.readers.DBPendingReaderPublicData
import com.mercata.openemail.db.pending.readers.toPublicUserData
import com.mercata.openemail.models.ContentHeaders
import com.mercata.openemail.models.MessageCategory
import com.mercata.openemail.models.MessageFilePartInfo
import java.time.Instant

data class DBPendingMessage(
    @Embedded val message: DBPendingRootMessage,
    @Relation(
        parentColumn = "message_id",
        entityColumn = "parent_id"
    )
    val fileParts: List<DBPendingAttachment>,
    @Relation(
        parentColumn = "message_id",
        entityColumn = "message_id"
    )
    val readers: List<DBPendingReaderPublicData>
) : HomeItem {
    override fun getContacts() = readers.map { it.toPublicUserData() }
    override fun getTitle(): String = readers.first().fullName
    override fun getAddressValue(): String = message.authorAddress
    override fun getSubtitle() = message.subject
    override fun getTextBody(): String = message.textBody
    override fun getMessageId() = message.messageId
    override fun getAttachmentsAmount(): Int = fileParts.size
    override fun isUnread(): Boolean = false
    override fun getTimestamp(): Long = message.timestamp

    fun getRootContentHeaders() = ContentHeaders(messageID = message.messageId,
        date = Instant.ofEpochMilli(message.timestamp),
        subject = message.subject,
        parentId = null,
        subjectId = message.subjectId,
        fileParts = fileParts.map { dbPart ->
            MessageFilePartInfo(
                urlInfo = dbPart.getUrlInfo(),
                messageId = dbPart.messageId,
                part = dbPart.partNumber,
                size = dbPart.partSize,
                checksum = dbPart.checkSum,
                offset = dbPart.offset,
                totalParts = dbPart.totalParts
            )
        },
        category = MessageCategory.getByName(message.category),
        size = message.size,
        checksum = message.checksum,
        authorAddress = message.authorAddress,
        readersAddresses = if (message.isBroadcast) null else readers.map { it.address }
    )
}
