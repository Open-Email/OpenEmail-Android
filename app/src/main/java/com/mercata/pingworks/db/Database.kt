package com.mercata.pingworks.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mercata.pingworks.db.contacts.ContactsDao
import com.mercata.pingworks.db.contacts.DBContact

@Database(entities = [DBContact::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): ContactsDao
}