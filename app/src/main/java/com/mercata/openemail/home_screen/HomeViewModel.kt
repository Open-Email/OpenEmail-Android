package com.mercata.openemail.home_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.viewModelScope
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.R
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.db.archive.DBArchiveWitAttachments
import com.mercata.openemail.db.archive.toArchive
import com.mercata.openemail.db.contacts.DBContact
import com.mercata.openemail.db.drafts.DBDraftWithReaders
import com.mercata.openemail.db.messages.DBMessageWithDBAttachments
import com.mercata.openemail.db.notifications.DBNotification
import com.mercata.openemail.db.notifications.toPublicUserData
import com.mercata.openemail.db.pending.DBPendingMessage
import com.mercata.openemail.emailRegex
import com.mercata.openemail.models.CachedAttachment
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.models.toDBContact
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.repository.AddContactRepository
import com.mercata.openemail.repository.ProcessIncomingIntentsRepository
import com.mercata.openemail.repository.SendMessageRepository
import com.mercata.openemail.utils.Address
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.NotificationsResult
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.getNewNotifications
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.revokeMarkedOutboxMessages
import com.mercata.openemail.utils.safeApiCall
import com.mercata.openemail.utils.syncAllMessages
import com.mercata.openemail.utils.syncContacts
import com.mercata.openemail.utils.uploadPendingMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class HomeViewModel : AbstractViewModel<HomeState>(HomeState()) {

    private val dl: DownloadRepository by inject()
    private val fu: FileUtils by inject()
    private val addContactRepository: AddContactRepository by inject()
    private val newIntentRepository: ProcessIncomingIntentsRepository by inject()
    private val sendMessageRepository: SendMessageRepository by inject()
    private var listUpdateState: HomeListUpdateState? = null

    val items: SnapshotStateList<HomeItem> = mutableStateListOf()
    val selectedItems: SnapshotStateList<HomeItem> = mutableStateListOf()
    val unread: SnapshotStateMap<HomeScreen, Int> = mutableStateMapOf()
    private val notificationsFlow: MutableStateFlow<NotificationsResult> = MutableStateFlow(
        NotificationsResult(
            listOf(),
            listOf()
        )
    )

    data class HomeListUpdateState(
        val dbMessages: List<DBMessageWithDBAttachments>,
        val dbPendingMessages: List<DBPendingMessage>,
        val dbDrafts: List<DBDraftWithReaders>,
        val dbContacts: List<DBContact>,
        val notifications: NotificationsResult,
        val attachments: List<CachedAttachment>,
        val archive: List<DBArchiveWitAttachments>
    )

    init {
        val sp: SharedPreferences by inject()
        val db: AppDatabase by inject()

        viewModelScope.launch {
            newIntentRepository.cachedUris.collect { uris ->
                if (uris.isNotEmpty()) {
                    updateState(currentState.copy(intentUris = arrayListOf<Uri>().apply { addAll(uris) }))
                    newIntentRepository.clear()
                }
            }
        }

        viewModelScope.launch {
            sendMessageRepository.sendingState.collect { isSending ->
                if (isSending) {
                    updateState(currentState.copy(snackBar = SnackBarData(titleResId = R.string.message_sending)))
                } else {
                    updateState(currentState.copy(snackBar = null))
                }
            }
        }
        updateState(
            currentState.copy(
                currentUser = sp.getUserData(),
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                db.messagesDao().getAllAsFlowWithAttachments(),
                db.pendingMessagesDao().getAllAsFlowWithAttachments(),
                db.draftDao().getAllFlow(),
                db.userDao().getAllAsFlow(),
                notificationsFlow
            ) { dbMessages, dbPendingMessages, dbDrafts, dbContacts, notifications ->
                HomeListUpdateState(
                    dbMessages.filterNot { it.message.markedToDelete }.toList(),
                    dbPendingMessages,
                    dbDrafts,
                    dbContacts.filterNot { it.address == sp.getUserAddress() },
                    notifications,
                    listOf(),
                    listOf()
                )
            }.combine(dl.downloadedAttachmentsState) { listUpdateState, newAttachments ->
                listUpdateState.copy(attachments = newAttachments)
            }.combine(db.archiveDao().getAllAsFlow()) { listUpdateState, archive ->
                listUpdateState.copy(archive = archive)
            }.collect { listUpdateState ->
                var unreadBroadcasts = 0
                var unreadMessages = 0
                listUpdateState.dbMessages.forEach {
                    if (it.isUnread()) {
                        if (it.message.isBroadcast) {
                            unreadBroadcasts++
                        } else {
                            unreadMessages++
                        }
                    }
                }

                if (listUpdateState.notifications.contactRequests.isNotEmpty()) {
                    updateState(
                        currentState.copy(
                            newContactsAmount = listUpdateState.notifications.contactRequests.size,
                            snackBar = SnackBarData(R.string.you_have_new_contact_requests)
                        )
                    )
                }

                unread[HomeScreen.Inbox] = unreadMessages
                unread[HomeScreen.Broadcast] = unreadBroadcasts
                unread[HomeScreen.Pending] = listUpdateState.dbPendingMessages.size
                unread[HomeScreen.Drafts] = listUpdateState.dbDrafts.size
                unread[HomeScreen.Contacts] = listUpdateState.notifications.contactRequests.size

                this@HomeViewModel.listUpdateState = listUpdateState

                updateList()
            }
        }

        viewModelScope.launch {
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
            currentState.itemToDelete?.run {
                onDeleteWaitComplete()
            }
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
                    notificationsFlow.value = getNewNotifications(sp, db)
                    syncAllMessages(db, sp, dl)
                },
                launch {
                    uploadPendingMessages(currentUser, db, fu, sp)
                },
                launch {
                    sendMessageRepository.revokeMarkedMessages()
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
                        it.message.isBroadcast
                                && it.message.authorAddress != currentUserAddress
                                && it.searchMatched()
                    }.toList())

                    HomeScreen.Outbox -> {
                        items.addAll(dbMessages.filter {
                            it.message.authorAddress == currentUserAddress && it.searchMatched()
                        }.toList())
                    }

                    HomeScreen.Inbox -> {
                        items.addAll(dbMessages.filter {
                            it.message.isBroadcast.not() &&
                                    it.message.authorAddress != currentUserAddress && it.searchMatched()
                        }.toList())
                    }

                    HomeScreen.DownloadedAttachments -> items.addAll(attachments.filter { it.searchMatched() }
                        .toList())

                    HomeScreen.Contacts -> {
                        val filteredNotifications: List<DBNotification> = notifications.contactRequests
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

                    HomeScreen.Trash -> {
                        items.addAll(archive.filter { it.searchMatched() }.toList())
                    }
                }
            }
        }
    }

    private suspend fun HomeItem.searchMatched(): Boolean =
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
                is DBArchiveWitAttachments -> {
                    undoSnackbarResId = R.string.archived_message_deleted
                }

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

    fun onDeleteSnackbarHide() {
        viewModelScope.launch(Dispatchers.IO) {
            onDeleteWaitComplete()
            clearSnackbarData()
        }
    }

    fun clearSnackbarData() {
        updateState(currentState.copy(snackBar = null))
    }

    suspend fun onDeleteWaitComplete() {
        withContext(Dispatchers.IO) {
            updateState(currentState.copy(undoDelete = null))
            when (currentState.itemToDelete) {
                is DBArchiveWitAttachments -> {
                    currentState.itemToDelete?.getMessageId()?.let {
                        db.archiveDao().delete(it)
                    }
                }

                is CachedAttachment -> {
                    dl.deleteFile((currentState.itemToDelete as CachedAttachment).uri)
                }

                is DBDraftWithReaders -> {
                    db.draftDao()
                        .delete((currentState.itemToDelete as DBDraftWithReaders).draft.draftId)
                }

                is DBMessageWithDBAttachments -> {
                    val message = (currentState.itemToDelete as DBMessageWithDBAttachments)
                    db.archiveDao().insert(message.toArchive())
                    db.archiveAttachmentsDao()
                        .insertAll(message.attachmentParts.map { it.toArchive() })
                    db.messagesDao().update(
                        message.message.copy(
                            markedToDelete = true
                        )
                    )
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
                .update(item.message.copy(isUnread = item.message.isUnread.not()))
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
            updateState(currentState.copy(refreshing = true))
            val selectedRequests = selectedItems.filterIsInstance<DBNotification>()
            val approvedRequests = selectedRequests.map { request ->
                request.toPublicUserData().toDBContact().copy(uploaded = false)
            }

            db.userDao().insertAll(approvedRequests)
            notificationsFlow.value = getNewNotifications(sp, db)
            hideRequestApprovingConfirmationDialog()
            onDeleteWaitComplete()
            addContactRepository.syncWithServer()
            updateState(currentState.copy(refreshing = false))
        }
    }

    fun showRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(addRequestsToContactsDialogShown = true))
    }

    fun hideRequestApprovingConfirmationDialog() {
        updateState(currentState.copy(addRequestsToContactsDialogShown = false))
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

    fun consumeIntentUris() {
        updateState(currentState.copy(intentUris = null))
    }
}

data class HomeState(
    val itemToDelete: HomeItem? = null,
    val currentUser: UserData? = null,
    val snackBar: SnackBarData? = null,
    val intentUris: List<Uri>? = null,
    val addRequestsToContactsDialogShown: Boolean = false,
    val searchButtonActive: Boolean = false,
    val loading: Boolean = false,
    val newContactsAmount: Int = 0,
    val addressNotFoundError: Boolean = false,
    val newContactSearchDialogShown: Boolean = false,
    val newContactAddressInput: String = "",
    val query: String = "",
    val refreshing: Boolean = false,
    val undoDelete: Int? = null,
    val existingContactFound: PublicUserData? = null,
    val screen: HomeScreen = HomeScreen.Inbox,
)

data class SnackBarData(val titleResId: Int)

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
        placeholderDescriptionResId = R.string.downloaded_attachments_placeholder
    ),
    Trash(
        R.string.trash,
        iconResId = R.drawable.delete,
        outbox = false,
        placeholderDescriptionResId = R.string.trash_folder_placeholder
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

