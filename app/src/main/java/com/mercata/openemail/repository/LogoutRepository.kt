package com.mercata.openemail.repository

import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.exceptions.LoginCallError
import com.mercata.openemail.exceptions.NoUserSaved
import com.mercata.openemail.exceptions.UserDoesNotExist
import com.mercata.openemail.utils.DownloadRepository
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.loginCall
import com.mercata.openemail.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogoutRepository(
    private val sp: SharedPreferences,
    private val db: AppDatabase,
    private val dl: DownloadRepository
) {

    private val _logout = MutableSharedFlow<Boolean>()
    val logout: SharedFlow<Boolean> = _logout

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            listOf(
                launch { db.userDao().deleteAll() },
                launch { db.messagesDao().deleteAll() },
                launch { db.attachmentsDao().deleteAll() },
                launch { db.draftDao().deleteAll() },
                launch { db.draftReaderDao().deleteAll() },
                launch { db.archiveDao().deleteAll() },
                launch { db.archiveAttachmentsDao().deleteAll() },
                launch { db.notificationsDao().deleteAll() },
                launch { db.pendingMessagesDao().deleteAll() },
                launch { db.pendingAttachmentsDao().deleteAll() },
                launch { db.pendingReadersDao().deleteAll() },
                launch { dl.clearAllCachedAttachments() },
            ).joinAll()
            sp.clear()
            _logout.emit(true)
        }
    }

    @Throws
    suspend fun tryCurrentLogin() {
        withContext(Dispatchers.IO) {
            val currentUser = sp.getUserData() ?: throw NoUserSaved("No user saved in SP")

            when (val call = safeApiCall { getProfilePublicData(currentUser.address) }) {
                is HttpResult.Success -> {
                    call.data ?: run {
                        throw UserDoesNotExist("No User saved on server with current address: ${currentUser.address}")
                    }
                }

                is HttpResult.Error -> {
                    throw UserDoesNotExist("No User saved on server with current address: ${currentUser.address}")
                }
            }

            when (val call = safeApiCall { loginCall(currentUser) }) {
                is HttpResult.Error -> {
                    throw LoginCallError(call.message ?: "login call error")
                }

                is HttpResult.Success -> {
                    //ignore. Continue current flow
                }
            }
        }
    }
}