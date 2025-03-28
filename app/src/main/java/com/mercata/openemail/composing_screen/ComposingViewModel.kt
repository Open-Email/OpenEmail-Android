package com.mercata.openemail.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.R
import com.mercata.openemail.db.contacts.toPublicUserData
import com.mercata.openemail.db.drafts.DBDraft
import com.mercata.openemail.db.drafts.draft_reader.toPublicUserData
import com.mercata.openemail.db.messages.DBMessage
import com.mercata.openemail.db.notifications.toPublicUserData
import com.mercata.openemail.emailRegex
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.models.toDBDraftReader
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.repository.AddContactRepository
import com.mercata.openemail.repository.SendMessageRepository
import com.mercata.openemail.utils.Address
import com.mercata.openemail.utils.CopyAttachmentService
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import com.mercata.openemail.utils.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
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
            initDraftId()
            consumeIntentAttachments()
            launch {
                db.draftDao().getById(draftId)?.let { draft ->
                    updateState(
                        currentState.copy(
                            subject = draft.getSubtitle(),
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
                updateAvailableContacts()
            }
            launch {
                updateState(currentState.copy(addressLoading = true))
                val selectedContactAddresses: String =
                    savedStateHandle.get<String>("contactAddress") ?: ""
                selectedContactAddresses.split(",")
                    .filterNot { it.isBlank() || it == sp.getUserAddress() }
                    .takeIf { it.isNotEmpty() }?.map { address ->
                        launch(Dispatchers.IO) {
                            addAddress(address)
                        }
                    }?.joinAll()
                updateState(currentState.copy(addressLoading = false))
            }
            launch { listenToDraftReaders() }
            launch { listenToDraftChanges() }
            launch { consumeReplyMessage() }
        }
    }

    private lateinit var draftId: String
    private val fileUtils: FileUtils by inject()
    private val attachmentCopier: CopyAttachmentService by inject()
    private val sendMessageRepository: SendMessageRepository by inject()
    private val addContactRepository: AddContactRepository by inject()
    private var instantPhotoUri: Uri? = null

    private suspend fun updateAvailableContacts() {
        withContext(Dispatchers.IO) {
            val allContacts: List<PublicUserData> =
                (db.userDao().getAll().map { it.toPublicUserData() } + db.notificationsDao()
                    .getAll()
                    .map { it.toPublicUserData() }).distinctBy { it.address }.toList()
                    .sortedBy { it.fullName.takeIf { name -> name.isNotBlank() } ?: it.address }

            val currentUserAddress: String = sp.getUserAddress() ?: ""
            currentState.contacts.clear()
            currentState.contacts.addAll(
                allContacts
                    .filterNot { contact ->
                        contact.address == currentUserAddress ||
                                currentState.recipients.toList()
                                    .any { contact.address == it.address }
                    }
                    .toList()
            )
        }
    }

    private suspend fun initDraftId() {
        if (::draftId.isInitialized) return
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
            updateAvailableContacts()
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

        val replyMessage: DBMessage? =
            db.messagesDao().getById(savedStateHandle.get<String>("replyMessageId") ?: "")?.message

        updateState(
            currentState.copy(replyMessage = replyMessage, currentUser = sp.getUserData())
        )
    }

    fun updateTo(str: String) {
        updateState(currentState.copy(addressFieldText = str, addressErrorResId = null))
        if (str.lowercase().matches(emailRegex) && !currentState.currentUser?.address?.lowercase()
                .equals(str)
        ) {
            viewModelScope.launch {
                updateState(currentState.copy(loading = true))
                when (val call = safeApiCall { getProfilePublicData(str) }) {
                    is HttpResult.Error -> {
                        //ignore
                    }

                    is HttpResult.Success -> {
                        updateState(currentState.copy(externalContact = call.data))
                    }
                }
                updateState(currentState.copy(loading = false))
            }
        }
    }

    fun updateSubject(str: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.draftDao().update(db.draftDao().getById(draftId)!!.draft.copy(subject = str))
        }
        updateState(currentState.copy(subject = str, subjectErrorResId = null))
    }

    fun toggleBroadcast() {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = db.draftDao().getById(draftId)!!
            db.draftDao().update(draft.draft.copy(isBroadcast = !draft.draft.isBroadcast))
        }
        updateState(
            currentState.copy(
                broadcast = !currentState.broadcast, addressErrorResId = null
            )
        )
    }

    fun updateBody(str: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.draftDao().update(db.draftDao().getById(draftId)!!.draft.copy(textBody = str))
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
            val draft = db.draftDao().getById(draftId)!!
            val uris: ArrayList<String> = arrayListOf<String>().apply {
                addAll(
                    draft.draft.attachmentUriList?.split(",") ?: listOf()
                )
            }
            uris.remove(attachment.toString())
            db.draftDao().update(
                draft.draft.copy(attachmentUriList = uris.joinToString(",")
                    .takeIf { it.isNotEmpty() })
            )
        }

        currentState.attachments.remove(attachment)
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
            initDraftId()
            updateState(currentState.copy(loading = true))

            val attachmentUriStrings: List<String> = attachmentUris.map { uri ->
                async {
                    attachmentCopier.copyUriToLocalStorage(uri, fileUtils.getURLInfo(uri).name)
                }
            }.awaitAll().map { it.toString() }
            val draft = db.draftDao().getById(draftId)!!
            val uris = draft.draft.attachmentUriList?.split(",") ?: listOf()

            db.draftDao().update(draft.draft.copy(attachmentUriList = hashSetOf<String>().apply {
                addAll(uris)
                addAll(attachmentUriStrings)
            }.joinToString(",").takeIf { it.isNotEmpty() }))

            updateState(currentState.copy(loading = false))
        }
    }

    fun removeRecipient(user: PublicUserData) {
        viewModelScope.launch(Dispatchers.IO) {
            db.draftReaderDao().delete(user.address)
        }
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

    fun addContactSuggestion(person: PublicUserData) {
        if (!currentState.recipients.contains(person)) {
            viewModelScope.launch(Dispatchers.IO) {
                updateState(currentState.copy(loading = true))
                val allContacts = db.userDao().getAll()
                if (!allContacts.any { it.address == person.address }) {
                    if (currentState.externalContact?.address == person.address) {
                        updateState(currentState.copy(externalContact = null))
                    }

                    addContactRepository.addContact(person)

                }
                db.draftReaderDao().insert(person.toDBDraftReader(draftId))
                updateState(currentState.copy(loading = false))
            }
        }
    }


    fun clearAddressField() {
        updateState(currentState.copy(addressFieldText = ""))
    }

    fun toggleAttachmentBottomSheet(shown: Boolean) {
        updateState(currentState.copy(attachmentBottomSheetShown = shown))
    }

    private fun consumeIntentAttachments() {
        val intentUris = URLDecoder.decode(
            currentState.intentAttachments ?: "",
            StandardCharsets.UTF_8.toString()
        ).split(",").filter { it.isNotBlank() }.map { Uri.parse(it) }
        if (intentUris.isNotEmpty()) {
            addAttachments(intentUris)
        }
        updateState(
            currentState.copy(intentAttachments = null)
        )
    }
}

data class ComposingState(
    val sent: Boolean = false,
    val mode: ComposingScreenMode = ComposingScreenMode.Default,
    val subject: String = "",
    val replyMessage: DBMessage? = null,
    val body: String = "",
    val intentAttachments: String? = null,
    val addressFieldText: Address = "",
    val recipients: SnapshotStateList<PublicUserData> = mutableStateListOf(),
    val contacts: SnapshotStateList<PublicUserData> = mutableStateListOf(),
    val externalContact: PublicUserData? = null,
    val attachmentBottomSheetShown: Boolean = false,
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