package com.mercata.pingworks.home_screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.Downloader
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.decrypt_xchacha20poly1305
import com.mercata.pingworks.utils.encrypt_xchacha20poly1305
import com.mercata.pingworks.utils.generateRandomBytes
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    init {
        val sp: SharedPreferences by inject()
        val db: AppDatabase by inject()
        val dl: Downloader by inject()
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
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(refreshing = true))
            syncContacts(sp, db.userDao())
            syncAllMessages(db, sp, dl)
            updateState(currentState.copy(refreshing = false))
        }
    }

    private val dl: Downloader by inject()
    private val allMessages: ArrayList<DBMessageWithDBAttachments> = arrayListOf()


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
        viewModelScope.launch {
            updateState(currentState.copy(refreshing = true))
            syncAllMessages(db, sp, dl)
            updateState(currentState.copy(refreshing = false))
        }
    }

    private fun updateList() {
        val currentUserAddress = sp.getUserAddress()
        currentState.messages.clear()
        currentState.messages.addAll(allMessages.asSequence().filter {
            when (currentState.screen) {
                HomeScreen.Broadcast -> it.message.message.isBroadcast && it.message.author?.address != currentUserAddress
                HomeScreen.Outbox -> it.message.author?.address == currentUserAddress
                HomeScreen.Inbox -> it.message.message.isBroadcast.not() &&
                        it.message.author?.address != currentUserAddress
            } && (it.message.message.subject.lowercase().contains(currentState.query.lowercase()) ||
                    it.message.message.textBody.lowercase()
                        .contains(currentState.query.lowercase()))
        })
    }
}

data class HomeState(
    val currentUser: UserData? = null,
    val searchOpened: Boolean = false,
    val query: String = "",
    val refreshing: Boolean = false,
    val screen: HomeScreen = HomeScreen.Broadcast,
    val messages: SnapshotStateList<DBMessageWithDBAttachments> = mutableStateListOf(),
    //TODO get unread statuses from DB
    val unread: Map<HomeScreen, Int> = mapOf()
)

enum class HomeScreen(
    val titleResId: Int,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
) {
    Broadcast(R.string.broadcast_title, iconResId = R.drawable.cast),
    Inbox(R.string.inbox_title, Icons.Default.KeyboardArrowDown),
    Outbox(R.string.outbox_title, Icons.Default.KeyboardArrowUp)
}