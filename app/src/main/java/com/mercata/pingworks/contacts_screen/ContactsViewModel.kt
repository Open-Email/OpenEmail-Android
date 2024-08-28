package com.mercata.pingworks.contacts_screen

import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.models.Person

class ContactsViewModel: AbstractViewModel<ContactsState>(ContactsState()) {

    init {
        //TODO getContacts request
    }

    fun addContact() {

    }
}

data class ContactsState(val contacts: List<Person> = listOf(), val searchInput: String = "")