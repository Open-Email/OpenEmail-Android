package com.mercata.pingworks.sign_in

import androidx.lifecycle.viewModelScope
import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.EncryptionKeys
import com.mercata.pingworks.HttpResult
import com.mercata.pingworks.PrivateKey
import com.mercata.pingworks.PublicKey
import com.mercata.pingworks.R
import com.mercata.pingworks.SigningKeys
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.getHost
import com.mercata.pingworks.getProfilePublicData
import com.mercata.pingworks.getWellKnownHosts
import com.mercata.pingworks.loginCall
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.safeApiCall
import kotlinx.coroutines.launch

class SignInViewModel : AbstractViewModel<SignInState>(SignInState()) {

    init {
        //TODO check autologin
    }


    fun signIn() {
        if (!emailValid()) {
            updateState(currentState.copy(emailErrorResId = R.string.invalid_email))
            return
        }
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val result = getWellKnownHosts(currentState.emailInput.getHost())
            if (result.isNotEmpty()) {
                updateState(currentState.copy(keysInputOpen = true))
            } else {
                updateState(currentState.copy(emailErrorResId = R.string.no_account_error))
            }
            updateState(currentState.copy(loading = false))
        }
    }

    private fun emailValid(): Boolean {
        if (currentState.emailInput.isBlank()) {
            return false
        }

        return currentState.emailInput.lowercase().matches(emailRegex)
    }

    fun onEmailChange(str: String) {
        updateState(
            currentState.copy(
                emailInput = str,
                emailErrorResId = null,
                keysInputOpen = false,
                privateSigningKeyInput = "",
                privateEncryptionKeyInput = "",
                signInButtonActive = str.isNotBlank()
            )
        )
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

    fun openInputKeys() {
        updateState(currentState.copy(keysInputOpen = true))
    }

    fun authenticateWithKeys() {
        val encryptionKey = currentState.privateEncryptionKeyInput.trim().replace("\n", "")
        val signingKey = currentState.privateSigningKeyInput.trim().replace("\n", "")

        viewModelScope.launch {
            var publicData: PublicUserData? = null
            updateState(currentState.copy(loading = true))
            when (val call = safeApiCall { getProfilePublicData(currentState.emailInput) }) {
                is HttpResult.Success -> {
                    publicData = call.data
                }

                is HttpResult.Error -> {
                    println()
                }
            }
            if (publicData == null) {
                updateState(currentState.copy(loading = false))
                return@launch
            }

            val userData = UserData(
                name = publicData.fullName,
                address = currentState.emailInput,
                encryptionKeys = EncryptionKeys(
                    privateKey = PrivateKey(Key.fromBase64String(encryptionKey)),
                    publicKey = PublicKey(Key.fromBase64String(publicData.publicEncryptionKey)),
                    id = publicData.encryptionKeyId
                ),
                signingKeys = SigningKeys(
                    PrivateKey(Key.fromBase64String(signingKey)),
                    publicKey = PublicKey(Key.fromBase64String(publicData.publicSigningKey))
                )
            )

            val error: String? = loginCall(userData)
            if (error == null) {
                //TODO navigate to main screen
            } else {
                //TODO show error dialog
            }

            updateState(currentState.copy(loading = false))
        }
    }
}

data class SignInState(
    val emailInput: String = "",
    val privateEncryptionKeyInput: String = "",
    val privateSigningKeyInput: String = "",
    val keysInputOpen: Boolean = false,
    val authenticateButtonVisible: Boolean = false,
    val authenticateButtonEnabled: Boolean = false,
    val emailErrorResId: Int? = null,
    val signInButtonActive: Boolean = false,
    val loading: Boolean = false
)