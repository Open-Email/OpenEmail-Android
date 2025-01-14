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
import com.mercata.pingworks.db.contacts.ContactItem
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    private val dl: DownloadRepository by inject()
    private val fu: FileUtils by inject()
    private val sendMessageRepository: SendMessageRepository by inject()
    private val allMessages: ArrayList<DBMessageWithDBAttachments> = arrayListOf()
    private val pendingMessages: ArrayList<DBPendingMessage> = arrayListOf()
    private val draftMessages: ArrayList<DBDraftWithReaders> = arrayListOf()
    private val contacts: ArrayList<ContactItem> = arrayListOf()
    private val notifications: ArrayList<ContactItem> = arrayListOf()
    private val cachedAttachments: ArrayList<CachedAttachment> = arrayListOf()

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
            db.messagesDao().getAllAsFlowWithAttachments().collect { dbEntities ->
                allMessages.clear()
                allMessages.addAll(dbEntities.filterNot { it.message.message.markedToDelete }
                    .toList())
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
            db.userDao().getAllAsFlow().collect { dbContacts ->
                contacts.clear()
                contacts.addAll(dbContacts)
                currentState.unread[HomeScreen.Contacts] =
                    contacts.filterIsInstance<DBNotification>().size
                updateList()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.notificationsDao().getAllAsFlow().collect { dbEntities ->
                notifications.clear()
                notifications.addAll(dbEntities.filterNot {
                    it.address == sp.getUserAddress() || it.isExpired() || it.dismissed
                }.toList())
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

        viewModelScope.launch {
            delay(100)
            refresh()
        }
    }

    fun contactPresented(contactAddress: Address): Boolean {
        return contacts.any { it.address == contactAddress }
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

    private fun updateList() {
        val currentUserAddress = sp.getUserAddress()
        currentState.items.clear()

        when (currentState.screen) {
            HomeScreen.Drafts -> {
                currentState.items.addAll(draftMessages.filter { it.searchMatched() }.toList())
            }

            HomeScreen.Pending -> {
                currentState.items.addAll(pendingMessages.filter { it.searchMatched() }.toList())
            }

            HomeScreen.Broadcast -> currentState.items.addAll(allMessages.filter {
                it.message.message.isBroadcast
                        && it.message.author?.address != currentUserAddress
                        && it.searchMatched()
            }.toList())

            HomeScreen.Outbox -> {
                currentState.items.addAll(allMessages.filter {
                    it.message.author?.address == currentUserAddress && it.searchMatched()
                }.toList())
            }

            HomeScreen.Inbox -> {
                currentState.items.addAll(allMessages.filter {
                    it.message.message.isBroadcast.not() &&
                            it.message.author?.address != currentUserAddress && it.searchMatched()
                }.toList())
            }

            HomeScreen.DownloadedAttachments -> currentState.items.addAll(cachedAttachments.filter { it.searchMatched() }
                .toList())

            HomeScreen.Contacts -> {
                val filteredNotifications = notifications.filter { it.searchMatched() }.toList()
                val filteredContacts = contacts.filter { it.searchMatched() }.toList()

                val bothTypesPresented = filteredNotifications.isNotEmpty() && filteredContacts.isNotEmpty()
                if (bothTypesPresented) {
                    currentState.items.add(NotificationSeparator(notifications.size))
                }
                currentState.items.addAll(filteredNotifications)

                if (bothTypesPresented) {
                    currentState.items.add(ContactsSeparator(contacts.size))
                }
                currentState.items.addAll(filteredContacts)
            }
        }
    }

    private fun HomeItem.searchMatched(): Boolean =
        this.getSubtitle()?.lowercase()?.contains(currentState.query.lowercase()) ?: false ||
                this.getTextBody().lowercase()
                    .contains(currentState.query.lowercase()) || this.getTitle().lowercase()
            .contains(currentState.query.lowercase())


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
                    db.messagesDao().getAllForContactAddress((currentState.itemToDelete as DBContact).address)
                )
                db.userDao().update((currentState.itemToDelete as DBContact).copy(markedToDelete = true))
            }

            is DBNotification -> {
                db.notificationsDao().update((currentState.itemToDelete as DBNotification).copy(dismissed = true))
            }
        }
        updateState(currentState.copy(itemToDelete = null))
        refresh()
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
        }
    }

    fun updateRead(item: DBMessageWithDBAttachments) {
        viewModelScope.launch(Dispatchers.IO) {
            db.messagesDao()
                .update(item.message.message.copy(isUnread = item.message.message.isUnread.not()))
        }
    }

    fun toggleSelectItem(item: HomeItem) {
        if (currentState.selectedItems.contains(item)) {
            currentState.selectedItems.remove(item)
        } else {
            currentState.selectedItems.add(item)
        }
    }

    suspend fun addSelectedNotificationsToContacts() {
        withContext(Dispatchers.IO) {
            val selectedRequests = currentState.selectedItems.filterIsInstance<DBNotification>()
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
        updateState(currentState.copy(newContactAddressInput = str))
        updateState(currentState.copy(searchButtonActive = emailValid()))
    }

    fun searchNewContact() {
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val address = currentState.newContactAddressInput.trim()
            val publicData: PublicUserData? =
                when (val call = safeApiCall { getProfilePublicData(address) }) {
                    is HttpResult.Success -> {
                        call.data
                    }

                    is HttpResult.Error -> {
                        null
                    }
                }
            publicData?.let {
                updateState(currentState.copy(existingContactFound = it))
            }
            updateState(currentState.copy(loading = false))
        }
    }

    fun toggleSearchAddressDialog(isShown: Boolean) {
        updateState(currentState.copy(newContactSearchDialogShown = isShown))
        if (!isShown) {
            updateState(currentState.copy(newContactAddressInput = ""))
            clearFoundContact()
        }
    }

    fun deleteSelected() {
        currentState.selectedItems.forEach {
            when (it) {
                is CachedAttachment -> {
                    dl.deleteFile(it.uri)
                }
            }
        }
        currentState.selectedItems.clear()
    }

    fun clearSelection() {
        currentState.selectedItems.clear()
    }
}

data class HomeState(
    val itemToDelete: HomeItem? = null,
    val currentUser: UserData? = null,
    val addRequestsToContactsDialogShown: Boolean = false,
    val searchButtonActive: Boolean = false,
    val loading: Boolean = false,
    val searchOpened: Boolean = false,
    val sendingSnackBar: Boolean = false,
    val newContactSearchDialogShown: Boolean = false,
    val newContactAddressInput: String = "",
    val query: String = "",
    val refreshing: Boolean = false,
    val undoDelete: Int? = null,
    val existingContactFound: PublicUserData? = null,
    val screen: HomeScreen = HomeScreen.Broadcast,
    val items: SnapshotStateList<HomeItem> = mutableStateListOf(),
    val selectedItems: SnapshotStateList<HomeItem> = mutableStateListOf(),
    val unread: SnapshotStateMap<HomeScreen, Int> = mutableStateMapOf()
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

