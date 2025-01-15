package com.mercata.pingworks.home_screen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.drafts.DBDraftWithReaders
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.db.notifications.DBNotification
import com.mercata.pingworks.db.notifications.toPublicUserData
import com.mercata.pingworks.db.pending.DBPendingMessage
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.models.CachedAttachment
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBContact
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.repository.SendMessageRepository
import com.mercata.pingworks.utils.Address
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.revokeMarkedOutboxMessages
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.syncContacts
import com.mercata.pingworks.utils.syncNotifications
import com.mercata.pingworks.utils.uploadPendingMessages
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    private val dl: DownloadRepository by inject()
    private val fu: FileUtils by inject()
    private val sendMessageRepository: SendMessageRepository by inject()
    private var listUpdateState: HomeListUpdateState? = null

    val items: SnapshotStateList<HomeItem> = mutableStateListOf()
    val selectedItems: SnapshotStateList<HomeItem> = mutableStateListOf()
    val unread: SnapshotStateMap<HomeScreen, Int> = mutableStateMapOf()

    data class HomeListUpdateState(
        val dbMessages: List<DBMessageWithDBAttachments>,
        val dbPendingMessages: List<DBPendingMessage>,
        val dbDrafts: List<DBDraftWithReaders>,
        val dbContacts: List<DBContact>,
        val dbNotifications: List<DBNotification>,
        val attachments: ArrayList<CachedAttachment> = arrayListOf()
    )

    init {
        val sp: SharedPreferences by inject()
        val db: AppDatabase by inject()
        val dl: DownloadRepository by inject()
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
            combine(
                db.messagesDao().getAllAsFlowWithAttachments(),
                db.pendingMessagesDao().getAllAsFlowWithAttachments(),
                db.draftDao().getAllFlow(),
                db.userDao().getAllAsFlow(),
                db.notificationsDao().getAllAsFlow(),
            ) { dbMessages, dbPendingMessages, dbDrafts, dbContacts, dbNotifications ->
                HomeListUpdateState(
                    dbMessages.filterNot { it.message.message.markedToDelete }.toList(),
                    dbPendingMessages,
                    dbDrafts,
                    dbContacts,
                    dbNotifications.filterNot {
                        it.address == sp.getUserAddress() || it.isExpired() || it.dismissed
                    })
            }.combine(dl.downloadedAttachmentsState) { listUpdateState, newAttachments ->
                listUpdateState.apply {
                    attachments.clear()
                    attachments.addAll(newAttachments)
                }
            }.collect { listUpdateState ->

                var unreadBroadcasts = 0
                var unreadMessages = 0
                listUpdateState.dbMessages.forEach {
                    if (it.isUnread()) {
                        if (it.message.message.isBroadcast) {
                            unreadBroadcasts++
                        } else {
                            unreadMessages++
                        }
                    }
                }
                unread[HomeScreen.Inbox] = unreadMessages
                unread[HomeScreen.Broadcast] = unreadBroadcasts
                unread[HomeScreen.Pending] = listUpdateState.dbPendingMessages.size
                unread[HomeScreen.Drafts] = listUpdateState.dbDrafts.size

                //unread[HomeScreen.Contacts] = listUpdateState.dbContacts.filterIsInstance<DBNotification>().size

                this@HomeViewModel.listUpdateState = listUpdateState

                updateList()
            }
        }

        viewModelScope.launch {
            delay(100)
            refresh()
        }
    }

    fun contactPresented(contactAddress: Address): Boolean {
        return listUpdateState?.dbContacts?.any { it.address == contactAddress } ?: false
    }

    fun onSearchQuery(query: String) {
        viewModelScope.launch {
            updateState(currentState.copy(query = query))
            updateList()
        }
    }

    fun selectScreen(screen: HomeScreen) {
        viewModelScope.launch {
            updateState(currentState.copy(screen = screen))
            updateList()
            sp.saveSelectedNavigationScreen(screen)
            selectedItems.clear()
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(refreshing = true))

            val currentUser = sp.getUserData()!!

            listOf(
                launch {
                    syncContacts(sp, db.userDao())
                    syncAllMessages(db, sp, dl)
                    syncNotifications(currentUser, db)
                },
                launch {
                    uploadPendingMessages(currentUser, db, fu, sp)
                },
                launch {
                    revokeMarkedOutboxMessages(sp.getUserData()!!, db.messagesDao())
                },
                launch {
                    dl.getCachedAttachments()
                },
            ).joinAll()

            updateState(currentState.copy(refreshing = false))
        }
    }

    private suspend fun updateList() {
        withContext(Dispatchers.Main) {
            val currentUserAddress = sp.getUserAddress()
            items.clear()

            listUpdateState?.run {
                when (currentState.screen) {
                    HomeScreen.Drafts -> {
                        items.addAll(dbDrafts.filter { it.searchMatched() }.toList())
                    }

                    HomeScreen.Pending -> {
                        items.addAll(dbPendingMessages.filter { it.searchMatched() }.toList())
                    }

                    HomeScreen.Broadcast -> items.addAll(dbMessages.filter {
                        it.message.message.isBroadcast
                                && it.message.author?.address != currentUserAddress
                                && it.searchMatched()
                    }.toList())

                    HomeScreen.Outbox -> {
                        items.addAll(dbMessages.filter {
                            it.message.author?.address == currentUserAddress && it.searchMatched()
                        }.toList())
                    }

                    HomeScreen.Inbox -> {
                        items.addAll(dbMessages.filter {
                            it.message.message.isBroadcast.not() &&
                                    it.message.author?.address != currentUserAddress && it.searchMatched()
                        }.toList())
                    }

                    HomeScreen.DownloadedAttachments -> items.addAll(attachments.filter { it.searchMatched() }
                        .toList())

                    HomeScreen.Contacts -> {
                        items.clear()
                        val filteredNotifications: List<DBNotification> =
                            dbNotifications.filter { it.searchMatched() }.toList()
                        val filteredContacts: List<DBContact> =
                            dbContacts.filter { it.searchMatched() }.toList()

                        val bothTypesPresented =
                            filteredNotifications.isNotEmpty() && filteredContacts.isNotEmpty()
                        if (bothTypesPresented) {
                            items.add(NotificationSeparator(filteredNotifications.size))
                        }

                        items.addAll(filteredNotifications)

                        if (bothTypesPresented) {
                            items.add(ContactsSeparator(filteredContacts.size))
                        }
                        items.addAll(filteredContacts)
                    }
                }
            }
        }
    }

    private fun HomeItem.searchMatched(): Boolean =
        this.getMessageId() != currentState.itemToDelete?.getMessageId()
                && (this.getSubtitle()?.lowercase()
            ?.contains(currentState.query.lowercase()) ?: false
                || this.getTextBody().lowercase().contains(currentState.query.lowercase())
                || this.getTitle().lowercase().contains(currentState.query.lowercase()))

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
            updateList()
            var undoSnackbarResId: Int? = null
            when (item) {
                is CachedAttachment -> {
                    undoSnackbarResId = R.string.attachment_deleted
                }

                is DBDraftWithReaders -> {
                    undoSnackbarResId = R.string.draft_deleted
                }

                is DBMessageWithDBAttachments -> {
                    undoSnackbarResId = R.string.message_deleted
                }

                is DBNotification -> {
                    undoSnackbarResId = R.string.contact_request_dismissed
                }

                is DBContact -> {
                    undoSnackbarResId = R.string.contact_deleted
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

            is DBMessageWithDBAttachments -> {
                db.messagesDao().update(
                    (currentState.itemToDelete as DBMessageWithDBAttachments).message.message.copy(
                        markedToDelete = true
                    )
                )
                sendMessageRepository.revokeMarkedMessages()
            }

            is DBContact -> {
                dl.deleteAttachmentsForMessages(
                    db.messagesDao()
                        .getAllForContactAddress((currentState.itemToDelete as DBContact).address)
                )
                db.userDao()
                    .update((currentState.itemToDelete as DBContact).copy(markedToDelete = true))
            }

            is DBNotification -> {
                db.notificationsDao()
                    .update((currentState.itemToDelete as DBNotification).copy(dismissed = true))
            }
        }
        refresh()
        updateState(currentState.copy(itemToDelete = null))
    }

    fun onUndoDeletePressed() {
        viewModelScope.launch(Dispatchers.IO) {
            when (currentState.itemToDelete) {
                is CachedAttachment -> {
                    dl.getCachedAttachments()
                }
            }
            updateState(
                currentState.copy(
                    itemToDelete = null,
                    undoDelete = null
                )
            )
            updateList()
        }
    }

    fun updateRead(item: DBMessageWithDBAttachments) {
        viewModelScope.launch(Dispatchers.IO) {
            db.messagesDao()
                .update(item.message.message.copy(isUnread = item.message.message.isUnread.not()))
        }
    }

    fun toggleSelectItem(item: HomeItem) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
    }

    suspend fun addSelectedNotificationsToContacts() {
        withContext(Dispatchers.IO) {
            val selectedRequests = selectedItems.filterIsInstance<DBNotification>()
            val approvedRequests = selectedRequests.map { request ->
                request.toPublicUserData().toDBContact().copy(uploaded = false)
            }

            db.userDao().insertAll(approvedRequests)
            db.notificationsDao().deleteList(selectedRequests)
            hideRequestApprovingConfirmationDialog()
            syncWithServer()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun syncWithServer() {
        GlobalScope.launch(Dispatchers.IO) {
            onDeleteWaitComplete()
            val db: AppDatabase by inject()
            val sp: SharedPreferences by inject()
            val dl: DownloadRepository by inject()
            syncContacts(sp, db.userDao())
            syncAllMessages(db, sp, dl)
        }
    }

    fun showRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(addRequestsToContactsDialogShown = true))
    }

    fun hideRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(addRequestsToContactsDialogShown = false))
    }

    fun addContact() {
        viewModelScope.launch(Dispatchers.IO) {
            val publicData = currentState.existingContactFound!!
            val dbContact = publicData.toDBContact().copy(uploaded = false)
            db.userDao().insert(dbContact)
            toggleSearchAddressDialog(false)
        }
    }

    private fun emailValid(): Boolean {
        if (currentState.newContactAddressInput.isBlank()) {
            return false
        }

        return currentState.newContactAddressInput.lowercase().matches(emailRegex)
    }

    fun clearFoundContact() {
        updateState(currentState.copy(existingContactFound = null))
    }

    fun onNewContactAddressInput(str: String) {
        updateState(currentState.copy(newContactAddressInput = str, addressNotFoundError = false))
        updateState(currentState.copy(searchButtonActive = emailValid()))
    }

    fun searchNewContact() {
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val address = currentState.newContactAddressInput.trim()

            when (val call = safeApiCall { getProfilePublicData(address) }) {
                is HttpResult.Success -> {
                    updateState(
                        currentState.copy(
                            existingContactFound = call.data,
                            addressNotFoundError = false
                        )
                    )
                }

                is HttpResult.Error -> {
                    updateState(
                        currentState.copy(
                            existingContactFound = null,
                            addressNotFoundError = true
                        )
                    )
                }
            }

            updateState(currentState.copy(loading = false))
        }
    }

    fun toggleSearchAddressDialog(isShown: Boolean) {
        updateState(
            currentState.copy(
                newContactSearchDialogShown = isShown,
                addressNotFoundError = false,
                newContactAddressInput = ""
            )
        )
        if (!isShown) {
            clearFoundContact()
        }
    }

    fun deleteSelected() {
        selectedItems.forEach {
            when (it) {
                is CachedAttachment -> {
                    dl.deleteFile(it.uri)
                }
            }
        }
        selectedItems.clear()
    }

    fun clearSelection() {
        selectedItems.clear()
    }
}

