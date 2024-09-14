package com.mercata.pingworks.db.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DBMessage::class,
            parentColumns = ["message_id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)
data class DBAttachment(
    @PrimaryKey @ColumnInfo("attachment_id") val attachmentMessageId: String,
    @ColumnInfo ("access_key") val accessKeyHex: String,
    @ColumnInfo("author_address") val authorAddress: String,
    @ColumnInfo("parent_id") val parentId: String,
    @ColumnInfo("file_name") val name: String,
    @ColumnInfo("file_type") val type: String,
    @ColumnInfo("file_size") val size: Long,
    @ColumnInfo("created_timestamp") val createdTimestamp: String,
)
