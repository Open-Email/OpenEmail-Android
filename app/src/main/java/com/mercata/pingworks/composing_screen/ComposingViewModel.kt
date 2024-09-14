package com.mercata.pingworks.composing_screen

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.Address
import com.mercata.pingworks.utils.SharedPreferences
import org.koin.core.component.inject

class ComposingViewModel(savedStateHandle: SavedStateHandle) :
    AbstractViewModel<ComposingState>(
        ComposingState(
            to = savedStateHandle.get<String>("contactAddress") ?: ""
        )
    ) {

    init {
        val sp: SharedPreferences by inject()
        updateState(currentState.copy(currentUser = sp.getUserData()))
    }

    fun updateTo(str: String) {
        updateState(currentState.copy(to = str))
    }

    fun updateSubject(str: String) {
        updateState(currentState.copy(subject = str))
    }

    fun toggleBroadcast() {
        updateState(currentState.copy(broadcast = !currentState.broadcast))
    }

    fun updateBody(str: String) {
        updateState(currentState.copy(body = str))
    }
}

data class ComposingState(
    val subject: String = "",
    val body: String = "",
    val to: Address = "",
    val broadcast: Boolean = false,
    val currentUser: UserData? = null,
    val attachments: SnapshotStateList<Uri> = mutableStateListOf()
)