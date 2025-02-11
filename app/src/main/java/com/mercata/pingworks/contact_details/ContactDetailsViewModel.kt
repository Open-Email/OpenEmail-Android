package com.mercata.pingworks.contact_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBContact
import com.mercata.pingworks.repository.AddContactRepository
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ContactDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ContactDetailsState>(
        ContactDetailsState(
            address = savedStateHandle.get<String>("address")!!,
            isNotification = savedStateHandle.get<Boolean>("isNotification")!!,
        )
    ) {

    private val addContactRepository: AddContactRepository by inject()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val db: AppDatabase by inject()
            db.userDao().findByAddressFlow(currentState.address).collect { contact ->
                updateState(
                    currentState.copy(dbContact = contact)
                )
            }
        }
        viewModelScope.launch {
            addContactRepository.addingState.collect { isAdded ->
                if (isAdded) {
                    updateState(currentState.copy(snackBarResId = R.string.added_to_contacts))
                } else {
                    updateState(currentState.copy(snackBarResId = null))
                }
            }
        }
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            when (val call = safeApiCall { getProfilePublicData(currentState.address) }) {
                is HttpResult.Error -> {
                    //ignore
                }

                is HttpResult.Success -> {
                    updateState(
                        currentState.copy(contact = call.data)
                    )
                }
            }
            updateState(currentState.copy(loading = false))
        }
    }

    suspend fun approveRequest() {
        updateState(currentState.copy(loading = true))
        currentState.contact ?: return

        addContactRepository.addContact(currentState.contact!!)
        updateState(
            currentState.copy(
                loading = false,
                isNotification = false,
                dbContact = currentState.contact!!.toDBContact()
            )
        )
    }

    fun toggleBroadcast() {
        viewModelScope.launch(Dispatchers.IO) {
            val contact: DBContact? = db.userDao().findByAddress(currentState.address)
            contact?.let {
                db.userDao()
                    .update(contact.copy(receiveBroadcasts = !contact.receiveBroadcasts))
            }
        }
    }

    fun hideRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(requestApprovalDialogShown = false))
    }

    fun showRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(requestApprovalDialogShown = true))
    }
}

data class ContactDetailsState(
    val address: String,
    val isNotification: Boolean,
    val snackBarResId: Int? = null,
    val requestApprovalDialogShown: Boolean = false,
    val loading: Boolean = false,
    val contact: PublicUserData? = null,
    val dbContact: DBContact? = null
)