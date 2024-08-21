package com.mercata.pingworks.sign_in

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.HttpResult
import com.mercata.pingworks.R
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.getHost
import com.mercata.pingworks.getWellKnownHosts
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
            currentState.copy(
                privateEncryptionKeyInput = str,
                authenticateButtonEnabled = currentState.privateSigningKeyInput.isNotBlank()
                        && currentState.privateEncryptionKeyInput.isNotBlank()
            )
        )
    }

    fun onPrivateSigningKeyInput(str: String) {
        updateState(
            currentState.copy(
                privateSigningKeyInput = str,
                authenticateButtonEnabled = currentState.privateSigningKeyInput.isNotBlank()
                        && currentState.privateEncryptionKeyInput.isNotBlank()
            )
        )
    }

    fun openInputKeys() {
        updateState(currentState.copy(keysInputOpen = true))
    }

    fun authenticateWithKeys() {
        val encryptionKey = currentState.privateEncryptionKeyInput.trim().replace("\n", "")
        val signingKey = currentState.privateSigningKeyInput.trim().replace("\n", "")
        //TODO login

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