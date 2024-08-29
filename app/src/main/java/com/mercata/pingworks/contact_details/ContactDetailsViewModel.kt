package com.mercata.pingworks.contact_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.toPerson
import com.mercata.pingworks.models.Person
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
            updateState(
                currentState.copy(
                    contact = db.userDao().loadAllByIds(listOf(currentState.address)).first()
                        .toPerson()
                )
            )
        }
    }

}

data class ContactDetailsState(val address: String, val contact: Person?)