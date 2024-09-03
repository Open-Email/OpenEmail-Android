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
import com.mercata.pingworks.SharedPreferences
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.models.BroadcastMessage
import com.mercata.pingworks.models.Message
import com.mercata.pingworks.syncBroadcasts
import com.mercata.pingworks.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    init {
        val sp: SharedPreferences by inject(SharedPreferences::class.java)
        val db: AppDatabase by inject(AppDatabase::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            syncContacts(sp, db.userDao())

            syncBroadcasts(sp, db.userDao())
        }
    }

    fun selectScreen(screen: HomeScreen) {
        updateState(currentState.copy(screen = screen))
    }

    fun removeItem(item: Message) {
        currentState.messages.remove(item)
    }
}

data class HomeState(
    val screen: HomeScreen = HomeScreen.Inbox,
    val messages: SnapshotStateList<BroadcastMessage> = mutableStateListOf(),
    //TODO get unread statuses from DB
    val unread: Map<HomeScreen, Int> = mapOf()
)

enum class HomeScreen(val screenName: String, val titleResId: Int, val icon: ImageVector) {
    Broadcast("BroadcastListScreen", R.string.broadcast_title, Icons.Default.Email),
    Inbox("InboxListScreen", R.string.inbox_title, Icons.Default.KeyboardArrowDown),
    Outbox("OutboxListScreen", R.string.outbox_title, Icons.Default.KeyboardArrowUp)
}