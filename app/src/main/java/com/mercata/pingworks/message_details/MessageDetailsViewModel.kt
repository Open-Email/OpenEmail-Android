package com.mercata.pingworks.message_details

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.db.messages.FusedAttachment
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.AttachmentResult
import com.mercata.pingworks.utils.Downloader
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.Progress
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class MessageDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<MessageDetailsState>(
        MessageDetailsState(
            messageId = savedStateHandle.get<String>("messageId")!!,
        )
    ) {

    init {
        val db: AppDatabase by inject()
        val dl: Downloader by inject()
        val sp: SharedPreferences by inject()
        viewModelScope.launch {
            val message = db.messagesDao().getById(currentState.messageId)

            launch {
                when (val call =
                    safeApiCall { getProfilePublicData(message?.message?.author?.address ?: "") }) {
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

        }
    }

    private val downloader: Downloader by inject()

    fun downloadFile(attachment: FusedAttachment) {
        currentState.attachmentsWithStatus[attachment] = AttachmentResult(null, Progress(0))
        viewModelScope.launch(Dispatchers.IO) {
            downloader.downloadAttachment(sp.getUserData()!!, attachment)
                .collect { result ->
                    currentState.attachmentsWithStatus[attachment] = result
                }
        }
    }

    fun share(uri: Uri, attachment: FusedAttachment) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = attachment.fileType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        updateState(currentState.copy(shareIntent = sendIntent))
    }

    fun shared() {
        updateState(currentState.copy(shareIntent = null))
    }
}

data class MessageDetailsState(
    val messageId: String,
    val noReply: Boolean = true,
    val shareIntent: Intent? = null,
    val message: DBMessageWithDBAttachments? = null,
    val currentUser: UserData? = null,
    val attachmentsWithStatus: SnapshotStateMap<FusedAttachment, AttachmentResult> = mutableStateMapOf()
)