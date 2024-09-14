package com.mercata.pingworks.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
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
        updateState(currentState.copy(to = str, addressErrorResId = null))
    }

    fun updateSubject(str: String) {
        updateState(currentState.copy(subject = str, subjectErrorResId = null))
    }

    fun toggleBroadcast() {
        updateState(currentState.copy(broadcast = !currentState.broadcast, addressErrorResId = null))
    }

    fun updateBody(str: String) {
        updateState(currentState.copy(body = str, bodyErrorResId = null))
    }

    fun send() {
        var valid = true
        if (currentState.addressErrorResId != null) {
            valid = false
        }

        if (currentState.to.isBlank() && currentState.broadcast.not()) {
            updateState(currentState.copy(addressErrorResId = R.string.empty_email_error))
            valid = false
        }

        if (currentState.subject.isBlank()) {
            updateState(currentState.copy(subjectErrorResId = R.string.subject_error))
            valid = false
        }

        if (currentState.attachments.isEmpty() && currentState.body.isBlank()) {
            updateState(currentState.copy(bodyErrorResId = R.string.empty_email_body_error))
            valid = false
        }

        if (!valid) return

        
    }


    fun removeAttachment(attachment: Uri) {
        currentState.attachments.remove(attachment)
    }

    fun checkAddressExist() {
        if (currentState.addressErrorResId != null) {
            viewModelScope.launch {
                updateState(currentState.copy(loading = true))
                when (val call = safeApiCall { getProfilePublicData(currentState.to) }) {
                    is HttpResult.Error -> {
                        updateState(currentState.copy(addressErrorResId = R.string.invalid_email))
                    }

                    is HttpResult.Success -> {
                        updateState(currentState.copy(addressErrorResId = if (call.data == null) R.string.invalid_email else null))
                    }
                }
                updateState(currentState.copy(loading = false))
            }
        }
    }

    fun addAttachments(attachmentUris: List<Uri>) {
        updateState(currentState.copy(bodyErrorResId = null))
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
    val addressErrorResId: Int? = null,
    val subjectErrorResId: Int? = null,
    val bodyErrorResId: Int? = null,
    val broadcast: Boolean = false,
    val currentUser: UserData? = null,
    val attachments: SnapshotStateList<Uri> = mutableStateListOf()
)