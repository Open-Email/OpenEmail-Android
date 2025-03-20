package com.mercata.openemail.message_details

import android.content.Intent
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.messages.DBMessageWithDBAttachments
import com.mercata.openemail.db.messages.FusedAttachment
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.repository.SendMessageRepository
import com.mercata.openemail.utils.AttachmentResult
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.Progress
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class MessageDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<MessageDetailsState>(
        MessageDetailsState(
            messageId = savedStateHandle.get<String>("messageId")!!,
            deletable = savedStateHandle.get<Boolean>("deletable")!!,
        )
    ) {

    init {
        val db: AppDatabase by inject()
        val dl: DownloadRepository by inject()
        val sp: SharedPreferences by inject()
        viewModelScope.launch(Dispatchers.IO) {
            val message = db.messagesDao().getById(currentState.messageId)

            launch {
                message?.message?.copy(isUnread = false)?.let {
                    db.messagesDao().update(it)
                }
            }

            launch {
                when (val call =
                    safeApiCall { getProfilePublicData(message?.message?.authorAddress ?: "") }) {
                    is HttpResult.Error -> updateState(currentState.copy(noReply = true))
                    is HttpResult.Success -> updateState(currentState.copy(noReply = call.data?.publicEncryptionKey.isNullOrBlank()))
                }
            }

            launch {
                updateState(
                    currentState.copy(
                        message = message,
                        currentUser = sp.getUserData(),
                    )
                )
                currentState.attachmentsWithStatus.clear()
                currentState.attachmentsWithStatus.putAll(
                    dl.getDownloadedAttachmentsForMessage(
                        message!!
                    )
                )
            }

            launch {
                if (savedStateHandle.get<Boolean>("outbox")!!) {
                    val readersPublicData: List<PublicUserData?>? =
                        message?.message?.readerAddresses?.split(",")?.map {
                            async {
                                when (val call = safeApiCall { getProfilePublicData(it) }) {
                                    is HttpResult.Error -> null
                                    is HttpResult.Success -> call.data
                                }
                            }
                        }?.awaitAll()

                    updateState(
                        currentState.copy(
                            outboxAddresses = readersPublicData?.filterNotNull() ?: listOf()
                        )
                    )
                }
            }
        }
    }

    private val downloadRepository: DownloadRepository by inject()
    private val fileUtils: FileUtils by inject()
    private val sendMessageRepository: SendMessageRepository by inject()

    fun downloadFile(attachment: FusedAttachment) {
        currentState.attachmentsWithStatus[attachment] = AttachmentResult(null, Progress(0))
        viewModelScope.launch(Dispatchers.IO) {
            downloadRepository.downloadAttachment(sp.getUserData()!!, attachment)
                .collect { result ->
                    currentState.attachmentsWithStatus[attachment] = result
                }
        }
    }

    fun share(attachment: FusedAttachment) {
        val uri = fileUtils.getUriForFile(currentState.attachmentsWithStatus[attachment]?.file!!)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = attachment.fileType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        updateState(currentState.copy(shareIntent = sendIntent))
    }

    fun getOpenIntent(attachment: FusedAttachment): Intent {
        val uri = fileUtils.getUriForFile(currentState.attachmentsWithStatus[attachment]?.file!!)
        return Intent().apply {
            setAction(Intent.ACTION_VIEW)
            setDataAndType(
                uri,
                attachment.fileType
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun shared() {
        updateState(currentState.copy(shareIntent = null))
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun deleteMessage() {
        withContext(Dispatchers.IO) {
            toggleDeletionConfirmation(false)
            currentState.message?.message?.copy(
                markedToDelete = true
            )?.let { db.messagesDao().update(it) }
            GlobalScope.launch(Dispatchers.IO) {
                sendMessageRepository.revokeMarkedMessages()
            }
        }
    }

    fun toggleDeletionConfirmation(shown: Boolean) {
        updateState(currentState.copy(deleteConfirmationShown = shown))
    }
}

data class MessageDetailsState(
    val messageId: String,
    val deletable: Boolean,
    val outboxAddresses: List<PublicUserData>? = null,
    val deleteConfirmationShown: Boolean = false,
    val noReply: Boolean = true,
    val shareIntent: Intent? = null,
    val message: DBMessageWithDBAttachments? = null,
    val currentUser: UserData? = null,
    val attachmentsWithStatus: SnapshotStateMap<FusedAttachment, AttachmentResult> = mutableStateMapOf()
)