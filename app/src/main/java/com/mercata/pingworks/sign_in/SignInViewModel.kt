package com.mercata.pingworks.sign_in

import androidx.lifecycle.viewModelScope
import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.EncryptionKeys
import com.mercata.pingworks.HttpResult
import com.mercata.pingworks.PrivateKey
import com.mercata.pingworks.PublicKey
import com.mercata.pingworks.R
import com.mercata.pingworks.SharedPreferences
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
import org.koin.core.component.inject

class SignInViewModel : AbstractViewModel<SignInState>(SignInState()) {

    init {
        val sharedPreferences: SharedPreferences by inject()
        val address = sharedPreferences.getUserAddress()
        if (!address.isNullOrBlank()) {
            updateState(currentState.copy(emailInput = address, signInButtonActive = true))
            if (sharedPreferences.isAutologin()) {
                if (sharedPreferences.isBiometry()) {
                    updateState(currentState.copy(biometryShown = true))
                } else {
                    val currentUser = sharedPreferences.getUserData()!!
                    updateState(
                        currentState.copy(
                            privateSigningKeyInput = currentUser.signingKeys.privateKey.toString(),
                            privateEncryptionKeyInput = currentUser.encryptionKeys.privateKey.toString()
                        )
                    )
                    authenticateWithKeys()
                }
            }
        }
    }

    fun biometryPassed() {
        val currentUser = sharedPreferences.getUserData()!!
        updateState(
            currentState.copy(
                biometryShown = false,
                privateSigningKeyInput = currentUser.signingKeys.privateKey.toString(),
                privateEncryptionKeyInput = currentUser.encryptionKeys.privateKey.toString()
            )
        )
        authenticateWithKeys()
    }

    fun biometryCanceled() {
        updateState(
            currentState.copy(
                biometryShown = false,
                keysInputOpen = true
            )
        )
    }

    fun signInClicked() {
        if (!emailValid()) {
            updateState(currentState.copy(emailErrorResId = R.string.invalid_email))
            return
        }
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            when (val call = safeApiCall { getWellKnownHosts(currentState.emailInput.getHost()) }) {
                is HttpResult.Success -> {
                    if (call.data?.isNotEmpty() == true) {
                        if (sharedPreferences.getUserAddress() == currentState.emailInput && sharedPreferences.isBiometry()) {
                            updateState(currentState.copy(biometryShown = true))
                        } else {
                            updateState(currentState.copy(keysInputOpen = true))
                        }
                    } else {
                        updateState(currentState.copy(emailErrorResId = R.string.no_account_error))
                    }
                }

                is HttpResult.Error -> {
                    updateState(currentState.copy(emailErrorResId = R.string.no_account_error))
                }
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

    fun clearError() {
        updateState(currentState.copy(registrationError = null))
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
                    updateState(currentState.copy(registrationError = "${call.code}: ${call.message}"))
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

            val error: String? = when (val call = safeApiCall { loginCall(userData) }) {
                is HttpResult.Error -> call.message
                is HttpResult.Success -> null
            }

            if (error == null) {
                sharedPreferences.saveUserKeys(userData)
                updateState(currentState.copy(isLoggedIn = true))
            } else {
                updateState(currentState.copy(registrationError = error))
            }

            updateState(currentState.copy(loading = false))
        }
    }
}

data class SignInState(
    val emailInput: String = "",
    val privateEncryptionKeyInput: String = "",
    val privateSigningKeyInput: String = "",
    val registrationError: String? = null,
    val keysInputOpen: Boolean = false,
    val biometryShown: Boolean = false,
    val isLoggedIn: Boolean = false,
    val authenticateButtonVisible: Boolean = false,
    val authenticateButtonEnabled: Boolean = false,
    val emailErrorResId: Int? = null,
    val signInButtonActive: Boolean = false,
    val loading: Boolean = false
)