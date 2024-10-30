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
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.Downloader
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import com.mercata.pingworks.utils.uploadPendingMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    init {
        val sp: SharedPreferences by inject()
        val db: AppDatabase by inject()
        val dl: Downloader by inject()
        val fu: FileUtils by inject()
        updateState(
            currentState.copy(
                currentUser = sp.getUserData(),
                screen = sp.getSelectedNavigationScreen()
            )
        )
        viewModelScope.launch {
            db.messagesDao().getAllAsFlowWithAttachments().collect { dbEntities ->
                allMessages.clear()
                allMessages.addAll(dbEntities)
                updateList()
            }
        }
        viewModelScope.launch {
            db.pendingMessagesDao().getAllAsFlowWithAttachments().collect { pending ->
                pendingMessages.clear()
                pendingMessages.addAll(pending)
                currentState.unread[HomeScreen.Pending] = pendingMessages.size
                updateList()
            }
        }
        viewModelScope.launch {
            db.draftDao().getAll().collect { drafts ->
                draftMessages.clear()
                draftMessages.addAll(drafts)
                currentState.unread[HomeScreen.Drafts] = draftMessages.size
                updateList()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(refreshing = true))
            syncContacts(sp, db.userDao())
            uploadPendingMessages(sp.getUserData()!!, db, fu, sp)
            syncAllMessages(db, sp, dl)
            updateState(currentState.copy(refreshing = false))
        }
    }

    private val dl: Downloader by inject()
    private val fu: FileUtils by inject()
    private val allMessages: ArrayList<DBMessageWithDBAttachments> = arrayListOf()
    private val pendingMessages: ArrayList<DBPendingMessage> = arrayListOf()
    private val draftMessages: ArrayList<DBDraftWithReaders> = arrayListOf()

    fun onSearchQuery(query: String) {
        updateState(currentState.copy(query = query))
        updateList()
    }

    fun selectScreen(screen: HomeScreen) {
        updateState(currentState.copy(screen = screen))
        updateList()
        sp.saveSelectedNavigationScreen(screen)
    }

    fun removeItem(item: DBMessageWithDBAttachments) {
        //TODO remove from DB (outbox only)
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
            updateState(currentState.copy(refreshing = false))
        }
    }

    private fun updateList() {
        val currentUserAddress = sp.getUserAddress()
        currentState.messages.clear()

        when (currentState.screen) {
            HomeScreen.Drafts -> {
                currentState.messages.addAll(draftMessages.filter { it.searchMatched() })
            }

            HomeScreen.Pending -> {
                currentState.messages.addAll(pendingMessages.filter { it.searchMatched() })
            }

            HomeScreen.Broadcast -> currentState.messages.addAll(allMessages.filter {
                it.message.message.isBroadcast
                        && it.message.author?.address != currentUserAddress
                        && it.searchMatched()
            })

            HomeScreen.Outbox -> {
                currentState.messages.addAll(allMessages.filter {
                    it.message.author?.address == currentUserAddress && it.searchMatched()
                })
            }

            HomeScreen.Inbox -> {
                currentState.messages.addAll(allMessages.filter {
                    it.message.message.isBroadcast.not() &&
                            it.message.author?.address != currentUserAddress && it.searchMatched()
                })
            }
        }
    }

    private fun HomeItem.searchMatched(): Boolean =
        this.getSubject().lowercase().contains(currentState.query.lowercase()) ||
                this.getTextBody().lowercase().contains(currentState.query.lowercase())

    fun deleteDraft(draft: DBDraftWithReaders) {
        viewModelScope.launch(Dispatchers.IO) {
            db.draftDao().delete(draft.draft.draftId)
        }
    }

}

data class HomeState(
    val currentUser: UserData? = null,
    val searchOpened: Boolean = false,
    val query: String = "",
    val refreshing: Boolean = false,
    val screen: HomeScreen = HomeScreen.Broadcast,
    val messages: SnapshotStateList<HomeItem> = mutableStateListOf(),
    //TODO get unread statuses from DB
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
    Drafts(R.string.drafts, iconResId = R.drawable.draft, outbox = true)
}