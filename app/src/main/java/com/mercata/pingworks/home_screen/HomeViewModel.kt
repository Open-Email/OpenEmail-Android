package com.mercata.pingworks.home_screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    init {
        val sp: SharedPreferences by inject(SharedPreferences::class.java)
        val db: AppDatabase by inject(AppDatabase::class.java)
        val dl: Downloader by inject(Downloader::class.java)
        updateState(currentState.copy(currentUser = sp.getUserData(), screen = sp.getSelectedNavigationScreen()))
        viewModelScope.launch(Dispatchers.IO) {
            syncContacts(sp, db.userDao())
            syncAllMessages(db, sp, dl)
        }
        viewModelScope.launch {
            db.messagesDao().getAllAsFlowWithAttachments().collect { dbEntities ->
                allMessages.clear()
                allMessages.addAll(dbEntities)
                updateList()
            }
        }
    }

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

    private fun updateList() {
        currentState.messages.clear()
        currentState.messages.addAll(allMessages.asSequence().filter {
            when (currentState.screen) {
                HomeScreen.Broadcast -> it.message.message.isBroadcast
                HomeScreen.Outbox -> it.message.author?.address == null
                HomeScreen.Inbox -> it.message.message.isBroadcast.not() &&
                        it.message.author?.address != null
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
    val screen: HomeScreen = HomeScreen.Broadcast,
    val messages: SnapshotStateList<DBMessageWithDBAttachments> = mutableStateListOf(),
    //TODO get unread statuses from DB
    val unread: Map<HomeScreen, Int> = mapOf()
)

enum class HomeScreen(val screenName: String, val titleResId: Int, val icon: ImageVector) {
    Broadcast("BroadcastListScreen", R.string.broadcast_title, Icons.Default.Email),
    Inbox("InboxListScreen", R.string.inbox_title, Icons.Default.KeyboardArrowDown),
    Outbox("OutboxListScreen", R.string.outbox_title, Icons.Default.KeyboardArrowUp)
}