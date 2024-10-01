package com.mercata.pingworks.db.pending

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.pending.attachments.DBPendingAttachment
import com.mercata.pingworks.db.pending.messages.DBPendingRootMessage
import com.mercata.pingworks.db.pending.readers.DBPendingReaderPublicData
import com.mercata.pingworks.models.ContentHeaders
import com.mercata.pingworks.models.MessageCategory
import com.mercata.pingworks.models.MessageFilePartInfo
import com.mercata.pingworks.models.toPublicUserData
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
    override fun getSubject() = message.subject
    override fun getTextBody(): String = message.textBody
    override fun getMessageId() = message.messageId

    fun getRootContentHeaders() = ContentHeaders(messageID = message.messageId,
        date = Instant.ofEpochMilli(message.timestamp),
        subject = message.subject,
        parentId = null,
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
        readersAddresses = readers.map { it.address }
    )
}
