package com.mercata.pingworks.contacts_screen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.utils.Downloader
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.utils.deleteContact
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.syncContacts
import com.mercata.pingworks.utils.syncMessagesForContact
import com.mercata.pingworks.utils.uploadContact
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ContactsViewModel : AbstractViewModel<ContactsState>(ContactsState()) {

    init {
        val db: AppDatabase by inject()
        val sp: SharedPreferences by inject()

        viewModelScope.launch {
            syncContacts(sp, db.userDao())
        }

        viewModelScope.launch {
            db.userDao().getAllAsFlow().collect { dbEntities ->
                if (currentState.itemToDelete == null) {
                    currentState.contacts.clear()
                    currentState.contacts.addAll(dbEntities)
                }
            }
        }

        updateState(currentState.copy(loggedInPersonAddress = sp.getUserAddress()!!))
    }

    val downloader: Downloader by inject()

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
                updateState(currentState.copy(newContactFound = it))
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
        viewModelScope.launch {
            val publicData = currentState.newContactFound!!
            updateState(currentState.copy(loadingContactAddress = publicData.address))
            val dbContact = DBContact(
                address = publicData.address,
                name = publicData.fullName.takeUnless { it.isBlank() },
                //TODO update when public profile will contain image
                imageUrl = null,
                receiveBroadcasts = true,
                signingKeyAlgorithm = publicData.signingKeyAlgorithm,
                encryptionKeyAlgorithm = publicData.encryptionKeyAlgorithm,
                publicSigningKey = publicData.publicSigningKey,
                publicEncryptionKey = publicData.publicEncryptionKey,
                lastSeen = publicData.lastSeen?.toString(),
                updated = publicData.updated?.toString()
            )
            db.userDao().insert(dbContact)
            updateContactSearchDialog(false)
            when (safeApiCall {
                uploadContact(
                    contact = publicData,
                    sharedPreferences = sharedPreferences
                )
            }) {
                is HttpResult.Error -> {
                    db.userDao().delete(dbContact)
                    updateState(currentState.copy(showUploadExceptionSnackBar = true))
                }

                is HttpResult.Success -> {
                    syncMessagesForContact(dbContact, db, sharedPreferences, downloader)
                }
            }
            updateState(currentState.copy(loadingContactAddress = null))
        }
    }

    fun updateContactSearchDialog(isShown: Boolean) {
        updateState(currentState.copy(newContactSearchDialogShown = isShown))
        if (!isShown) {
            updateState(currentState.copy(newContactAddressInput = ""))
            clearFoundContact()
        }
    }

    fun clearFoundContact() {
        updateState(currentState.copy(newContactFound = null))
    }

    fun onUndoDeletePressed() {
        currentState.contacts.add(currentState.itemToDeleteIndex!!, currentState.itemToDelete!!)
        updateState(
            currentState.copy(
                itemToDelete = null,
                itemToDeleteIndex = null,
                showUndoDeleteSnackBar = false
            )
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onDeleteWaitComplete(item: DBContact) {
        updateState(currentState.copy(showUndoDeleteSnackBar = false))
        GlobalScope.launch(Dispatchers.IO) {
            launch {
                db.userDao().delete(item)
            }
            launch {
                when (safeApiCall { deleteContact(item, sharedPreferences) }) {
                    is HttpResult.Error -> {
                        //ignore
                    }

                    is HttpResult.Success -> {
                        //ignore
                    }
                }
            }
        }
    }

    fun removeItem(index: Int) {
        currentState.itemToDelete?.run {
            onDeleteWaitComplete(this)
        }
        updateState(
            currentState.copy(
                itemToDelete = currentState.contacts[index],
                itemToDeleteIndex = index,
            )
        )
        currentState.contacts.removeAt(index)
        updateState(currentState.copy(showUndoDeleteSnackBar = false))
        updateState(currentState.copy(showUndoDeleteSnackBar = true))
    }

    fun onErrorDismissed() {
        updateState(currentState.copy(showUploadExceptionSnackBar = false))
    }

    override fun onCleared() {
        currentState.itemToDelete?.run {
            onDeleteWaitComplete(this)
        }
        super.onCleared()
    }
}


data class ContactsState(
    val contacts: SnapshotStateList<DBContact> = mutableStateListOf(),
    val itemToDeleteIndex: Int? = null,
    val itemToDelete: DBContact? = null,
    val searchInput: String = "",
    val newContactAddressInput: String = "",
    val loggedInPersonAddress: String = "",
    val loadingContactAddress: String? = null,
    val showUploadExceptionSnackBar: Boolean = false,
    val showUndoDeleteSnackBar: Boolean = false,
    val newContactFound: PublicUserData? = null,
    val searchButtonActive: Boolean = false,
    val loading: Boolean = false,
    val newContactSearchDialogShown: Boolean = false,
)