data class HomeState(
    val itemToDelete: HomeItem? = null,
    val currentUser: UserData? = null,
    val addRequestsToContactsDialogShown: Boolean = false,
    val searchButtonActive: Boolean = false,
    val loading: Boolean = false,
    val addressNotFoundError: Boolean = false,
    val sendingSnackBar: Boolean = false,
    val newContactSearchDialogShown: Boolean = false,
    val newContactAddressInput: String = "",
    val query: String = "",
    val refreshing: Boolean = false,
    val undoDelete: Int? = null,
    val existingContactFound: PublicUserData? = null,
    val screen: HomeScreen = HomeScreen.Broadcast,
)

enum class HomeScreen(
    val titleResId: Int,
    val outbox: Boolean,
    val placeholderDescriptionResId: Int,
    val iconResId: Int,
    val fabIcon: Int = R.drawable.edit,
    val fabTitleRes: Int = R.string.create_message,
) {
    Broadcast(
        R.string.broadcast_title,
        iconResId = R.drawable.cast,
        outbox = false,
        placeholderDescriptionResId = R.string.broadcast_placeholder
    ),
    Inbox(
        R.string.inbox_title,
        iconResId = R.drawable.inbox,
        outbox = false,
        placeholderDescriptionResId = R.string.inbox_placeholder
    ),
    Outbox(
        R.string.outbox_title,
        iconResId = R.drawable.outbox,
        outbox = true,
        placeholderDescriptionResId = R.string.outbox_placeholder
    ),
    Pending(
        R.string.pending,
        iconResId = R.drawable.pending,
        outbox = true,
        placeholderDescriptionResId = R.string.pending_placeholder
    ),
    Drafts(
        R.string.drafts,
        iconResId = R.drawable.draft,
        outbox = true,
        placeholderDescriptionResId = R.string.drafts_placeholder
    ),
    DownloadedAttachments(
        R.string.downloaded_attachments,
        iconResId = R.drawable.download,
        outbox = false,
        placeholderDescriptionResId = R.string.downloaded_attachemnts_placeholder
    ),
    Contacts(
        R.string.contacts,
        iconResId = R.drawable.contacts,
        outbox = false,
        placeholderDescriptionResId = R.string.contacts_placeholder,
        fabIcon = R.drawable.add_contact,
        fabTitleRes = R.string.add_contact,
    ),
}

