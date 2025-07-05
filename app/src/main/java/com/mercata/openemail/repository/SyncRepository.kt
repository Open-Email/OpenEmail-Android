package com.mercata.openemail.repository

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.db.archive.toArchive
import com.mercata.openemail.db.messages.toDraftWithReaders
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.SoundPlayer
import com.mercata.openemail.utils.getNewNotifications
import com.mercata.openemail.utils.rewokeOutboxMessage
import com.mercata.openemail.utils.syncAllMessages
import com.mercata.openemail.utils.syncContacts
import com.mercata.openemail.utils.uploadPendingMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncRepository(
    private val context: Context,
    private val soundPlayer: SoundPlayer,
) {
    private val _sendingState = MutableSharedFlow<Boolean>()
    val sendingState: SharedFlow<Boolean> = _sendingState

    private val _refreshing = MutableSharedFlow<Boolean>()
    val refreshing: SharedFlow<Boolean> = _refreshing

    init {

    }

    class SyncWorker(
        appContext: Context,
        params: WorkerParameters,

        ) : CoroutineWorker(appContext, params), KoinComponent {
        private val sp: SharedPreferences by inject()
        private val db: AppDatabase by inject()
        private val fu: FileUtils by inject()
        private val dl: DownloadRepository by inject()

        override suspend fun doWork(): Result {
            withContext(Dispatchers.IO) {
                val currentUser = sp.getUserData()!!

                listOf(
                    launch {
                        syncContacts(sp, db)
                        uploadPendingMessages(currentUser, db, fu, sp)
                        getNewNotifications(sp, db)
                        syncAllMessages(db, sp, dl)
                    },
                    launch {
                        syncDeletedMessages()
                    },
                    launch {
                        dl.getCachedAttachments()
                    },
                ).joinAll()
            }

            return Result.success()
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
                                fileUtils = fu,
                                db = db
                            ) ?: return@launch
                            rewokeOutboxMessage(currentUser, message.message)
                            db.draftDao().insert(draft.draft)
                            db.draftReaderDao().insertAll(draft.readers)
                        } else {
                            db.archiveDao().insert(message.toArchive())
                            db.archiveAttachmentsDao()
                                .insertAll(message.attachmentParts.map { it.toArchive() })
                        }
                        db.messagesDao().delete(message.message)
                    }
                }
            }
        }
    }

    suspend fun sync(isUploadingNewMessage: Boolean = false) {

        if (isUploadingNewMessage) {
            soundPlayer.playSwoosh()
        }

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)

        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(request.id)
            .asFlow()
            .collect { info ->
                when (info?.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING -> {
                        if (isUploadingNewMessage) {
                            _sendingState.emit(true)
                        }
                        _refreshing.emit(true)

                    }

                    else -> {
                        if (isUploadingNewMessage) {
                            _sendingState.emit(false)
                        }
                        _refreshing.emit(false)
                    }
                }
            }
    }


}