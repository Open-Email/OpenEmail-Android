package com.mercata.pingworks.db.drafts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DBDraft(
    @PrimaryKey @ColumnInfo("draft_id") val draftId: String,
    @ColumnInfo("attachment_uris") val attachmentUriList: String?, // joined to string with "," separator
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("text_body") val textBody: String,
    @ColumnInfo("is_broadcast") val isBroadcast: Boolean,
    @ColumnInfo("timestamp") val timestamp: Long,
    @ColumnInfo("reader") val readerAddresses: String? // joined to string with "," separator
)
