package com.mercata.pingworks.common

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.SharedPreferences
import org.koin.core.component.inject

class NavigationDrawerViewModel :
    AbstractViewModel<NavigationDrawerState>(NavigationDrawerState()) {

    init {
        val sharedPreferences: SharedPreferences by inject()
        updateState(currentState.copy(currentScreenName = sharedPreferences.getSelectedNavigationScreenName()))
    }

    fun selectPage(pageName: String) {
        updateState(currentState.copy(currentScreenName = pageName))
        saveScreenSelection(pageName)
    }

    private fun saveScreenSelection(screenName: String) {
        sharedPreferences.saveSelectedNavigationScreenName(screenName)
    }
}

data class NavigationDrawerState(
    val currentScreenName: String = "InboxListScreen",
    val broadcastUnreadCount: Int = 0,
    val inboxUnreadCount: Int = 0,
    val outboxUnreadCount: Int = 0,
    val drawerState: DrawerState = DrawerState(initialValue = DrawerValue.Closed)
)