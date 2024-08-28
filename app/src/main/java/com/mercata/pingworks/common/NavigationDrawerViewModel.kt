package com.mercata.pingworks.common

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import com.mercata.pingworks.AbstractViewModel

class NavigationDrawerViewModel :
    AbstractViewModel<NavigationDrawerState>(NavigationDrawerState()) {

    init {
        //TODO check persistent storage to open last opened screen
    }

    fun selectPage(pageName: String) {
        updateState(currentState.copy(currentScreenName = pageName))
    }

}

data class NavigationDrawerState(
    val currentScreenName: String = "InboxListScreen",
    val broadcastUnreadCount: Int = 0,
    val inboxUnreadCount: Int = 0,
    val outboxUnreadCount: Int = 0,
    val drawerState: DrawerState = DrawerState(initialValue = DrawerValue.Closed)
)