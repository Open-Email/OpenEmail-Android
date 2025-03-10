package com.mercata.openemail.db.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mercata.openemail.db.contacts.DBContact

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DBContact::class,
            parentColumns = ["address"],
            childColumns = ["author_address"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)
data class DBArchivedMessage(
    @PrimaryKey
    @ColumnInfo("archive_id") val archiveId: String,
    @ColumnInfo("author_address", index = true) val authorAddress: String,
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("text_body") val textBody: String,
    @ColumnInfo("is_broadcast") val isBroadcast: Boolean,
    @ColumnInfo("is_unread") val isUnread: Boolean,
    @ColumnInfo("timestamp") val timestamp: Long,
    @ColumnInfo("reader") val readerAddresses: String? // joined to string with "," separator
)