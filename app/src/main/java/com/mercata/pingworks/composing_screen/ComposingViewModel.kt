package com.mercata.pingworks.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.models.ComposingData
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.Address
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.uploadPrivateMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ComposingViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ComposingState>(
        ComposingState(
            addressFieldText = savedStateHandle.get<String>("contactAddress") ?: ""
        )
    ) {

    init {
        val sp: SharedPreferences by inject()
        updateState(currentState.copy(currentUser = sp.getUserData()))
    }

    private val fileUtils: FileUtils by inject()

    fun updateTo(str: String) {
        updateState(currentState.copy(addressFieldText = str, addressErrorResId = null))
    }

    fun updateSubject(str: String) {
        updateState(currentState.copy(subject = str, subjectErrorResId = null))
    }

    fun toggleBroadcast() {
        updateState(
            currentState.copy(
                broadcast = !currentState.broadcast,
                addressErrorResId = null
            )
        )
    }

    fun updateBody(str: String) {
        updateState(currentState.copy(body = str, bodyErrorResId = null))
    }

    fun send() {
        var valid = true
        if (currentState.addressErrorResId != null) {
            valid = false
        }

        if (currentState.addressFieldText.isBlank() && currentState.broadcast.not()) {
            updateState(currentState.copy(addressErrorResId = R.string.empty_email_error))
            valid = false
        }

        if (currentState.subject.isBlank()) {
            updateState(currentState.copy(subjectErrorResId = R.string.subject_error))
            valid = false
        }

        if (currentState.body.isBlank()) {
            updateState(currentState.copy(bodyErrorResId = R.string.empty_email_body_error))
            valid = false
        }

        if (!valid) return

        GlobalScope.launch {
            //TODO multiple recipients
            //TODO save messages instantly
            updateState(currentState.copy(loading = true))
            uploadPrivateMessage(
                composingData = ComposingData(
                    recipients = currentState.recipients,
                    subject = currentState.subject,
                    body = currentState.body,
                    attachments = currentState.attachments
                ),
                fileUtils = fileUtils,
                currentUser = sp.getUserData()!!,
                currentUserPublicData = sp.getPublicUserData()!!
            )
            updateState(currentState.copy(loading = false))
        }
    }


    fun removeAttachment(attachment: Uri) {
        currentState.attachments.remove(attachment)
    }

    fun checkAddressExist() {
        if (!currentState.recipients.any { it.address == currentState.addressFieldText }) {
            viewModelScope.launch {
                updateState(currentState.copy(loading = true))
                when (val call =
                    safeApiCall { getProfilePublicData(currentState.addressFieldText) }) {
                    is HttpResult.Error -> {
                        updateState(currentState.copy(addressErrorResId = R.string.invalid_email))
                    }

                    is HttpResult.Success -> {
                        if (call.data == null) {
                            updateState(currentState.copy(addressErrorResId = R.string.invalid_email))
                        } else {
                            updateState(currentState.copy(addressErrorResId = null))
                            currentState.recipients.add(call.data)
                        }

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
    val addressFieldText: Address = "",
    val recipients: SnapshotStateList<PublicUserData> = mutableStateListOf(),
    val loading: Boolean = false,
    val addressErrorResId: Int? = null,
    val subjectErrorResId: Int? = null,
    val bodyErrorResId: Int? = null,
    val broadcast: Boolean = false,
    val currentUser: UserData? = null,
    val attachments: SnapshotStateList<Uri> = mutableStateListOf()
)