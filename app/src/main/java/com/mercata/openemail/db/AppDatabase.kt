package com.mercata.openemail.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mercata.openemail.db.archive.ArchiveDao
import com.mercata.openemail.db.archive.DBArchivedMessage
import com.mercata.openemail.db.archive.archive_attachments.ArchivedAttachmentsDao
import com.mercata.openemail.db.archive.archive_attachments.DBArchivedAttachment
import com.mercata.openemail.db.attachments.AttachmentsDao
import com.mercata.openemail.db.attachments.DBAttachment
import com.mercata.openemail.db.contacts.ContactsDao
import com.mercata.openemail.db.contacts.DBContact
import com.mercata.openemail.db.drafts.DBDraft
import com.mercata.openemail.db.drafts.DraftDao
import com.mercata.openemail.db.drafts.draft_reader.DBDraftReader
import com.mercata.openemail.db.drafts.draft_reader.DraftReaderDao
import com.mercata.openemail.db.messages.DBMessage
import com.mercata.openemail.db.messages.MessagesDao
import com.mercata.openemail.db.notifications.DBNotification
import com.mercata.openemail.db.notifications.NotificationsDao
import com.mercata.openemail.db.pending.attachments.DBPendingAttachment
import com.mercata.openemail.db.pending.attachments.PendingAttachmentsDao
import com.mercata.openemail.db.pending.messages.DBPendingRootMessage
import com.mercata.openemail.db.pending.messages.PendingMessagesDao
import com.mercata.openemail.db.pending.readers.DBPendingReaderPublicData
import com.mercata.openemail.db.pending.readers.PendingReadersDao

@Database(
    entities = [DBContact::class,
        DBMessage::class,
        DBAttachment::class,
        DBPendingRootMessage::class,
        DBPendingAttachment::class,
        DBPendingReaderPublicData::class,
        DBDraft::class,
        DBArchivedMessage::class,
        DBArchivedAttachment::class,
        DBDraftReader::class,
        DBNotification::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): ContactsDao
    abstract fun messagesDao(): MessagesDao
    abstract fun attachmentsDao(): AttachmentsDao
    abstract fun pendingMessagesDao(): PendingMessagesDao
    abstract fun pendingAttachmentsDao(): PendingAttachmentsDao
    abstract fun pendingReadersDao(): PendingReadersDao
    abstract fun draftDao(): DraftDao
    abstract fun draftReaderDao(): DraftReaderDao
    abstract fun notificationsDao(): NotificationsDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun archiveAttachmentsDao(): ArchivedAttachmentsDao
}