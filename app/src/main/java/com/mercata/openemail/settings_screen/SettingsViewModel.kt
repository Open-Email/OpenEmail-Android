package com.mercata.openemail.settings_screen

import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.repository.LogoutRepository
import com.mercata.openemail.utils.BioManager
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.SharedPreferences
import com.mercata.openemail.utils.deleteCurrentUser
import com.mercata.openemail.utils.encodeToBase64
import com.mercata.openemail.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class SettingsViewModel : AbstractViewModel<SettingsState>(SettingsState()) {

    private val logoutRepository: LogoutRepository by inject()

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

    suspend fun logout() {
        logoutRepository.logout()
    }

    suspend fun deleteAccount() {
        withContext(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))
            when(safeApiCall { deleteCurrentUser(sp) }) {
                is HttpResult.Error -> {
                    //ignore
                }
                is HttpResult.Success -> {
                    logout()
                }
            }
            updateState(currentState.copy(loading = false))
        }

    }

    fun toggleLogoutConfirmation() {
        updateState(currentState.copy(logoutConfirmationShown = !currentState.logoutConfirmationShown))
    }

    fun toggleAccountDeletionConfirmation() {
        updateState(currentState.copy(deleteAccountConfirmationShown = !currentState.deleteAccountConfirmationShown))
    }
}

data class SettingsState(
    val privateEncryptionKey: String? = null,
    val privateSigningKey: String? = null,
    val publicEncryptionKey: String? = null,
    val publicSigningKey: String? = null,
    val address: String? = null,
    val loading: Boolean = false,
    val logoutConfirmationShown: Boolean = false,
    val deleteAccountConfirmationShown: Boolean = false,
    val biometryAvailable: Boolean = false,
    val biometryEnabled: Boolean = false,
    val autologinEnabled: Boolean = false
)