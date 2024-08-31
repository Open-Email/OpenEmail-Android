package com.mercata.pingworks.contacts_screen

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.HttpResult
import com.mercata.pingworks.R
import com.mercata.pingworks.SharedPreferences
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.contacts.toPerson
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.getProfilePublicData
import com.mercata.pingworks.models.Person
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.safeApiCall
import com.mercata.pingworks.uploadContact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ContactsViewModel : AbstractViewModel<ContactsState>(ContactsState()) {

    init {
        val db: AppDatabase by inject()
        viewModelScope.launch {
            db.userDao().getAll().collect { dbEntities ->
                currentState.contacts.clear()
                currentState.contacts.addAll(dbEntities.map { it.toPerson() })
            }
        }
        val sp: SharedPreferences by inject()
        updateState(currentState.copy(loggedInPersonAddress = sp.getUserAddress()!!))

    }

    val snackBarDuration = SnackbarDuration.Short

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
            db.userDao().insertAll(dbContact)
            updateContactSearchDialog(false)
            when (safeApiCall {
                uploadContact(
                    contact = publicData,
                    sharedPreferences = sharedPreferences
                )
            }) {
                is HttpResult.Error -> {
                    db.userDao().delete(dbContact)
                    updateState(currentState.copy(snackBarTextResId = R.string.uploading_contact_failed))
                    delay(4000L)
                    updateState(currentState.copy(snackBarTextResId = null))
                }

                is HttpResult.Success -> {
                    println()
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
}

data class ContactsState(
    val contacts: SnapshotStateList<Person> = mutableStateListOf(),
    val searchInput: String = "",
    val newContactAddressInput: String = "",
    val loggedInPersonAddress: String = "",
    val loadingContactAddress: String? = null,
    val snackBarTextResId: Int? = null,
    val newContactFound: PublicUserData? = null,
    val searchButtonActive: Boolean = false,
    val loading: Boolean = false,
    val newContactSearchDialogShown: Boolean = false,
)