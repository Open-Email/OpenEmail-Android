package com.mercata.pingworks.sign_in

import androidx.lifecycle.viewModelScope
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.EncryptionKeys
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.SigningKeys
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.loginCall
import com.mercata.pingworks.utils.safeApiCall
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class SignInViewModel : AbstractViewModel<SignInState>(SignInState()) {

    private var currentUser: UserData? = null

    init {
        val sharedPreferences: SharedPreferences by inject()
        currentUser = sharedPreferences.getUserData()
        if (!currentUser?.address.isNullOrBlank()) {
            updateState(
                currentState.copy(
                    emailInput = currentUser?.address ?: "",
                    signInButtonActive = true
                )
            )
            if (sharedPreferences.isAutologin()) {
                if (sharedPreferences.isBiometry()) {
                    updateState(currentState.copy(biometryShown = true))
                } else {
                    authenticateWithKeys()
                }
            }
            viewModelScope.launch {
                updateState(currentState.copy(loading = true))
                when(val call = safeApiCall { getProfilePublicData(currentUser!!.address) }) {
                    is HttpResult.Error -> updateState(currentState.copy(currentUserPublic = null))
                    is HttpResult.Success -> updateState(currentState.copy(currentUserPublic = call.data))
                }
                updateState(currentState.copy(loading = false))
            }
        }
    }

    fun biometryPassed() {
        updateState(currentState.copy(biometryShown = false))
        authenticateWithKeys()
    }

    fun biometryCanceled() {
        updateState(currentState.copy(biometryShown = false))
    }

    fun signInClicked(onNewUser: () -> Unit) {
        if (!emailValid()) {
            updateState(currentState.copy(emailErrorResId = R.string.invalid_email))
            return
        }
        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            when (val call =
                safeApiCall { getProfilePublicData(currentState.emailInput) }) {
                is HttpResult.Success -> {
                    if (call.data != null) {
                        updateState(currentState.copy(currentUserPublic = call.data))
                        if (sp.getUserAddress() == currentState.emailInput && sp.isBiometry()) {
                            updateState(currentState.copy(biometryShown = true))
                        } else {
                            onNewUser()
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
                signInButtonActive = str.isNotBlank()
            )
        )
    }

    fun clearError() {
        updateState(currentState.copy(registrationError = null))
    }

    fun openManualEmailInput() {
        updateState(currentState.copy(currentUserPublic = null))
    }

    private fun authenticateWithKeys() {
        val encryptionKey = currentUser!!.encryptionKeys.pair.secretKey
        val signingKey = currentUser!!.signingKeys.pair.secretKey

        viewModelScope.launch {
            updateState(currentState.copy(loading = true))
            val publicData: PublicUserData? =
                when (val call = safeApiCall { getProfilePublicData(currentState.emailInput) }) {
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
                address = currentState.emailInput,
                encryptionKeys = EncryptionKeys(
                    pair = KeyPair(
                        Key.fromBase64String(publicData.publicEncryptionKey),
                        encryptionKey
                    ),
                    id = publicData.encryptionKeyId
                ),
                signingKeys = SigningKeys(
                    pair = KeyPair(
                        Key.fromBase64String(publicData.publicSigningKey),
                        signingKey
                    )
                )
            )

            when (val call = safeApiCall { loginCall(userData) }) {
                is HttpResult.Error -> {
                    updateState(currentState.copy(registrationError = call.message))
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

data class SignInState(
    val emailInput: String = "",
    val currentUserPublic: PublicUserData? = null,
    val registrationError: String? = null,
    val biometryShown: Boolean = false,
    val isLoggedIn: Boolean = false,
    val emailErrorResId: Int? = null,
    val signInButtonActive: Boolean = false,
    val loading: Boolean = false
)