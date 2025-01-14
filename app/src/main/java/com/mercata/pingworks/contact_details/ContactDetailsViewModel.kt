package com.mercata.pingworks.contact_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBContact
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ContactDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ContactDetailsState>(
        ContactDetailsState(
            address = savedStateHandle.get<String>("address")!!,
            isNotification = savedStateHandle.get<Boolean>("isNotification")!!,
        )
    ) {

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
        val dbContact = currentState.contact?.toDBContact()?.copy(uploaded = false) ?: return
        //TODO uncomment
        //db.userDao().insert(dbContact)
        syncContacts(sp, db.userDao())
        updateState(currentState.copy(loading = false, isNotification = false, dbContact = dbContact))
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
}

data class ContactDetailsState(
    val address: String,
    val isNotification: Boolean,
    val loading: Boolean = false,
    val contact: PublicUserData? = null,
    val dbContact: DBContact? = null
)