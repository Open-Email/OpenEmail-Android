package com.mercata.pingworks.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mercata.pingworks.db.attachments.AttachmentsDao
import com.mercata.pingworks.db.attachments.DBAttachment
import com.mercata.pingworks.db.contacts.ContactsDao
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.messages.DBMessage
import com.mercata.pingworks.db.messages.MessagesDao
import com.mercata.pingworks.db.pending.attachments.DBPendingAttachment
import com.mercata.pingworks.db.pending.attachments.PendingAttachmentsDao
import com.mercata.pingworks.db.pending.messages.DBPendingRootMessage
import com.mercata.pingworks.db.pending.messages.PendingMessagesDao
import com.mercata.pingworks.db.pending.readers.DBPendingReaderPublicData
import com.mercata.pingworks.db.pending.readers.PendingReadersDao

@Database(
    entities = [DBContact::class,
        DBMessage::class,
        DBAttachment::class,
        DBPendingRootMessage::class,
        DBPendingAttachment::class,
        DBPendingReaderPublicData::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): ContactsDao
    abstract fun messagesDao(): MessagesDao
    abstract fun attachmentsDao(): AttachmentsDao
    abstract fun pendingMessagesDao(): PendingMessagesDao
    abstract fun pendingAttachmentsDao(): PendingAttachmentsDao
    abstract fun pendingReadersDao(): PendingReadersDao
}