package com.mercata.openemail.contact_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.R
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.contacts.DBContact
import com.mercata.openemail.models.Link
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.models.toDBContact
import com.mercata.openemail.repository.AddContactRepository
import com.mercata.openemail.repository.UserDataUpdateRepository
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.connectionLink
import com.mercata.openemail.utils.getAllLinks
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import com.mercata.openemail.utils.updateBroadcastsForLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ContactDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ContactDetailsState>(
        ContactDetailsState(
            address = savedStateHandle.get<String>("address")!!,
            type = ContactType.getTypeById(savedStateHandle.get<String>("type")!!)!!,
        )
    ) {

    private val addContactRepository: AddContactRepository by inject()

    init {
        if (currentState.type == ContactType.CurrentUser) {
            val userDataUpdateRepository: UserDataUpdateRepository by inject()
            viewModelScope.launch {
                userDataUpdateRepository.userData.collect { data ->
                    updateState(
                        currentState.copy(contact = data)
                    )
                }
            }
            userDataUpdateRepository.updateCurrentUserData()
        } else {
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
                updateAllowedBroadcasts()
                updateState(currentState.copy(loading = false))
            }
        }
    }

    suspend fun updateAllowedBroadcasts() {
        when (val call = safeApiCall { getAllLinks(sp) }) {
            is HttpResult.Error -> {
                //ignore
            }

            is HttpResult.Success -> {
                val link: Link? = call.data?.firstOrNull { it.address == currentState.address }
                db.userDao().findByAddress(currentState.address)
                    ?.copy(receiveBroadcasts = link?.allowedBroadcasts != false)
                    ?.let { currentContact ->
                        db.userDao().update(currentContact)
                    }
            }
        }
    }

    suspend fun approveRequest() {
        updateState(currentState.copy(loading = true))
        currentState.contact ?: return

        addContactRepository.addContact(currentState.contact!!)
        updateState(
            currentState.copy(
                loading = false,
                type = ContactType.SavedContact,
                dbContact = currentState.contact!!.toDBContact()
            )
        )
    }

    fun toggleBroadcast() {
        currentState.address.connectionLink()?.let { connectionLink ->
            viewModelScope.launch(Dispatchers.IO) {
                updateState(currentState.copy(loading = true))
                when (safeApiCall {
                    updateBroadcastsForLink(
                        sp = sp,
                        link = Link(
                            address = currentState.address,
                            link = connectionLink,
                            allowedBroadcasts = currentState.dbContact?.receiveBroadcasts != false
                        ),
                        currentState.dbContact?.receiveBroadcasts == false
                    )
                }) {
                    is HttpResult.Error -> {
                        //ignore
                    }
                    is HttpResult.Success -> {
                        updateAllowedBroadcasts()
                    }
                }
                updateState(currentState.copy(loading = false))
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
    val type: ContactType,
    val snackBarResId: Int? = null,
    val requestApprovalDialogShown: Boolean = false,
    val loading: Boolean = false,
    val contact: PublicUserData? = null,
    val dbContact: DBContact? = null
)

enum class ContactType(val id: String) {
    CurrentUser("current"),
    SavedContact("saved"),
    DetailsOnly("details"),
    ContactNotification("notification");

    companion object {
        fun getTypeById(typeId: String) = ContactType.entries.firstOrNull { it.id == typeId }
    }

}