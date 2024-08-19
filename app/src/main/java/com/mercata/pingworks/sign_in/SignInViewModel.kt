package com.mercata.pingworks.sign_in

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.HttpResult
import com.mercata.pingworks.emailRegex
import com.mercata.pingworks.getWellKnownHosts
import com.mercata.pingworks.safeApiCall
import kotlinx.coroutines.launch

class SignInViewModel : AbstractViewModel<SignInState>(SignInState()) {

    init {
        //TODO check autologin
    }

    fun onEmailChange(str: String) {
        updateState(
            currentState.copy(
                emailInput = str,
                emailValid = true,
                signInButtonActive = str.isNotBlank()
            )
        )
    }

    fun signIn() {
        if (emailValid()) {
            viewModelScope.launch {
                updateState(currentState.copy(loading = true))
                when (val call = safeApiCall { getWellKnownHosts(getHost()) }) {
                    is HttpResult.Success -> {
                        val knownHosts: List<String> =
                            call.data?.split("\n")
                                ?.filter { it.startsWith("#") || it.isBlank() }
                                ?: listOf()
                        if (knownHosts.isNotEmpty()) {
                            //TODO send LaunchedEffect to open keys input screen
                        }
                    }

                    is HttpResult.Error -> {
                        //TODO send LaunchedEffect to show error dialog
                    }
                }
                updateState(currentState.copy(loading = false))
            }
        } else {
            updateState(currentState.copy(emailValid = false))
        }
    }

    private fun emailValid(): Boolean {
        if (currentState.emailInput.isBlank()) {
            return false
        }

        return currentState.emailInput.lowercase().matches(emailRegex)
    }

    private fun getHost(): String = currentState.emailInput.substringAfter("@")
    private fun getLocal(): String = currentState.emailInput.substringBefore("@")
}

data class SignInState(
    val emailInput: String = "",
    val emailValid: Boolean = true,
    val signInButtonActive: Boolean = false,
    val loading: Boolean = false
)