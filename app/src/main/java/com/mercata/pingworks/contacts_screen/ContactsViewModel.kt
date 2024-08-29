package com.mercata.pingworks.contacts_screen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.HttpResult
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.contacts.toPerson
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.getProfilePublicData
import com.mercata.pingworks.models.Person
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.safeApiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.util.UUID

class ContactsViewModel : AbstractViewModel<ContactsState>(ContactsState()) {

    init {
        val db: AppDatabase by inject()
        viewModelScope.launch {
            val contacts: List<Person> = db.userDao().getAll().map { it.toPerson() }
            currentState.contacts.addAll(contacts)
        }

    }

    var searchJob: Job? = null

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
            val address = currentState.newContactAddressInput!!.trim()
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
        if (currentState.newContactAddressInput.isNullOrBlank()) {
            return false
        }

        return currentState.newContactAddressInput!!.lowercase().matches(emailRegex)
    }

    fun addContact() {
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val publicData = currentState.newContactFound!!
            db.userDao().insertAll(
                DBContact(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    address = publicData.address,
                    name = publicData.fullName.takeUnless { it.isBlank() },
                    //TODO update when public profile will contain image
                    imageUrl = null,
                    receiveBroadcasts = true,
                    signingKeyAlgorithm = publicData.signingKeyAlgorithm,
                    encryptionKeyAlgorithm = publicData.encryptionKeyAlgorithm,
                    publicSigningKey = publicData.publicSigningKey,
                    publicEncryptionKey = publicData.publicEncryptionKey
                )
            )
            updateState(currentState.copy(loading = false))
        }

    }

    fun updateContactSearchDialog(isShown: Boolean) {
        updateState(currentState.copy(newContactSearchDialogShown = isShown))
        if (!isShown) {
            clearFoundContact()
            searchJob?.cancel()
        }
    }

    fun clearFoundContact() {
        updateState(currentState.copy(newContactFound = null))
    }
}

data class ContactsState(
    val contacts: SnapshotStateList<Person> = mutableStateListOf(),
    val searchInput: String = "",
    val newContactAddressInput: String? = null,
    val newContactFound: PublicUserData? = null,
    val searchButtonActive: Boolean = false,
    val loading: Boolean = false,
    val newContactSearchDialogShown: Boolean = false,
)