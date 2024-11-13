package com.mercata.pingworks.registration

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.SUPPORT_ADDRESS
import com.mercata.pingworks.availableHosts
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.models.toDBContact
import com.mercata.pingworks.utils.Downloader
import com.mercata.pingworks.utils.EncryptionKeys
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SigningKeys
import com.mercata.pingworks.utils.generateEncryptionKeys
import com.mercata.pingworks.utils.generateSigningKeys
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.isAddressAvailable
import com.mercata.pingworks.utils.registerCall
import com.mercata.pingworks.utils.safeApiCall
import com.mercata.pingworks.utils.syncMessagesForContact
import com.mercata.pingworks.utils.uploadContact
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.inject


class RegistrationViewModel : AbstractViewModel<RegistrationState>(RegistrationState()) {

   private val dl: Downloader by inject()

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
                when (val call = safeApiCall { registerCall(user) }) {
                    is HttpResult.Error -> {
                        updateState(currentState.copy(registrationError = call.message))
                    }

                    is HttpResult.Success -> {
                        sp.saveUserKeys(user)
                        addSupportContactForNewUser()
                        updateState(currentState.copy(isRegistered = true))

                    }
                }
                updateState(currentState.copy(isLoading = false))
            } else {
                updateState(currentState.copy(isLoading = false, userNameError = true))
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun addSupportContactForNewUser() {
        GlobalScope.launch(Dispatchers.IO) {
            val publicData: PublicUserData =
            when (val call = safeApiCall { getProfilePublicData(SUPPORT_ADDRESS) }) {
                is HttpResult.Success -> {
                    call.data
                }

                is HttpResult.Error -> {
                    null
                }
            } ?: return@launch

            val dbContact = publicData.toDBContact()
            db.userDao().insert(dbContact)
            when (safeApiCall {
                uploadContact(
                    contact = publicData,
                    sharedPreferences = sp
                )
            }) {
                is HttpResult.Error -> {
                    db.userDao().delete(dbContact)
                }

                is HttpResult.Success -> {
                    syncMessagesForContact(dbContact, db, sp, dl, true)
                }
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
    val avatarLink: String? = null,
)