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
    @ColumnInfo("parent_id") val parentId: String,
    @ColumnInfo("download_link") val downloadLink: String,
    @ColumnInfo("file_name") val name: String
)
