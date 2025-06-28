package com.mercata.openemail.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.R
import com.mercata.openemail.db.contacts.toPublicUserData
import com.mercata.openemail.db.drafts.DBDraft
import com.mercata.openemail.db.drafts.DBDraftWithReaders
import com.mercata.openemail.db.drafts.draft_reader.DBDraftReader
import com.mercata.openemail.db.notifications.toPublicUserData
import com.mercata.openemail.emailRegex
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.models.toDBDraftReader
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.repository.AddContactRepository
import com.mercata.openemail.repository.SyncRepository
import com.mercata.openemail.utils.Address
import com.mercata.openemail.utils.CopyAttachmentService
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.ReplyBodyConstructor
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import com.mercata.openemail.utils.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class ComposingViewModel(private val savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ComposingState>(
        ComposingState(
            intentAttachments = savedStateHandle.get<String>("attachmentUri")
        )
    ) {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            initDraft()
            consumeIntentAttachments()
            launch {
                updateAvailableContacts()
            }
            launch { consumeReplyMessage() }
        }
    }

    private val fileUtils: FileUtils by inject()
    private val replyBodyConstructor: ReplyBodyConstructor by inject()
    private val attachmentCopier: CopyAttachmentService by inject()
    private val syncRepository: SyncRepository by inject()
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
                                currentState.draft!!.readers
                                    .any { contact.address == it.address }
                    }
                    .toList()
            )
        }
    }

    private suspend fun initDraft() {
        val oldDraftId = savedStateHandle.get<String>("draftId")
        if (oldDraftId == null) {
            updateState(
                currentState.copy(
                    draft = DBDraftWithReaders(
                        DBDraft(
                            draftId = UUID.randomUUID().toString(),
                            attachmentUriList = null,
                            subject = "",
                            textBody = "",
                            isBroadcast = false,
                            timestamp = System.currentTimeMillis(),
                            readerAddresses = null
                        ), listOf()
                    )
                )
            )
        } else {
            db.draftDao().getById(oldDraftId)?.let { oldDraft ->
                updateState(currentState.copy(draft = oldDraft))
            }
        }
    }

    private suspend fun consumeReplyMessage() {

        db.messagesDao().getById(
            savedStateHandle.get<String>("replyMessageId") ?: ""
        )?.message?.let { replyMessage ->
            updateEditedDraft(
                currentState.draft!!.draft.copy(
                    subject = replyMessage.subject,
                    textBody = replyBodyConstructor.getReplyBody(replyMessage),
                    isBroadcast = replyMessage.isBroadcast,
                    readerAddresses = replyMessage.readerAddresses,
                )
            )
            savedStateHandle.remove<String>("replyMessageId")
        }
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
        updateEditedDraft(currentState.draft!!.draft.copy(subject = str))
    }

    private fun updateEditedDraft(newDraftState: DBDraft) {
        val updatedDraftWithReaders = currentState.draft!!.copy(draft = newDraftState)
        updateState(currentState.copy(draft = updatedDraftWithReaders, subjectErrorResId = null))
    }

    fun toggleBroadcast() {
        updateEditedDraft(currentState.draft!!.draft.copy(isBroadcast = !currentState.draft!!.draft.isBroadcast))
    }

    fun updateBody(str: String) {
        updateEditedDraft(currentState.draft!!.draft.copy(textBody = str))
    }

    fun send() {
        var valid = true
        if (currentState.addressErrorResId != null) {
            valid = false
        }

        if (currentState.draft!!.readers.isEmpty() && !currentState.draft!!.draft.isBroadcast) {
            updateState(currentState.copy(addressErrorResId = R.string.empty_email_error))
            valid = false
        }

        if (currentState.draft!!.draft.subject.isBlank()) {
            updateState(currentState.copy(subjectErrorResId = R.string.subject_error))
            valid = false
        }

        if (currentState.draft!!.draft.textBody.isBlank()) {
            updateState(currentState.copy(bodyErrorResId = R.string.empty_email_body_error))
            valid = false
        }

        if (!valid) return

        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))
            if (getNonSyncedContacts().isEmpty()) {
                syncRepository.send(
                    currentState.draft!!.draft.draftId,
                    currentState.draft!!.draft.isBroadcast,
                    savedStateHandle.get<String>("replyMessageId")
                )
                updateState(currentState.copy(sent = true))
            } else {
                syncContacts(sp, db.userDao())
                if (getNonSyncedContacts().isEmpty()) {
                    syncRepository.send(
                        currentState.draft!!.draft.draftId,
                        currentState.draft!!.draft.isBroadcast,
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

    private suspend fun getNonSyncedContacts(): List<String> {
        val syncedContacts = db.userDao().getAll().map { it.address }
        return currentState.draft!!.draft.readerAddresses?.split(",")?.filterNot {
            syncedContacts.contains(it)
        } ?: listOf()
    }

    fun removeAttachment(attachment: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val uris: ArrayList<String> = arrayListOf<String>().apply {
                addAll(
                    currentState.draft!!.draft.attachmentUriList?.split(",") ?: listOf()
                )
            }
            uris.remove(attachment.toString())
            updateEditedDraft(
                currentState.draft!!.draft.copy(
                    attachmentUriList = uris.joinToString(",")
                        .takeIf { it.isNotEmpty() })
            )
        }
    }

    fun addAddress() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(addressLoading = true))
            when (val call =
                safeApiCall { getProfilePublicData(currentState.addressFieldText.trim()) }) {
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
                        val updatedDraftReaders = ArrayList(currentState.draft!!.readers).apply {
                            add(call.data.toDBDraftReader(currentState.draft!!.draft.draftId))
                        }
                        val updatedDraft = currentState.draft!!.copy(readers = updatedDraftReaders)
                        updateState(currentState.copy(draft = updatedDraft))
                        updateEditedDraft(
                            currentState.draft!!.draft.copy(
                                readerAddresses = updatedDraftReaders.joinToString(
                                    ","
                                ) { it.address })
                        )
                    }
                }
            }
            updateState(currentState.copy(addressLoading = false))
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
            val uris = currentState.draft!!.draft.attachmentUriList?.split(",") ?: listOf()

            updateEditedDraft(currentState.draft!!.draft.copy(attachmentUriList = hashSetOf<String>().apply {
                addAll(uris)
                addAll(attachmentUriStrings)
            }.joinToString(",").takeIf { it.isNotEmpty() }))

            updateState(currentState.copy(loading = false))
        }
    }

    fun removeRecipient(address: Address) {
        val updatedReadersList = ArrayList(currentState.draft!!.readers).apply { removeIf { it.address == address } }
        val updatedDraft = currentState.draft!!.copy(readers = updatedReadersList)
        updateState(currentState.copy(draft = updatedDraft))
        updateEditedDraft(
            currentState.draft!!.draft.copy(
                readerAddresses = updatedReadersList.joinToString(
                    ","
                ) { it.address })
        )
    }

    fun confirmExit() {
        updateState(currentState.copy(confirmExitDialogShown = true))
    }

    fun closeExitConfirmation() {
        updateState(currentState.copy(confirmExitDialogShown = false))
    }

    fun saveDraft() {
        viewModelScope.launch(Dispatchers.IO) {
            db.draftDao().insert(currentState.draft!!.draft)
            db.draftReaderDao().insertAll(currentState.draft!!.readers)
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
        updateState(
            currentState.copy(
                mode = if (addressFieldFocused)
                    ComposingScreenMode.ContactSuggestion
                else
                    ComposingScreenMode.Default
            )
        )
        if (currentState.mode == ComposingScreenMode.Default && currentState.addressFieldText.trim()
                .isNotBlank()
        ) {
            addAddress()
        }
    }

    fun addContactSuggestion(person: PublicUserData) {
        val readers: ArrayList<DBDraftReader> = ArrayList(currentState.draft!!.readers)
        if (!readers.any { it.address == person.address }) {
            viewModelScope.launch(Dispatchers.IO) {
                updateState(currentState.copy(loading = true))
                val allContacts = db.userDao().getAll()
                if (!allContacts.any { it.address == person.address }) {
                    if (currentState.externalContact?.address == person.address) {
                        updateState(currentState.copy(externalContact = null))
                    }

                    addContactRepository.addContact(person)

                }

                readers.add(person.toDBDraftReader(currentState.draft!!.draft.draftId))
                val updatedDraftWithReaders = currentState.draft!!.copy(readers = readers)

                updateState(currentState.copy(draft = updatedDraftWithReaders, loading = false))
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
        ).split(",").filter { it.isNotBlank() }.map { it.toUri() }
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
    val draft: DBDraftWithReaders? = null,
    val mode: ComposingScreenMode = ComposingScreenMode.Default,
    val intentAttachments: String? = null,
    val addressFieldText: Address = "",
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
    val currentUser: UserData? = null,
)

enum class ComposingScreenMode {
    Default, ContactSuggestion
}