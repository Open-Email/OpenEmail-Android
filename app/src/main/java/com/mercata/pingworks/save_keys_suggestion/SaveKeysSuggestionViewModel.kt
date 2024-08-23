package com.mercata.pingworks.save_keys_suggestion

import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.BioManager
import com.mercata.pingworks.SharedPreferences
import org.koin.core.component.inject

class SaveKeysSuggestionViewModel :
    AbstractViewModel<SaveKeysSuggestionState>(SaveKeysSuggestionState()) {

    init {
        val bioManager: BioManager by inject()
        val sharedPreferences: SharedPreferences by inject()
        val keys = sharedPreferences.getUserPrivateKeys()!!
        updateState(
            currentState.copy(
                biometryAvailable = bioManager.isBiometricAvailable(),
                privateEncryptionKey = keys.privateEncryptionKey.toString(),
                privateSigningKey = keys.privateSigningKey.toString()
            )
        )
    }

    fun autologinToggle(isEnabled: Boolean) {
        updateState(currentState.copy(autologinEnabled = isEnabled))
    }

    fun biometryToggle(isEnabled: Boolean) {
        updateState(currentState.copy(biometryEnabled = isEnabled))
    }

    fun biometryPassed() {
        sharedPreferences.run {
            setAutologin(currentState.autologinEnabled)
            setBiometry(currentState.biometryEnabled)
        }
        updateState(currentState.copy(navigate = true))
    }

    fun biometryCanceled() {
        updateState(currentState.copy(biometryPrompt = false, biometryEnabled = false))
    }

    fun saveSettings() {
        if (currentState.biometryAvailable) {
            if (currentState.biometryEnabled) {
                updateState(currentState.copy(biometryPrompt = true))
            } else {
                sharedPreferences.setAutologin(currentState.autologinEnabled)
                updateState(currentState.copy(navigate = true))
            }
        } else {
            sharedPreferences.setAutologin(currentState.autologinEnabled)
            updateState(currentState.copy(navigate = true))
        }
    }
}

data class SaveKeysSuggestionState(
    val privateSigningKey: String = "",
    val privateEncryptionKey: String = "",
    val biometryAvailable: Boolean = false,
    val biometryPrompt: Boolean = false,
    val autologinEnabled: Boolean = true,
    val biometryEnabled: Boolean = true,
    val navigate: Boolean = false
)