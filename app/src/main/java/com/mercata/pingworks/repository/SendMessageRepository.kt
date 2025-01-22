package com.mercata.pingworks.repository

import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.drafts.draft_reader.toPublicUserData
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.SoundPlayer
import com.mercata.pingworks.utils.revokeMarkedOutboxMessages
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.uploadMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SendMessageRepository(
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

    @OptIn(DelicateCoroutinesApi::class)
    fun revokeMarkedMessages() {
        GlobalScope.launch(Dispatchers.IO) {
            revokeMarkedOutboxMessages(sp.getUserData()!!, db.messagesDao())
        }
    }
}