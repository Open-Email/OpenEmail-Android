package com.mercata.openemail.db.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity/*(
    foreignKeys = [
        ForeignKey(
            entity = DBContact::class,
            parentColumns = ["address"],
            childColumns = ["author_address"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)*/
data class DBMessage(
    @PrimaryKey @ColumnInfo("message_id") val messageId: String,
    @ColumnInfo("author_address", index = true) val authorAddress: String,
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("text_body") val textBody: String,
    @ColumnInfo("is_broadcast") val isBroadcast: Boolean,
    @ColumnInfo("is_unread") val isUnread: Boolean,
    @ColumnInfo("marked_to_delete") val markedToDelete: Boolean,
    @ColumnInfo("timestamp") val timestamp: Long,
    @ColumnInfo("reader") val readerAddresses: String? // joined to string with "," separator
)
