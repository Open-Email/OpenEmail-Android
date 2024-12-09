package com.mercata.pingworks.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.contacts.toPublicUserData
import com.mercata.pingworks.db.drafts.DBDraft
import com.mercata.pingworks.db.drafts.draft_reader.toPublicUserData
import com.mercata.pingworks.db.messages.MessageWithAuthor
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBDraftReader
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.repository.SendMessageRepository
import com.mercata.pingworks.utils.Address
import com.mercata.pingworks.utils.CopyAttachmentService
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class ComposingViewModel(private val savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ComposingState>(
        ComposingState(intentAttachments = savedStateHandle.get<String>("attachmentUri"))
    ) {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val db: AppDatabase by inject()
            val sp: SharedPreferences by inject()
            initDraftId()

            launch {
                delay(100)
                db.draftDao().getById(draftId)?.let { draft ->
                    updateState(
                        currentState.copy(
                            subject = draft.getSubject(),
                            body = draft.getTextBody(),
                            broadcast = draft.draft.isBroadcast
                        )
                    )
                    currentState.recipients.clear()
                    currentState.recipients.addAll(draft.readers.map { it.toPublicUserData() })
                    currentState.attachments.clear()
                    currentState.attachments.addAll(draft.draft.attachmentUriList?.split(",")
                        ?.map { Uri.parse(it) } ?: listOf())
                }
            }
            launch {
                updateState(currentState.copy(addressLoading = true))
                val selectedContactAddresses: String =
                    savedStateHandle.get<String>("contactAddress") ?: ""
                selectedContactAddresses.split(",").filterNot { it.isBlank() }
                    .takeIf { it.isNotEmpty() }?.map { address ->
                        launch(Dispatchers.IO) {
                            addAddress(address)
                        }
                    }
                updateState(currentState.copy(addressLoading = false))
            }
            launch { listenToDraftReaders() }
            launch { listenToDraftChanges() }
            launch { consumeReplyMessage() }
            launch {
                currentState.contacts.clear()
                currentState.contacts.addAll(
                    db.userDao().getAll().filterNot { it.address == sp.getUserAddress() }.toList()
                )
            }
        }
    }

    private lateinit var draftId: String
    private val fileUtils: FileUtils by inject()
    private val attachmentCopier: CopyAttachmentService by inject()
    private val sendMessageRepository: SendMessageRepository by inject()
    private val draftDB = db.draftDao()
    private var instantPhotoUri: Uri? = null

    private suspend fun initDraftId() {
        val oldDraftId = savedStateHandle.get<String>("draftId")
        if (oldDraftId == null) {
            draftId = UUID.randomUUID().toString()
            db.draftDao().insert(
                DBDraft(
                    draftId = draftId,
                    attachmentUriList = null,
                    subject = "",
                    textBody = "",
                    isBroadcast = false,
                    timestamp = System.currentTimeMillis(),
                    readerAddresses = null
                )
            )
        } else {
            draftId = oldDraftId
        }
    }

    private suspend fun listenToDraftReaders() {
        db.draftReaderDao().getAllAsFlow(draftId = draftId).collect { draftReaders ->
            currentState.recipients.clear()
            currentState.recipients.addAll(draftReaders.map { it.toPublicUserData() })
        }
    }

    private suspend fun listenToDraftChanges() {
        db.draftDao().getByIdFlow(draftId = draftId).collect { draft ->
            currentState.attachments.clear()
            currentState.attachments.addAll(draft?.draft?.attachmentUriList?.split(",")
                ?.map { Uri.parse(it) } ?: listOf())
        }
    }


    private suspend fun consumeReplyMessage() {
        val sp: SharedPreferences by inject()
        val db: AppDatabase by inject()
        updateState(currentState.copy(loading = true))

        val replyMessage: MessageWithAuthor? =
            db.messagesDao().getById(savedStateHandle.get<String>("replyMessageId") ?: "")?.message

        updateState(
            currentState.copy(
                loading = false, replyMessage = replyMessage, currentUser = sp.getUserData()
            )
        )
    }

    fun updateTo(str: String) {
        updateState(currentState.copy(addressFieldText = str, addressErrorResId = null))
    }

    fun updateSubject(str: String) {
        viewModelScope.launch(Dispatchers.IO) {
            draftDB.update(draftDB.getById(draftId)!!.draft.copy(subject = str))
        }
        updateState(currentState.copy(subject = str, subjectErrorResId = null))
    }

    fun toggleBroadcast() {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = draftDB.getById(draftId)!!
            draftDB.update(draft.draft.copy(isBroadcast = !draft.draft.isBroadcast))
        }
        updateState(
            currentState.copy(
                broadcast = !currentState.broadcast, addressErrorResId = null
            )
        )
    }

    fun updateBody(str: String) {
        viewModelScope.launch(Dispatchers.IO) {
            draftDB.update(draftDB.getById(draftId)!!.draft.copy(textBody = str))
        }
        updateState(currentState.copy(body = str, bodyErrorResId = null))
    }

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
            if (getNonSyncedContacts().isEmpty()) {
                sendMessageRepository.send(
                    draftId, currentState.broadcast, savedStateHandle.get<String>("replyMessageId")
                )
                updateState(currentState.copy(sent = true))
            } else {
                syncContacts(sp, db.userDao())
                if (getNonSyncedContacts().isEmpty()) {
                    sendMessageRepository.send(
                        draftId,
                        currentState.broadcast,
                        savedStateHandle.get<String>("replyMessageId")
                    )
                    updateState(currentState.copy(sent = true))
                } else {
                    updateState(currentState.copy(snackbarErrorResId = R.string.couldnt_upload_contacts_error))
                }
            }

            updateState(currentState.copy(loading = false))
        }
    }

    private suspend fun getNonSyncedContacts() = db.userDao().getAll()
        .filter { dbContact -> !dbContact.uploaded && currentState.recipients.any { it.address == dbContact.address } }


    fun removeAttachment(attachment: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = draftDB.getById(draftId)!!
            val uris: ArrayList<String> = arrayListOf<String>().apply {
                addAll(
                    draft.draft.attachmentUriList?.split(",") ?: listOf()
                )
            }
            uris.remove(attachment.toString())
            draftDB.update(
                draft.draft.copy(attachmentUriList = uris.joinToString(",")
                    .takeIf { it.isNotEmpty() })
            )
        }

        currentState.attachments.remove(attachment)
    }

    fun attemptToAddAddress() {
        val text = currentState.addressFieldText.trim()
        if (text.isBlank()) return
        if (!currentState.recipients.any { it.address == text }) {
            viewModelScope.launch(Dispatchers.IO) {
                updateState(currentState.copy(addressLoading = true))
                addAddress(text)
                updateState(currentState.copy(addressLoading = false))
            }
        }
    }

    private suspend fun addAddress(address: String) {
        when (val call = safeApiCall { getProfilePublicData(address) }) {
            is HttpResult.Error -> {
                updateState(currentState.copy(addressErrorResId = R.string.invalid_email))
            }

            is HttpResult.Success -> {
                if (call.data == null) {
                    updateState(currentState.copy(addressErrorResId = R.string.invalid_email))
                } else {
                    updateState(
                        currentState.copy(
                            addressErrorResId = null, addressFieldText = ""
                        )
                    )
                    db.draftReaderDao().insert(call.data.toDBDraftReader(draftId))
                }
            }
        }
    }

    fun addAttachments(attachmentUris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))

            val attachmentUriStrings: List<String> = attachmentUris.map { uri ->
                async {
                    attachmentCopier.copyUriToLocalStorage(uri, fileUtils.getURLInfo(uri).name)
                }
            }.awaitAll().map { it.toString() }
            val draft = draftDB.getById(draftId)!!
            val uris = draft.draft.attachmentUriList?.split(",") ?: listOf()

            draftDB.update(draft.draft.copy(attachmentUriList = hashSetOf<String>().apply {
                addAll(uris)
                addAll(attachmentUriStrings)
            }.joinToString(",").takeIf { it.isNotEmpty() }))

            updateState(currentState.copy(loading = false))
        }

        updateState(currentState.copy(bodyErrorResId = null))
    }

    fun removeRecipient(user: PublicUserData) {
        viewModelScope.launch(Dispatchers.IO) {
            db.draftReaderDao().delete(user.address)
        }
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

    fun confirmExit() {
        updateState(currentState.copy(confirmExitDialogShown = true))
    }

    fun closeExitConfirmation() {
        updateState(currentState.copy(confirmExitDialogShown = false))
    }

    suspend fun deleteDraft() {
        withContext(Dispatchers.IO) {
            db.draftDao().delete(draftId)
            closeExitConfirmation()
        }
    }

    fun getNewFileUri(): Uri {
        instantPhotoUri = fileUtils.getUriForFile(fileUtils.createImageFile())
        return instantPhotoUri!!
    }

    fun addInstantPhotoAsAttachment(success: Boolean) {
        if (success) {
            instantPhotoUri?.let {
                addAttachments(listOf(it))
            }
        } else {
            instantPhotoUri = null
        }
    }

    fun toggleMode(addressFieldFocused: Boolean) {
        updateState(currentState.copy(mode = if (addressFieldFocused) ComposingScreenMode.ContactSuggestion else ComposingScreenMode.Default))
    }

    fun addContactSuggestion(person: DBContact) {
        val publicData = person.toPublicUserData()
        if (!currentState.recipients.contains(publicData)) {
            viewModelScope.launch(Dispatchers.IO) {
                db.draftReaderDao().insert(person.toPublicUserData().toDBDraftReader(draftId))
            }
        }
    }

    fun clearAddressField() {
        updateState(currentState.copy(addressFieldText = ""))
    }

    fun consumeIntentAttachments() {
        val intentUris = URLDecoder.decode(
            currentState.intentAttachments ?: "",
            StandardCharsets.UTF_8.toString()
        ).split(",").map { Uri.parse(it) }
        addAttachments(intentUris)
        updateState(
            currentState.copy(intentAttachments = null)
        )
    }
}

data class ComposingState(
    val sent: Boolean = false,
    val mode: ComposingScreenMode = ComposingScreenMode.Default,
    val subject: String = "",
    val openedAddressDetails: PublicUserData? = null,
    val replyMessage: MessageWithAuthor? = null,
    val body: String = "",
    val intentAttachments: String? = null,
    val addressFieldText: Address = "",
    val recipients: SnapshotStateList<PublicUserData> = mutableStateListOf(),
    val contacts: SnapshotStateList<DBContact> = mutableStateListOf(),
    val addressLoading: Boolean = false,
    val loading: Boolean = false,
    val confirmExitDialogShown: Boolean = false,
    val addressErrorResId: Int? = null,
    val subjectErrorResId: Int? = null,
    val snackbarErrorResId: Int? = null,
    val bodyErrorResId: Int? = null,
    val broadcast: Boolean = false,
    val currentUser: UserData? = null,
    val attachments: SnapshotStateList<Uri> = mutableStateListOf()
)

enum class ComposingScreenMode {
    Default, ContactSuggestion
}