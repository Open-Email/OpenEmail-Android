package com.mercata.pingworks.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.Address
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ComposingViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ComposingState>(
        ComposingState(
            to = savedStateHandle.get<String>("contactAddress") ?: ""
        )
    ) {

    init {
        val sp: SharedPreferences by inject()
        updateState(currentState.copy(currentUser = sp.getUserData()))
    }

    fun updateTo(str: String) {
        updateState(currentState.copy(to = str, addressError = null))
    }

    fun updateSubject(str: String) {
        updateState(currentState.copy(subject = str))
    }

    fun toggleBroadcast() {
        updateState(currentState.copy(broadcast = !currentState.broadcast))
    }

    fun updateBody(str: String) {
        updateState(currentState.copy(body = str))
    }

    fun send() {
        //TODO
    }


    fun removeAttachment(attachment: Uri) {
        currentState.attachments.remove(attachment)
    }

    fun checkAddressExist() {
        if (currentState.addressError != false) {
            viewModelScope.launch {
                updateState(currentState.copy(loading = true))
                when (val call = safeApiCall { getProfilePublicData(currentState.to) }) {
                    is HttpResult.Error -> {
                        updateState(currentState.copy(addressError = true))
                    }

                    is HttpResult.Success -> {
                        updateState(currentState.copy(addressError = call.data == null))
                    }
                }
                updateState(currentState.copy(loading = false))
            }
        }
    }

    fun addAttachments(attachmentUris: List<Uri>) {
        attachmentUris.forEach {
            if (!currentState.attachments.contains(it)) {
                currentState.attachments.add(it)
            }
        }
    }
}

data class ComposingState(
    val subject: String = "",
    val body: String = "",
    val to: Address = "",
    val loading: Boolean = false,
    val addressError: Boolean? = null,
    val broadcast: Boolean = false,
    val currentUser: UserData? = null,
    val attachments: SnapshotStateList<Uri> = mutableStateListOf()
)