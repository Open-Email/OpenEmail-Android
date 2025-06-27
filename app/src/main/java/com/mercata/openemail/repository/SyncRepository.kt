package com.mercata.openemail.repository

import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.archive.toArchive
import com.mercata.openemail.db.drafts.draft_reader.toPublicUserData
import com.mercata.openemail.db.messages.toDraftWithReaders
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.SoundPlayer
import com.mercata.openemail.utils.rewokeOutboxMessage
import com.mercata.openemail.utils.syncAllMessages
import com.mercata.openemail.utils.uploadMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class SyncRepository(
    private val soundPlayer: SoundPlayer,
    private val db: AppDatabase,
    private val fileUtils: FileUtils,
    private val sp: SharedPreferences,
    private val dl: DownloadRepository
) {

    private val _sendingState = MutableSharedFlow<Boolean>()
    val sendingState: SharedFlow<Boolean> = _sendingState

    @OptIn(DelicateCoroutinesApi::class)
    fun send(draftId: String, isBroadcast: Boolean, replyToSubjectId: String?) {
        soundPlayer.playSwoosh()
        GlobalScope.launch(Dispatchers.IO) {
            _sendingState.emit(true)
            val draftWithRecipients = db.draftDao().getById(draftId)!!
            uploadMessage(
                draft = draftWithRecipients.draft,
                recipients = draftWithRecipients.readers.map { it.toPublicUserData() },
                fileUtils = fileUtils,
                currentUser = sp.getUserData()!!,
                db = db,
                isBroadcast = isBroadcast,
                replyToSubjectId = replyToSubjectId,
                sp = sp
            )
            db.draftDao().delete(draftId)
            _sendingState.emit(false)
            syncAllMessages(db, sp, dl)
        }
    }

    suspend fun syncDeletedMessages() {
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