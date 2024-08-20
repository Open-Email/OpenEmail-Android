package com.mercata.pingworks.registration

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.isAddressAvailable
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
            val available: Boolean = isAddressAvailable(
                hostname = "ping.works",
                localName = currentState.usernameInput.trim().lowercase()
            )
            if (available) {
                //TODO registration flow
                updateState(currentState.copy(isLoading = false))
            } else {
                updateState(currentState.copy(isLoading = false, userNameError = true))
            }
        }
    }
}

data class RegistrationState(
    val usernameInput: String = "",
    val fullNameInput: String = "",
    val userNameError: Boolean = false,
    val fullNameError: Boolean = false,
    val isLoading: Boolean = false,
)