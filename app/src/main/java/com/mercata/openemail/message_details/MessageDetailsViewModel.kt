package com.mercata.openemail.message_details

import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.db.archive.DBArchiveWitAttachments
import com.mercata.openemail.db.drafts.DBDraftWithReaders
import com.mercata.openemail.db.messages.DBMessageWithDBAttachments
import com.mercata.openemail.db.messages.FusedAttachment
import com.mercata.openemail.home_screen.HomeScreen
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.repository.SyncRepository
import com.mercata.openemail.utils.AttachmentResult
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.DownloadStatus
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
import java.io.File

class MessageDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<MessageDetailsState>(
        MessageDetailsState(
            messageId = savedStateHandle.get<String>("messageId")!!,
            scope = HomeScreen.getById(savedStateHandle.get<String>("scope")!!)
        )
    ) {

    init {
        val db: AppDatabase by inject()
        val dl: DownloadRepository by inject()
        val sp: SharedPreferences by inject()
        val fu: FileUtils by inject()

        viewModelScope.launch(Dispatchers.IO) {
            val message: HomeItem = when (currentState.scope) {
                HomeScreen.Outbox,
                HomeScreen.Inbox,
                HomeScreen.Broadcast -> db.messagesDao().getById(currentState.messageId)

                HomeScreen.Drafts -> db.draftDao().getById(currentState.messageId)
                HomeScreen.Trash -> db.archiveDao().getById(currentState.messageId)
                else -> null
            }!!

            launch {
                when (currentState.scope) {
                    HomeScreen.Inbox,
                    HomeScreen.Broadcast -> {
                        val message = message as DBMessageWithDBAttachments
                        db.messagesDao().update(message.message.copy(isUnread = false))
                    }

                    else -> {
                        //ignore
                    }
                }
            }

            launch {
                updateState(
                    currentState.copy(
                        message = message,
                        currentUser = sp.getUserData(),
                    )
                )

                currentState.attachments.clear()

                when (currentState.scope) {
                    HomeScreen.Outbox,
                    HomeScreen.Inbox,
                    HomeScreen.Trash,
                    HomeScreen.Broadcast -> {
                        val fusedAttachments = when (message) {
                            is DBMessageWithDBAttachments -> {
                                message.getFusedAttachments()
                            }

                            is DBArchiveWitAttachments -> {
                                message.getFusedAttachments()
                            }

                            else -> {
                                listOf()
                            }
                        }
                        val downloadedAttachments =
                            dl.getDownloadedAttachmentsForMessage(fusedAttachments)
                        fusedAttachments.forEach {
                            val downloaded: AttachmentResult? = downloadedAttachments[it]
                            currentState.attachments.add(
                                AttachmentDetails(
                                    file = downloaded?.file,
                                    name = it.name,
                                    fileSize = it.fileSize,
                                    fileType = it.fileType,
                                    attachmentDownloadStatus = if (downloaded == null) {
                                        AttachmentDownloadStatus.NotDownloaded
                                    } else {
                                        if (downloaded.file == null) {
                                            AttachmentDownloadStatus.Downloading
                                        } else {
                                            AttachmentDownloadStatus.Downloaded
                                        }
                                    },
                                    downloadStatus = downloaded?.status ?: Progress(0),
                                    fusedAttachment = it
                                )
                            )
                        }
                    }

                    HomeScreen.Drafts -> {
                        val message = message as DBDraftWithReaders
                        message.draft.attachmentUriList?.split(",")?.forEach { uriStr ->
                            val uri = uriStr.toUri()
                            val uriInfo = fu.getURLInfo(uri)
                            val file = fu.getFileFromUri(uri)
                            currentState.attachments.add(
                                AttachmentDetails(
                                    file = file,
                                    name = uriInfo.name,
                                    fileSize = uriInfo.size,
                                    fileType = uriInfo.mimeType,
                                    attachmentDownloadStatus = AttachmentDownloadStatus.Downloaded,
                                    downloadStatus = Progress(100),
                                    fusedAttachment = null
                                )
                            )
                        }
                    }

                    else -> {
                        //ignore
                    }
                }
            }

            launch {
                if (currentState.scope == HomeScreen.Outbox) {
                    val readersPublicData: List<PublicUserData?>? =
                        (message as DBMessageWithDBAttachments).message.readerAddresses?.split(",")
                            ?.map {
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
    private val syncRepository: SyncRepository by inject()

    fun downloadFile(attachment: AttachmentDetails) {
        val index = currentState.attachments.indexOf(attachment)
        currentState.attachments[index].downloadStatus = Progress(0)
        currentState.attachments[index].file = null
        attachment.fusedAttachment?.let {
            viewModelScope.launch(Dispatchers.IO) {
                downloadRepository.downloadAttachment(sp.getUserData()!!, it)
                    .collect { result ->
                        currentState.attachments[index].downloadStatus = result.status
                        currentState.attachments[index].file = result.file
                    }
            }
        }
    }

    fun share(attachment: AttachmentDetails) {
        val uri = fileUtils.getUriForFile(attachment.file!!)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = attachment.fileType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        updateState(currentState.copy(shareIntent = sendIntent))
    }

    fun getOpenIntent(attachment: AttachmentDetails): Intent {
        val uri = fileUtils.getUriForFile(attachment.file!!)
        return Intent().apply {
            action = Intent.ACTION_VIEW
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
            when (currentState.scope) {
                HomeScreen.Broadcast,
                HomeScreen.Inbox,
                HomeScreen.Outbox -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        (currentState.message as DBMessageWithDBAttachments).message.copy(
                            markedToDelete = true
                        ).let { db.messagesDao().update(it) }
                        syncRepository.syncDeletedMessages()
                    }
                }

                HomeScreen.Drafts -> {
                    db.draftDao().delete(currentState.message!!.getMessageId())
                }

                HomeScreen.Trash -> {
                    db.archiveDao().delete(currentState.message!!.getMessageId())
                }

                HomeScreen.DownloadedAttachments,
                HomeScreen.Pending,
                HomeScreen.Contacts -> {
                    //ignore
                }
            }

        }
    }

    fun toggleDeletionConfirmation(shown: Boolean) {
        updateState(currentState.copy(deleteConfirmationShown = shown))
    }
}

data class MessageDetailsState(
    val scope: HomeScreen,
    val messageId: String,
    val outboxAddresses: List<PublicUserData>? = null,
    val deleteConfirmationShown: Boolean = false,
    val shareIntent: Intent? = null,
    val message: HomeItem? = null,
    val currentUser: UserData? = null,
    val attachments: SnapshotStateList<AttachmentDetails> = mutableStateListOf()
)

data class AttachmentDetails(
    var file: File?,
    val name: String,
    val fileSize: Long,
    val attachmentDownloadStatus: AttachmentDownloadStatus,
    var downloadStatus: DownloadStatus,
    val fileType: String,
    val fusedAttachment: FusedAttachment?
)