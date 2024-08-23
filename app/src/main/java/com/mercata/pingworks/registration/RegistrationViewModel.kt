package com.mercata.pingworks.registration

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.DEFAULT_MAIL_SUBDOMAIN
import com.mercata.pingworks.EncryptionKeys
import com.mercata.pingworks.SigningKeys
import com.mercata.pingworks.availableHosts
import com.mercata.pingworks.generateEncryptionKeys
import com.mercata.pingworks.generateSigningKeys
import com.mercata.pingworks.getHost
import com.mercata.pingworks.isAddressAvailable
import com.mercata.pingworks.registerCall
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
            val available: Boolean = isAddressAvailable(
                hostname = currentState.selectedHostName,
                localName = localName
            )
            if (available) {
                val user = UserData(
                    name = currentState.fullNameInput,
                    address = "${currentState.usernameInput}@${currentState.selectedHostName}",
                    encryptionKeys = generateEncryptionKeys(),
                    signingKeys = generateSigningKeys()
                )
                val error: String? = registerCall(user)
                if (error == null) {
                    sharedPreferences.saveUserPrivateKeys(user)
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
) {
    fun getHost() = "$DEFAULT_MAIL_SUBDOMAIN.${address.getHost()}"
}