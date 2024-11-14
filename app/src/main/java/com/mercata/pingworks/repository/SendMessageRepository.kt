package com.mercata.pingworks.repository

import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.drafts.draft_reader.toPublicUserData
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.FileUtils
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.SoundPlayer
import com.mercata.pingworks.utils.syncAllMessages
import com.mercata.pingworks.utils.uploadMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SendMessageRepository(
    private val soundPlayer: SoundPlayer,
    private val db: AppDatabase,
    private val fileUtils: FileUtils,
    private val sp: SharedPreferences,
    private val dl: DownloadRepository
) {

    private val _sendingState = MutableStateFlow(false)
    val sendingState: StateFlow<Boolean> = _sendingState

    @OptIn(DelicateCoroutinesApi::class)
    fun send(draftId: String, isBroadcast: Boolean, replyToSubjectId: String?) {
        soundPlayer.playSwoosh()
        GlobalScope.launch(Dispatchers.IO) {
            _sendingState.value = true
            val draftWithRecipients = db.draftDao().getById(draftId)!!
            uploadMessage(
                draft = draftWithRecipients.draft,
                recipients = draftWithRecipients.readers.map { it.toPublicUserData() },
                fileUtils = fileUtils,
                currentUser = sp.getUserData()!!,
                currentUserPublicData = sp.getPublicUserData()!!,
                db = db,
                isBroadcast = isBroadcast,
                replyToSubjectId = replyToSubjectId,
                sp = sp
            )
            _sendingState.value = false
            syncAllMessages(db, sp, dl)
        }
    }
}