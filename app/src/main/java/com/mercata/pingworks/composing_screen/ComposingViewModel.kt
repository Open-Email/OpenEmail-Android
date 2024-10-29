package com.mercata.pingworks.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.messages.MessageWithAuthor
import com.mercata.pingworks.models.ComposingData
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.Address
import com.mercata.pingworks.utils.CopyAttachmentService
import com.mercata.pingworks.utils.Downloader
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.uploadMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.File

class ComposingViewModel(private val savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ComposingState>(
        ComposingState(
            addressFieldText = savedStateHandle.get<String>("contactAddress") ?: "",
        )
    ) {

    init {
        viewModelScope.launch {
            val sp: SharedPreferences by inject()
            val db: AppDatabase by inject()
            updateState(currentState.copy(loading = true))

            val replyMessage: MessageWithAuthor? = db.messagesDao()
                .getById(savedStateHandle.get<String>("replyMessageId") ?: "")?.message

            updateState(
                currentState.copy(
                    loading = false,
                    replyMessage = replyMessage,
                    currentUser = sp.getUserData()
                )
            )
        }
    }

    private val fileUtils: FileUtils by inject()
    private val dl: Downloader by inject()
    private val attachmentCopier: CopyAttachmentService by inject()

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

    @OptIn(DelicateCoroutinesApi::class)
    fun send() {
        var valid = true
        if (currentState.addressErrorResId != null) {
            valid = false
        }

        if (currentState.recipients.isEmpty() && currentState.broadcast.not()) {
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

        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))

            val attachments: List<File> = if (currentState.attachments.isNotEmpty()) {
                currentState.attachments.map { uri ->
                    async {
                        attachmentCopier.copyUriToLocalStorage(uri, fileUtils.getURLInfo(uri).name)
                    }
                }.awaitAll().filterNotNull()
            } else {
                listOf()
            }

            updateState(currentState.copy(loading = false))

            GlobalScope.launch {
                uploadMessage(
                    composingData = ComposingData(
                        recipients = currentState.recipients,
                        subject = currentState.subject,
                        body = currentState.body,
                        attachments = attachments
                    ),
                    fileUtils = fileUtils,
                    currentUser = sp.getUserData()!!,
                    currentUserPublicData = sp.getPublicUserData()!!,
                    db = db,
                    isBroadcast = currentState.broadcast,
                    replyToSubjectId = savedStateHandle.get<String>("replyMessageId"),
                    sp = sp
                )
                syncAllMessages(db, sp, dl)
            }
            updateState(currentState.copy(sent = true))
        }
    }


    fun removeAttachment(attachment: Uri) {
        currentState.attachments.remove(attachment)
    }

    fun attemptToAddAddress() {
        val text = currentState.addressFieldText.trim()
        if (text.isBlank()) return
        if (!currentState.recipients.any { it.address == text }) {
            viewModelScope.launch {
                updateState(currentState.copy(addressLoading = true))
                when (val call =
                    safeApiCall { getProfilePublicData(text) }) {
                    is HttpResult.Error -> {
                        updateState(currentState.copy(addressErrorResId = R.string.invalid_email))
                    }

                    is HttpResult.Success -> {
                        if (call.data == null) {
                            updateState(currentState.copy(addressErrorResId = R.string.invalid_email))
                        } else {
                            updateState(
                                currentState.copy(
                                    addressErrorResId = null,
                                    addressFieldText = ""
                                )
                            )
                            currentState.recipients.add(call.data)
                        }
                    }
                }
                updateState(currentState.copy(addressLoading = false))
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

    fun removeRecipient(user: PublicUserData) {
        currentState.recipients.remove(user)
    }

    fun openUserDetails(user: PublicUserData) {
        updateState(currentState.copy(openedAddressDetails = user))
    }

    fun closeUserDetails() {
        updateState(currentState.copy(openedAddressDetails = null))
    }

    fun consumeReplyData() {
        updateState(currentState.copy(replyMessage = null))
    }
}

data class ComposingState(
    val sent: Boolean = false,
    val subject: String = "",
    val openedAddressDetails: PublicUserData? = null,
    val replyMessage: MessageWithAuthor? = null,
    val body: String = "",
    val addressFieldText: Address = "",
    val recipients: SnapshotStateList<PublicUserData> = mutableStateListOf(),
    val addressLoading: Boolean = false,
    val loading: Boolean = false,
    val addressErrorResId: Int? = null,
    val subjectErrorResId: Int? = null,
    val bodyErrorResId: Int? = null,
    val broadcast: Boolean = false,
    val currentUser: UserData? = null,
    val attachments: SnapshotStateList<Uri> = mutableStateListOf()
)