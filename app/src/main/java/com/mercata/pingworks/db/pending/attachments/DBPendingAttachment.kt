package com.mercata.pingworks.db.pending.attachments

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mercata.pingworks.db.pending.readers.DBPendingReaderPublicData
import com.mercata.pingworks.models.ContentHeaders
import com.mercata.pingworks.models.MessageCategory
import com.mercata.pingworks.models.URLInfo
import com.mercata.pingworks.utils.Address
import java.time.Instant

@Entity
data class DBPendingAttachment(
    @PrimaryKey @ColumnInfo("message_id") val messageId: String,
    @ColumnInfo("subject_id") val subjectId: String?,
    @ColumnInfo("parent_id", index = true) val parentId: String,
    @ColumnInfo("uri") val uri: String,
    @ColumnInfo("file_name") val fileName: String,
    @ColumnInfo("mime_name") val mimeType: String,
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("sending_date_timestamp") val sendingDateTimestamp: Long,
    @ColumnInfo("size") val fullSize: Long,
    @ColumnInfo("modified_at_timestamp") val modifiedAtTimestamp: Long,
    @ColumnInfo("part_number") val partNumber: Int,
    @ColumnInfo("part_size") val partSize: Long,
    @ColumnInfo("checkSum") val checkSum: String,
    @ColumnInfo("offset") val offset: Long?,
    @ColumnInfo("total_parts") val totalParts: Int,
    @ColumnInfo("is_broadcast") val isBroadcast: Boolean,
) {
    fun getUrlInfo() = URLInfo(
        uri = Uri.parse(uri),
        name = fileName,
        mimeType = mimeType,
        size = fullSize,
        modifiedAt = Instant.ofEpochMilli(modifiedAtTimestamp)
    )

    fun getContentHeaders(
        currentUserAddress: Address,
        recipients: List<DBPendingReaderPublicData>
    ) = ContentHeaders(
        messageID = this.messageId,
        subjectId = this.subjectId,
        date = Instant.ofEpochMilli(this.sendingDateTimestamp),
        subject = this.subject,
        parentId = this.parentId,
        checksum = this.checkSum,
        category = MessageCategory.personal,
        size = this.fullSize,
        authorAddress = currentUserAddress,
        readersAddresses = recipients.map { it.address },
    )
}
