package com.mercata.pingworks.contact_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ContactDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ContactDetailsState>(
        ContactDetailsState(
            address = savedStateHandle.get<String>("address")!!,
            contact = null
        )
    ) {

    init {
        val db: AppDatabase by inject()
        viewModelScope.launch {
            db.userDao().findByAddressFlow(currentState.address).collect { contact ->
                updateState(
                    currentState.copy(contact = contact)
                )
            }
        }
    }

    fun toggleBroadcast() {
        viewModelScope.launch {
            db.userDao()
                .update(currentState.contact!!.copy(receiveBroadcasts = !currentState.contact!!.receiveBroadcasts))
        }
    }

}

data class ContactDetailsState(val address: String, val contact: DBContact?)