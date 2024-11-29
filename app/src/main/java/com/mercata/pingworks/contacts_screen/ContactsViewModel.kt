package com.mercata.pingworks.contacts_screen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.notifications.DBNotification
import com.mercata.pingworks.db.notifications.toPublicUserData
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBContact
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.syncNotifications
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class ContactsViewModel : AbstractViewModel<ContactsState>(ContactsState()) {
    init {
        val db: AppDatabase by inject()
        val sp: SharedPreferences by inject()

        viewModelScope.launch(Dispatchers.IO) {
            db.userDao().getAllAsFlow().collect { dbEntities ->
                currentState.contacts.clear()
                currentState.contacts.addAll(dbEntities.filterNot {
                    it.address == sp.getUserAddress() || it.markedToDelete
                }.toList())
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            db.notificationsDao().getAllAsFlow().collect { dbEntities ->
                currentState.notifications.clear()
                currentState.notifications.addAll(dbEntities.filterNot {
                    it.address == sp.getUserAddress() || it.isExpired() || it.dismissed
                }.toList())
            }
        }

        viewModelScope.launch {
            delay(100)
            refresh()
        }

        updateState(currentState.copy(loggedInPersonAddress = sp.getUserAddress()!!))
    }

    private val downloadRepository: DownloadRepository by inject()

    fun onNewContactAddressInput(str: String) {
        updateState(
            currentState.copy(
                newContactAddressInput = str,
                searchButtonActive = emailValid()
            )
        )
    }

    fun searchNewContact() {
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val address = currentState.newContactAddressInput.trim()
            val publicData: PublicUserData? =
                when (val call = safeApiCall { getProfilePublicData(address) }) {
                    is HttpResult.Success -> {
                        call.data
                    }

                    is HttpResult.Error -> {
                        null
                    }
                }
            publicData?.let {
                updateState(currentState.copy(existingContactFound = it))
            }
            updateState(currentState.copy(loading = false))
        }
    }

    private fun emailValid(): Boolean {
        if (currentState.newContactAddressInput.isBlank()) {
            return false
        }

        return currentState.newContactAddressInput.lowercase().matches(emailRegex)
    }

    fun addContact() {
        viewModelScope.launch(Dispatchers.IO) {
            val publicData = currentState.existingContactFound!!
            val dbContact = publicData.toDBContact().copy(uploaded = false)
            db.userDao().insert(dbContact)
            toggleSearchAddressDialog(false)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun approveRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(existingContactFound = currentState.requestDetails!!.toPublicUserData()))
            addContact()
            db.notificationsDao().update(currentState.requestDetails!!.copy(dismissed = true))
            closeNotificationDetails()
        }.invokeOnCompletion {
            GlobalScope.launch(Dispatchers.IO) {
                val db: AppDatabase by inject()
                val sp: SharedPreferences by inject()
                val dl: DownloadRepository by inject()
                syncAllMessages(db, sp, dl)
            }
        }
    }

    fun toggleSearchAddressDialog(isShown: Boolean) {
        updateState(currentState.copy(newContactSearchDialogShown = isShown))
        if (!isShown) {
            updateState(currentState.copy(newContactAddressInput = ""))
            clearFoundContact()
        }
    }

    fun clearFoundContact() {
        updateState(currentState.copy(existingContactFound = null))
    }

    fun onUndoDeletePressed() {
        updateState(
            currentState.copy(
                itemToDelete = null,
            )
        )
    }

    fun onSnackBarCountdownFinished() {
        currentState.itemToDelete?.let {
            viewModelScope.launch {
                onDeleteWaitComplete()
            }
        }
    }

    private suspend fun onDeleteWaitComplete() {
        val item = currentState.itemToDelete ?: return
        withContext(Dispatchers.IO) {
            when(item) {
                is DBContact -> {
                    downloadRepository.deleteAttachmentsForMessages(
                        db.messagesDao().getAllForContactAddress(item.address)
                    )
                    db.userDao().update(item.copy(markedToDelete = true))
                }
                is DBNotification -> {
                    db.notificationsDao().update(item.copy(dismissed = true))
                }
            }

            updateState(currentState.copy(itemToDelete = null))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun syncWithServer() {
        GlobalScope.launch(Dispatchers.IO) {
            onDeleteWaitComplete()
            val db: AppDatabase by inject()
            val sp: SharedPreferences by inject()
            val dl: DownloadRepository by inject()
            syncContacts(sp, db.userDao())
            syncAllMessages(db, sp, dl)
        }
    }

    fun removeItem(item: ContactItem) {
        viewModelScope.launch {
            onDeleteWaitComplete()
            updateState(currentState.copy(itemToDelete = item))
        }
    }

    fun showRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(addRequestsToContactsDialogShown = true))
    }

    fun hideRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(addRequestsToContactsDialogShown = false))
    }

    override fun onCleared() {
        syncWithServer()
        super.onCleared()
    }

    fun toggleSelect(person: ContactItem) {
        if (currentState.selectedContacts.contains(person)) {
            currentState.selectedContacts.remove(person)
        } else {
            currentState.selectedContacts.add(person)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            updateState(currentState.copy(refreshing = true))

            syncContacts(sp, db.userDao())
            syncNotifications(sp.getUserData()!!, db)

            updateState(currentState.copy(refreshing = false))
        }
    }

    suspend fun addSelectedNotificationsToContacts() {
        withContext(Dispatchers.IO) {
            val selectedRequests = currentState.selectedContacts.filterIsInstance<DBNotification>()
            val approvedRequests = selectedRequests.map { request ->
                request.toPublicUserData().toDBContact().copy(uploaded = false)
            }

            db.userDao().insertAll(approvedRequests)
            db.notificationsDao().deleteList(selectedRequests)
            hideRequestApprovingConfirmationDialog()
        }
    }

    fun clearSelectedRequests() {
        currentState.selectedContacts.removeAll { it is DBNotification }
    }

    fun openNotificationDetails(person: DBNotification) {
        updateState(currentState.copy(requestDetails = person))
    }

    fun closeNotificationDetails() {
        updateState(currentState.copy(requestDetails = null))
    }
}


data class ContactsState(
    val selectedContacts: SnapshotStateList<ContactItem> = mutableStateListOf(),
    val notifications: SnapshotStateList<DBNotification> = mutableStateListOf(),
    val contacts: SnapshotStateList<DBContact> = mutableStateListOf(),
    val requestDetails: DBNotification? = null,
    val itemToDelete: ContactItem? = null,
    val searchInput: String = "",
    val newContactAddressInput: String = "",
    val loggedInPersonAddress: String = "",
    val existingContactFound: PublicUserData? = null,
    val searchButtonActive: Boolean = false,
    val addRequestsToContactsDialogShown: Boolean = false,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val newContactSearchDialogShown: Boolean = false,
)

interface ContactItem {
    val name: String?
    val address: String
    val key: String
}