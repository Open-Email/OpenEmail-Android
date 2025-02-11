package com.mercata.pingworks.db.archive.archive_attachments

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mercata.pingworks.db.archive.DBArchivedMessage

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DBArchivedMessage::class,
            parentColumns = ["archive_id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)
data class DBArchivedAttachment (
    @PrimaryKey @ColumnInfo("attachment_id") val attachmentMessageId: String,
    @ColumnInfo("access_key") val accessKey: ByteArray?,
    @ColumnInfo("author_address") val authorAddress: String,
    @ColumnInfo("parent_id", index = true) val parentId: String,
    @ColumnInfo("file_name") val name: String,
    @ColumnInfo("file_type") val type: String,
    @ColumnInfo("file_size") val fileSize: Long,
    @ColumnInfo("part_size") val partSize: Long,
    @ColumnInfo("part_index") val partIndex: Int,
    @ColumnInfo("parts_amount") val partsAmount: Int,
    @ColumnInfo("created_timestamp") val createdTimestamp: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DBArchivedAttachment

        if (attachmentMessageId != other.attachmentMessageId) return false
        if (accessKey != null) {
            if (other.accessKey == null) return false
            if (!accessKey.contentEquals(other.accessKey)) return false
        } else if (other.accessKey != null) return false
        if (authorAddress != other.authorAddress) return false
        if (parentId != other.parentId) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (fileSize != other.fileSize) return false
        if (partSize != other.partSize) return false
        if (partIndex != other.partIndex) return false
        if (partsAmount != other.partsAmount) return false
        if (createdTimestamp != other.createdTimestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attachmentMessageId.hashCode()
        result = 31 * result + (accessKey?.contentHashCode() ?: 0)
        result = 31 * result + authorAddress.hashCode()
        result = 31 * result + parentId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + partSize.hashCode()
        result = 31 * result + partIndex
        result = 31 * result + partsAmount
        result = 31 * result + createdTimestamp.hashCode()
        return result
    }
}