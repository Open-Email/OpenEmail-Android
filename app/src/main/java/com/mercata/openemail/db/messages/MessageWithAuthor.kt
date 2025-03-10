package com.mercata.openemail.db.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.mercata.openemail.db.contacts.DBContact

data class MessageWithAuthor(
    @Embedded val message: DBMessage,
    @Relation(
        parentColumn = "author_address",
        entityColumn = "address"
    )
    val author: DBContact?
)
