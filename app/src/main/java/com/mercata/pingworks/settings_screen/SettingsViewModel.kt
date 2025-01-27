package com.mercata.pingworks.settings_screen

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.utils.BioManager
import com.mercata.pingworks.utils.DownloadRepository
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.encodeToBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class SettingsViewModel : AbstractViewModel<SettingsState>(SettingsState()) {

    private val dl: DownloadRepository by inject()

    init {
        val sharedPreferences: SharedPreferences by inject()
        val bioManager: BioManager by inject()
        val userData = sharedPreferences.getUserData()!!
        updateState(
            currentState.copy(
                address = userData.address,
                privateEncryptionKey = userData.encryptionKeys.pair.secretKey.asBytes.encodeToBase64(),
                privateSigningKey = userData.signingKeys.pair.secretKey.asBytes.encodeToBase64(),
                publicEncryptionKey = userData.encryptionKeys.pair.publicKey.asBytes.encodeToBase64(),
                publicSigningKey = userData.signingKeys.pair.publicKey.asBytes.encodeToBase64(),
                biometryAvailable = bioManager.isBiometricAvailable(),
                biometryEnabled = sharedPreferences.isBiometry(),
                autologinEnabled = sharedPreferences.isAutologin()
            )
        )
    }

    fun toggleBiometry(isEnabled: Boolean) {
        sp.setBiometry(isEnabled)
        updateState(currentState.copy(biometryEnabled = sp.isBiometry()))
    }

    fun toggleAutologin(isEnabled: Boolean) {
        sp.setAutologin(isEnabled)
        updateState(currentState.copy(autologinEnabled = sp.isAutologin()))
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            listOf(
                launch { db.userDao().deleteAll() },
                launch { db.messagesDao().deleteAll() },
                launch { db.attachmentsDao().deleteAll() },
                launch { db.draftDao().deleteAll() },
                launch { db.draftReaderDao().deleteAll() },
                launch { db.archiveDao().deleteAll() },
                launch { db.archiveReadersDao().deleteAll() },
                launch { db.notificationsDao().deleteAll() },
                launch { db.pendingMessagesDao().deleteAll() },
                launch { db.pendingAttachmentsDao().deleteAll() },
                launch { db.pendingReadersDao().deleteAll() },
                launch { dl.clearAllCachedAttachments() },
            ).joinAll()
        }.invokeOnCompletion {
            viewModelScope.launch(Dispatchers.Main) {
                toggleAutologin(false)
                onComplete()
            }
        }
    }
}

data class SettingsState(
    val privateEncryptionKey: String? = null,
    val privateSigningKey: String? = null,
    val publicEncryptionKey: String? = null,
    val publicSigningKey: String? = null,
    val address: String? = null,
    val biometryAvailable: Boolean = false,
    val biometryEnabled: Boolean = false,
    val autologinEnabled: Boolean = false
)