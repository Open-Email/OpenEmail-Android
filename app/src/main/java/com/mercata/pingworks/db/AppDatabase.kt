package com.mercata.pingworks.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mercata.pingworks.db.contacts.AttachmentsDao
import com.mercata.pingworks.db.contacts.ContactsDao
import com.mercata.pingworks.db.messages.DBAttachment
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.messages.DBMessage
import com.mercata.pingworks.db.messages.MessagesDao

@Database(entities = [DBContact::class, DBMessage::class, DBAttachment::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): ContactsDao
    abstract fun messagesDao(): MessagesDao
    abstract fun attachmentsDao(): AttachmentsDao
}