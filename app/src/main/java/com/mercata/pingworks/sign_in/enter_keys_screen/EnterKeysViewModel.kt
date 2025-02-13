package com.mercata.pingworks.sign_in.enter_keys_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.EncryptionKeys
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SigningKeys
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.loginCall
import com.mercata.pingworks.utils.safeApiCall
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
                        Key.fromBase64String(currentState.privateEncryptionKeyInput)
                    ),
                    id = publicData.encryptionKeyId
                ),
                signingKeys = SigningKeys(
                    pair = KeyPair(
                        Key.fromBase64String(publicData.publicSigningKey),
                        Key.fromBase64String(currentState.privateSigningKeyInput)
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