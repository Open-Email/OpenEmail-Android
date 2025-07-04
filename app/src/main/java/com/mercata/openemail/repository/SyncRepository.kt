package com.mercata.openemail.repository

import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.archive.toArchive
import com.mercata.openemail.db.messages.toDraftWithReaders
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.NotificationsResult
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.SoundPlayer
import com.mercata.openemail.utils.getNewNotifications
import com.mercata.openemail.utils.rewokeOutboxMessage
import com.mercata.openemail.utils.syncAllMessages
import com.mercata.openemail.utils.syncContacts
import com.mercata.openemail.utils.uploadPendingMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class SyncRepository(
    private val soundPlayer: SoundPlayer,
    private val db: AppDatabase,
    private val fileUtils: FileUtils,
    private val sp: SharedPreferences,
    private val dl: DownloadRepository,
    private val fu: FileUtils
) {
    private val _sendingState = MutableSharedFlow<Boolean>()
    val sendingState: SharedFlow<Boolean> = _sendingState
    private val _notificationsFlow: MutableSharedFlow<NotificationsResult> = MutableSharedFlow()
    val notificationsFlow: SharedFlow<NotificationsResult> = _notificationsFlow

    fun sync(isUploadingNewMessage: Boolean = false) {
        GlobalScope.launch(Dispatchers.IO) {
            if (isUploadingNewMessage) {
                soundPlayer.playSwoosh()
                _sendingState.emit(true)
            }
            val currentUser = sp.getUserData()!!
            listOf(
                launch {
                    syncContacts(sp, db.userDao())
                    uploadPendingMessages(currentUser, db, fu, sp)
                    getNewNotifications(sp, db)?.let { notificationsResult ->
                        _notificationsFlow.emit(notificationsResult)
                    }
                    syncAllMessages(db, sp, dl)
                },
                launch {
                    syncDeletedMessages()
                },
                launch {
                    dl.getCachedAttachments()
                },
            ).joinAll()
            if (isUploadingNewMessage) {
                _sendingState.emit(false)
            }
        }
    }

   private suspend fun syncDeletedMessages() {
        val currentUser = sp.getUserData() ?: return
        supervisorScope {
            db.messagesDao().getAllMarkedToDelete().forEach { message ->
                launch {
                    if (message.author?.address == sp.getUserAddress()) {
                        val draft = message.toDraftWithReaders(
                            downloadRepository = dl,
                            sp = sp,
                            fileUtils = fileUtils,
                            db = db
                        ) ?: return@launch
                        rewokeOutboxMessage(currentUser, message.message)
                        db.draftDao().insert(draft.draft)
                        db.draftReaderDao().insertAll(draft.readers)
                    } else {
                        db.archiveDao().insert(message.toArchive())
                        db.archiveAttachmentsDao().insertAll(message.attachmentParts.map { it.toArchive() })
                    }
                    db.messagesDao().delete(message.message)
                }
            }
        }
    }
}