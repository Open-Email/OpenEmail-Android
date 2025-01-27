package com.mercata.pingworks.db.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DBArchivedMessage(
    @PrimaryKey
    @ColumnInfo("archive_id") val archiveId: String,
    @ColumnInfo("author_address", index = true) val authorAddress: String,
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("text_body") val textBody: String,
    @ColumnInfo("is_broadcast") val isBroadcast: Boolean,
    @ColumnInfo("is_unread") val isUnread: Boolean,
    @ColumnInfo("marked_to_delete") val markedToDelete: Boolean,
    @ColumnInfo("timestamp") val timestamp: Long,
    @ColumnInfo("attachmentUrls") val attachmentUrls: String?, // joined to string with "," separator
    @ColumnInfo("reader") val readerAddresses: String? // joined to string with "," separator
)