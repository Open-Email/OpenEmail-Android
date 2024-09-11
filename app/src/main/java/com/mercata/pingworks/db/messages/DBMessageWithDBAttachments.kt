package com.mercata.pingworks.db.messages

import androidx.room.Embedded
import androidx.room.Relation

data class DBMessageWithDBAttachments(
    @Embedded val message: MessageWithAuthor,
    @Relation(
        parentColumn = "message_id",
        entityColumn = "parent_id"
    )
    val attachments: List<DBAttachment>
)
