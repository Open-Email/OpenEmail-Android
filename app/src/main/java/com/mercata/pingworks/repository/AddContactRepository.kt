package com.mercata.pingworks.repository

import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBContact
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

class AddContactRepository(
    private val sp: SharedPreferences,
    private val db: AppDatabase,
    private val dl: DownloadRepository
) {

    private val _addingState = MutableSharedFlow<Boolean>()
    val addingState: SharedFlow<Boolean> = _addingState

    suspend fun addContact(publicData: PublicUserData) {
        withContext(Dispatchers.IO) {
            _addingState.emit(false)
            val dbContact = publicData.toDBContact().copy(uploaded = false)
            db.userDao().insert(dbContact)

            db.notificationsDao().let { dao ->
                dao.getByAddress(publicData.address)?.let {
                    dao.update(it.copy(dismissed = true))
                }
            }
            syncWithServer()
            _addingState.emit(true)
        }

    }

    suspend fun syncWithServer() {
        withContext(Dispatchers.IO) {
            syncContacts(sp, db.userDao())
            syncAllMessages(db, sp, dl)
        }
    }
}