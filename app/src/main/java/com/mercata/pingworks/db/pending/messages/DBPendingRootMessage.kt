package com.mercata.pingworks.db.pending.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mercata.pingworks.db.contacts.DBContact

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
data class DBPendingRootMessage(
    @PrimaryKey @ColumnInfo("message_id", index = true) val messageId: String,
    @ColumnInfo("subject_id") val subjectId: String?,
    @ColumnInfo("timestamp") val timestamp: Long,
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("checksum") val checksum: String,
    @ColumnInfo("category") val category: String,
    @ColumnInfo("size") val size: Long,
    @ColumnInfo("author_address", index = true) val authorAddress: String,
    @ColumnInfo("text_body") val textBody: String,
    @ColumnInfo("is_broadcast") val isBroadcast: Boolean,
)

