package com.mercata.pingworks.settings_screen

import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.utils.BioManager
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.encodeToBase64
import org.koin.core.component.inject

class SettingsViewModel : AbstractViewModel<SettingsState>(SettingsState()) {

    init {
        val sharedPreferences: SharedPreferences by inject()
        val bioManager: BioManager by inject()
        val userData = sharedPreferences.getUserData()!!
        updateState(
            currentState.copy(
                address = userData.address,
                privateEncryptionKey =userData.encryptionKeys.pair.secretKey.asBytes.encodeToBase64(),
                privateSigningKey = userData.signingKeys.pair.secretKey.asBytes.encodeToBase64(),
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

    fun logout() {
        toggleAutologin(false)
    }
}

data class SettingsState(
    val privateEncryptionKey: String? = null,
    val privateSigningKey: String? = null,
    val address: String? = null,
    val biometryAvailable: Boolean = false,
    val biometryEnabled: Boolean = false,
    val autologinEnabled: Boolean = false
)