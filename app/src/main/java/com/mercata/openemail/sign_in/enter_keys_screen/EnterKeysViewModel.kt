package com.mercata.openemail.sign_in.enter_keys_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.registration.UserData
import com.mercata.openemail.utils.EncryptionKeys
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.SigningKeys
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.loginCall
import com.mercata.openemail.utils.safeApiCall
import kotlinx.coroutines.launch

class EnterKeysViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<EnterKeysState>(EnterKeysState(address = savedStateHandle.get<String>("address")!!)) {

    init {
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val publicUserData: PublicUserData? =
                when (val call = safeApiCall { getProfilePublicData(currentState.address) }) {
                    is HttpResult.Error -> null
                    is HttpResult.Success -> call.data
                }
            updateState(currentState.copy(publicUserData = publicUserData, loading = false))
        }
    }

    fun onPrivateEncryptionKeyInput(str: String) {
        updateState(
            currentState.copy(privateEncryptionKeyInput = str)
        )

        updateAuthButton()
    }

    fun onPrivateSigningKeyInput(str: String) {
        updateState(
            currentState.copy(privateSigningKeyInput = str)
        )
        updateAuthButton()
    }

    private fun updateAuthButton() {
        val enabled = currentState.privateSigningKeyInput.isNotBlank()
                && currentState.privateEncryptionKeyInput.isNotBlank()
        updateState(
            currentState.copy(
                authenticateButtonEnabled = enabled
            )
        )
    }

    fun clearError() {
        updateState(currentState.copy(registrationError = null))
    }

    fun authenticateWithKeys() {
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val publicData: PublicUserData? =
                when (val call = safeApiCall { getProfilePublicData(currentState.address) }) {
                    is HttpResult.Success -> {
                        call.data
                    }

                    is HttpResult.Error -> {
                        updateState(currentState.copy(registrationError = "${call.code}: ${call.message}"))
                        null
                    }
                }

            if (publicData == null) {
                updateState(currentState.copy(loading = false))
                return@launch
            }

            val userData = UserData(
                name = publicData.fullName,
                address = currentState.address,
                encryptionKeys = EncryptionKeys(
                    pair = KeyPair(
                        Key.fromBase64String(publicData.publicEncryptionKey),
                        Key.fromBase64String(currentState.privateEncryptionKeyInput.trim())
                    ),
                    id = publicData.encryptionKeyId
                ),
                signingKeys = SigningKeys(
                    pair = KeyPair(
                        Key.fromBase64String(publicData.publicSigningKey),
                        Key.fromBase64String(currentState.privateSigningKeyInput.trim())
                    )
                )
            )

            when (val call = safeApiCall { loginCall(userData) }) {
                is HttpResult.Error -> {
                    updateState(currentState.copy(registrationError = "${call.code}: ${call.message}"))
                }

                is HttpResult.Success -> {
                    sp.saveUserKeys(userData)
                    updateState(currentState.copy(isLoggedIn = true))
                }
            }

            updateState(currentState.copy(loading = false))
        }
    }

    fun parseScannedKeys(keys: String) {
        val split = keys.split(":")
        val encryptionKey = split.firstOrNull() ?: return
        val signingKey = split.getOrNull(1) ?: return
        updateState(currentState.copy(privateEncryptionKeyInput = encryptionKey, privateSigningKeyInput = signingKey))
        authenticateWithKeys()
    }
}

data class EnterKeysState(
    val loading: Boolean = false,
    val publicUserData: PublicUserData? = null,
    val authenticateButtonEnabled: Boolean = false,
    val isLoggedIn: Boolean = false,
    val privateEncryptionKeyInput: String = "",
    val privateSigningKeyInput: String = "",
    val address: String,
    val registrationError: String? = null,
)