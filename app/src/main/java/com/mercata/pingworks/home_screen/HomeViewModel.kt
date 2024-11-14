package com.mercata.pingworks.home_screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.drafts.DBDraftWithReaders
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.db.pending.DBPendingMessage
import com.mercata.pingworks.models.CachedAttachment
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.repository.SendMessageRepository
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import com.mercata.pingworks.utils.uploadPendingMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    private val dl: DownloadRepository by inject()
    private val fu: FileUtils by inject()
    private val allMessages: ArrayList<DBMessageWithDBAttachments> = arrayListOf()
    private val pendingMessages: ArrayList<DBPendingMessage> = arrayListOf()
    private val draftMessages: ArrayList<DBDraftWithReaders> = arrayListOf()
    private val cachedAttachments: ArrayList<CachedAttachment> = arrayListOf()

    init {
        val sp: SharedPreferences by inject()
        val db: AppDatabase by inject()
        val dl: DownloadRepository by inject()
        val fu: FileUtils by inject()
        val sendMessageRepository: SendMessageRepository by inject()
        viewModelScope.launch {
            sendMessageRepository.sendingState.collect { isSending ->
                updateState(currentState.copy(sendingSnackBar = isSending))
            }
        }
        updateState(
            currentState.copy(
                currentUser = sp.getUserData(),
                screen = sp.getSelectedNavigationScreen()
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            db.messagesDao().getAllAsFlowWithAttachments().collect { dbEntities ->
                allMessages.clear()
                allMessages.addAll(dbEntities)
                var unreadBroadcasts = 0
                var unreadMessages = 0
                dbEntities.forEach {
                    if (it.isUnread()) {
                        if (it.message.message.isBroadcast) {
                            unreadBroadcasts++
                        } else {
                            unreadMessages++
                        }
                    }
                }
                currentState.unread[HomeScreen.Inbox] = unreadMessages
                currentState.unread[HomeScreen.Broadcast] = unreadBroadcasts
                updateList()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.pendingMessagesDao().getAllAsFlowWithAttachments().collect { pending ->
                pendingMessages.clear()
                pendingMessages.addAll(pending)
                currentState.unread[HomeScreen.Pending] = pendingMessages.size
                updateList()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.draftDao().getAllFlow().collect { drafts ->
                draftMessages.clear()
                draftMessages.addAll(drafts)
                currentState.unread[HomeScreen.Drafts] = draftMessages.size
                updateList()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dl.downloadedAttachmentsState.collect { attachmentsList ->
                cachedAttachments.clear()
                cachedAttachments.addAll(attachmentsList)
                updateList()
            }
        }
        refresh()
    }

    fun onSearchQuery(query: String) {
        updateState(currentState.copy(query = query))
        updateList()
    }

    fun selectScreen(screen: HomeScreen) {
        updateState(currentState.copy(screen = screen))
        updateList()
        sp.saveSelectedNavigationScreen(screen)
        currentState.selectedItems.clear()
    }

    fun toggleSearch() {
        updateState(currentState.copy(searchOpened = !currentState.searchOpened, query = ""))
        if (currentState.searchOpened.not()) {
            updateList()
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(refreshing = true))
            uploadPendingMessages(sp.getUserData()!!, db, fu, sp)
            syncAllMessages(db, sp, dl)
            dl.getCachedAttachments()
            updateState(currentState.copy(refreshing = false))
        }
    }

    private fun updateList() {
        val currentUserAddress = sp.getUserAddress()
        currentState.items.clear()

        when (currentState.screen) {
            HomeScreen.Drafts -> {
                currentState.items.addAll(draftMessages.filter { it.searchMatched() })
            }

            HomeScreen.Pending -> {
                currentState.items.addAll(pendingMessages.filter { it.searchMatched() })
            }

            HomeScreen.Broadcast -> currentState.items.addAll(allMessages.filter {
                it.message.message.isBroadcast
                        && it.message.author?.address != currentUserAddress
                        && it.searchMatched()
            })

            HomeScreen.Outbox -> {
                currentState.items.addAll(allMessages.filter {
                    it.message.author?.address == currentUserAddress && it.searchMatched()
                })
            }

            HomeScreen.Inbox -> {
                currentState.items.addAll(allMessages.filter {
                    it.message.message.isBroadcast.not() &&
                            it.message.author?.address != currentUserAddress && it.searchMatched()
                })
            }

            HomeScreen.DownloadedAttachments -> currentState.items.addAll(cachedAttachments.filter { it.searchMatched() })
        }
    }

    private fun HomeItem.searchMatched(): Boolean =
        this.getSubject().lowercase().contains(currentState.query.lowercase()) ||
                this.getTextBody().lowercase().contains(currentState.query.lowercase())


    fun deleteItem(item: HomeItem) {
        viewModelScope.launch(Dispatchers.IO) {
            currentState.itemToDelete?.run {
                onDeleteWaitComplete()
            }
            updateState(
                currentState.copy(
                    itemToDelete = item,
                )
            )
            updateState(currentState.copy(undoDelete = null))
            var undoSnackbarResId: Int? = null
            when (item) {
                is CachedAttachment -> {
                    undoSnackbarResId = R.string.attachment_deleted
                }

                is DBDraftWithReaders -> {
                    undoSnackbarResId = R.string.draft_deleted
                }
            }
            updateState(currentState.copy(undoDelete = undoSnackbarResId!!))
        }
    }

    fun onCountdownSnackBarFinished() {
        viewModelScope.launch(Dispatchers.IO) {
            onDeleteWaitComplete()
        }
    }

    private suspend fun onDeleteWaitComplete() {
        updateState(currentState.copy(undoDelete = null))
        when (currentState.itemToDelete) {
            is CachedAttachment -> {
                dl.deleteFile((currentState.itemToDelete as CachedAttachment).uri)
            }

            is DBDraftWithReaders -> {
                db.draftDao()
                    .delete((currentState.itemToDelete as DBDraftWithReaders).draft.draftId)
            }
        }
        updateState(currentState.copy(itemToDelete = null))
    }

    fun onUndoDeletePressed() {
        viewModelScope.launch(Dispatchers.IO) {
            when (currentState.itemToDelete) {
                is CachedAttachment -> {
                    dl.getCachedAttachments()
                }

                is DBDraftWithReaders -> {
                    db.draftDao().getAll().let { drafts ->
                        draftMessages.clear()
                        draftMessages.addAll(drafts)
                        currentState.unread[HomeScreen.Drafts] = draftMessages.size
                        updateList()
                    }
                }
            }
            updateState(
                currentState.copy(
                    itemToDelete = null,
                    undoDelete = null
                )
            )
        }
    }

    fun updateRead(item: DBMessageWithDBAttachments) {
        viewModelScope.launch(Dispatchers.IO) {
            db.messagesDao()
                .update(item.message.message.copy(isUnread = item.message.message.isUnread.not()))
        }
    }

    fun toggleSelectItem(item: HomeItem) {
        when (item) {
            is CachedAttachment -> {
                if (currentState.selectedItems.contains(item)) {
                    currentState.selectedItems.remove(item)
                } else {
                    currentState.selectedItems.add(item)
                }
            }
        }
    }

    fun deleteSelected() {
        currentState.selectedItems.forEach {
            when(it) {
                is CachedAttachment -> {
                    dl.deleteFile(it.uri)
                }
            }
        }
        currentState.selectedItems.clear()
    }

}

data class HomeState(
    val itemToDelete: HomeItem? = null,
    val currentUser: UserData? = null,
    val searchOpened: Boolean = false,
    val sendingSnackBar: Boolean = false,
    val query: String = "",
    val refreshing: Boolean = false,
    val undoDelete: Int? = null,
    val screen: HomeScreen = HomeScreen.Broadcast,
    val items: SnapshotStateList<HomeItem> = mutableStateListOf(),
    val selectedItems: SnapshotStateList<HomeItem> = mutableStateListOf(),
    val unread: SnapshotStateMap<HomeScreen, Int> = mutableStateMapOf()
)

enum class HomeScreen(
    val titleResId: Int,
    val outbox: Boolean,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
) {
    Broadcast(R.string.broadcast_title, iconResId = R.drawable.cast, outbox = false),
    Inbox(R.string.inbox_title, icon = Icons.Default.KeyboardArrowDown, outbox = false),
    Outbox(R.string.outbox_title, icon = Icons.Default.KeyboardArrowUp, outbox = true),
    Pending(R.string.pending, iconResId = R.drawable.pending, outbox = true),
    Drafts(R.string.drafts, iconResId = R.drawable.draft, outbox = true),
    DownloadedAttachments(
        R.string.downloaded_attachments,
        iconResId = R.drawable.download,
        outbox = false
    )
}