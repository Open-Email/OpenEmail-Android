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
import com.mercata.pingworks.Downloader
import com.mercata.pingworks.R
import com.mercata.pingworks.SharedPreferences
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.getAllEnvelopes
import com.mercata.pingworks.models.Envelope
import com.mercata.pingworks.syncContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    init {
        val sp: SharedPreferences by inject(SharedPreferences::class.java)
        val db: AppDatabase by inject(AppDatabase::class.java)
        val dl: Downloader by inject(Downloader::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                syncContacts(sp, db.userDao())
            }
            launch {
                //TODO save attachments links
                val envelopes = getAllEnvelopes(
                    sp,
                    db.userDao()
                ).filter { it.contentHeaders.parentId.isNullOrBlank() }
                val envelopesWithBody: List<Pair<Envelope, String>> =
                    dl.downloadFilesAndGetFolder(envelopes)
                allMessages.clear()
                allMessages.addAll(envelopesWithBody)
                updateList()
            }
        }
    }

    private val allMessages: ArrayList<Pair<Envelope, String>> = arrayListOf()


    fun onSearchQuery(query: String) {
        updateState(currentState.copy(query = query))
        updateList()
    }

    fun selectScreen(screen: HomeScreen) {
        updateState(currentState.copy(screen = screen))
        updateList()
    }

    fun removeItem(item: Pair<Envelope, String>) {
        currentState.messages.remove(item)
    }

    private fun updateList() {
        currentState.messages.clear()
        currentState.messages.addAll(allMessages.asSequence().filter {
            when (currentState.screen) {
                HomeScreen.Broadcast -> it.first.isBroadcast()
                HomeScreen.Inbox -> false //TODO
                HomeScreen.Outbox -> false //TODO
            } && (it.first.contentHeaders.subject.lowercase()
                .contains(currentState.query.lowercase()) || it.second.lowercase()
                .contains(currentState.query.lowercase()))

        })
    }
}

data class HomeState(
    val query: String = "",
    val screen: HomeScreen = HomeScreen.Broadcast,
    val messages: SnapshotStateList<Pair<Envelope, String>> = mutableStateListOf(),
    //TODO get unread statuses from DB
    val unread: Map<HomeScreen, Int> = mapOf()
)

enum class HomeScreen(val screenName: String, val titleResId: Int, val icon: ImageVector) {
    Broadcast("BroadcastListScreen", R.string.broadcast_title, Icons.Default.Email),
    Inbox("InboxListScreen", R.string.inbox_title, Icons.Default.KeyboardArrowDown),
    Outbox("OutboxListScreen", R.string.outbox_title, Icons.Default.KeyboardArrowUp)
}