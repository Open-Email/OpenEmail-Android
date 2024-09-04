package com.mercata.pingworks.message_details

import androidx.lifecycle.SavedStateHandle
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.models.Envelope
import java.time.ZonedDateTime

class MessageDetailsViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<MessageDetailsState>(
        MessageDetailsState(message = null,
            //TODO
           // messageId = savedStateHandle.get<String>("messageId")!!
        )
    ) {

    init {
        //TODO DB request for message by id

    }
}

data class MessageDetailsState(val message: Envelope?, val messageId: String = "")