package com.mercata.pingworks.settings_screen

import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.BioManager
import com.mercata.pingworks.SharedPreferences
import org.koin.core.component.inject

class SettingsViewModel : AbstractViewModel<SettingsState>(SettingsState()) {

    init {
        val sharedPreferences: SharedPreferences by inject()
        val bioManager: BioManager by inject()
        val keys = sharedPreferences.getUserPrivateKeys()
        updateState(
            currentState.copy(
                privateEncryptionKey = keys?.privateEncryptionKey?.toString(),
                privateSigningKey = keys?.privateSigningKey?.toString(),
                biometryAvailable = bioManager.isBiometricAvailable(),
                biometryEnabled = sharedPreferences.isBiometry(),
                autologinEnabled = sharedPreferences.isAutologin()
            )
        )
    }

    fun toggleBiometry(isEnabled: Boolean) {
        sharedPreferences.setBiometry(isEnabled)
        updateState(currentState.copy(biometryEnabled = sharedPreferences.isBiometry()))
    }

    fun toggleAutologin(isEnabled: Boolean) {
        sharedPreferences.setAutologin(isEnabled)
        updateState(currentState.copy(autologinEnabled = sharedPreferences.isAutologin()))
    }
}

data class SettingsState(
    val privateEncryptionKey: String? = null,
    val privateSigningKey: String? = null,
    val biometryAvailable: Boolean = false,
    val biometryEnabled: Boolean = false,
    val autologinEnabled: Boolean = false
)