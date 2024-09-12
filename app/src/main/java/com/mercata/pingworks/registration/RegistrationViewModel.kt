package com.mercata.pingworks.registration

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.utils.EncryptionKeys
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SigningKeys
import com.mercata.pingworks.availableHosts
import com.mercata.pingworks.utils.generateEncryptionKeys
import com.mercata.pingworks.utils.generateSigningKeys
import com.mercata.pingworks.utils.isAddressAvailable
import com.mercata.pingworks.utils.registerCall
import com.mercata.pingworks.utils.safeApiCall
import kotlinx.coroutines.launch


class RegistrationViewModel : AbstractViewModel<RegistrationState>(RegistrationState()) {

    fun onUsernameChange(str: String) {
        updateState(currentState.copy(usernameInput = str))
    }

    fun onFullNameEdit(str: String) {
        updateState(currentState.copy(fullNameInput = str))
    }

    fun register() {
        viewModelScope.launch {
            updateState(currentState.copy(isLoading = true))
            val localName = currentState.usernameInput.trim().lowercase()
            val address = "$localName@${currentState.selectedHostName}"
            val available: Boolean = when (safeApiCall {
                isAddressAvailable(address)
            }) {
                is HttpResult.Error -> false
                is HttpResult.Success -> true
            }
            if (available) {
                val user = UserData(
                    name = currentState.fullNameInput,
                    address = address,
                    encryptionKeys = generateEncryptionKeys(),
                    signingKeys = generateSigningKeys()
                )
                val error: String? = when (val call = safeApiCall { registerCall(user) }) {
                    is HttpResult.Error -> call.message
                    is HttpResult.Success -> null
                }
                if (error == null) {
                    sharedPreferences.saveUserKeys(user)
                    updateState(currentState.copy(isRegistered = true))
                } else {
                    updateState(currentState.copy(registrationError = error))
                }
                updateState(currentState.copy(isLoading = false))
            } else {
                updateState(currentState.copy(isLoading = false, userNameError = true))
            }
        }
    }

    fun selectHostName(hostName: String) {
        updateState(currentState.copy(selectedHostName = hostName))
    }

    fun clearError() {
        updateState(currentState.copy(registrationError = null))
    }
}

data class RegistrationState(
    val usernameInput: String = "",
    val fullNameInput: String = "",
    val registrationError: String? = null,
    val userNameError: Boolean = false,
    val fullNameError: Boolean = false,
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val hostnames: List<String> = availableHosts,
    val selectedHostName: String = hostnames.first()
)

data class UserData(
    val name: String,
    val address: String,
    val encryptionKeys: EncryptionKeys,
    val signingKeys: SigningKeys,
)