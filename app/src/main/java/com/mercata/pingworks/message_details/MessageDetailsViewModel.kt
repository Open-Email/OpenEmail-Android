package com.mercata.pingworks.message_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.SharedPreferences
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class MessageDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<MessageDetailsState>(
        MessageDetailsState(
            messageId = savedStateHandle.get<String>("messageId")!!
        )
    ) {

    init {
        val db: AppDatabase by inject()
        val sp: SharedPreferences by inject()
        viewModelScope.launch {
            updateState(
                currentState.copy(
                    message = db.messagesDao().getById(currentState.messageId),
                    currentUser = sp.getUserData()
                )
            )
        }
    }
}

data class MessageDetailsState(
    val messageId: String,
    val message: DBMessageWithDBAttachments? = null,
    val currentUser: UserData? = null
)