package com.mercata.pingworks.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MARGIN_SMALLER
import com.mercata.pingworks.R
import kotlinx.coroutines.Job

private val viewModel: NavigationDrawerViewModel = NavigationDrawerViewModel()

@Composable
fun NavigationDrawerBody(
    navController: NavController,
    modifier: Modifier = Modifier,
    onNavigate: () -> Job
) {

    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.padding(12.dp)) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier.padding(MARGIN_DEFAULT),
            fontWeight = FontWeight.Bold
        )
        //TODO parse count
        NavigationItem(
            onClick = {
                navController.popBackStack(state.currentScreenName, inclusive = true)
                viewModel.selectPage("BroadcastListScreen")
                navController.navigate("BroadcastListScreen")
            },
            isSelected = state.currentScreenName == "BroadcastListScreen",
            icon = Icons.Default.Email,
            titleResId = R.string.broadcast_title,
            count = state.broadcastUnreadCount
        )
        NavigationItem(
            onClick = {
                navController.popBackStack(state.currentScreenName, inclusive = true)
                viewModel.selectPage("InboxListScreen")
                navController.navigate("InboxListScreen")
            },
            isSelected = state.currentScreenName == "InboxListScreen",
            icon = Icons.Default.KeyboardArrowDown,
            titleResId = R.string.inbox_title,
            count = 24
        )
        NavigationItem(
            onClick = {
                navController.popBackStack(state.currentScreenName, inclusive = true)
                viewModel.selectPage("OutboxListScreen")
                navController.navigate("OutboxListScreen")
            },
            isSelected = state.currentScreenName == "OutboxListScreen",
            icon = Icons.Default.KeyboardArrowUp,
            titleResId = R.string.outbox_title,
            count = 24
        )
        Spacer(modifier = modifier.weight(1f))
        TextButton(onClick = { navController.navigate("SettingsScreen") }) {
            Row(
                modifier = modifier.fillMaxWidth()

            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(id = R.string.settings_title)
                )
                Spacer(modifier = modifier.width(MARGIN_SMALLER))
                Text(
                    text = stringResource(id = R.string.settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = modifier.width(2.dp))
            }
        }
    }
}

@Composable
fun NavigationItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    icon: ImageVector,
    titleResId: Int,
    count: Int,
    onClick: () -> Unit
) {
    val color =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clip(shape = CircleShape)
            .clickable { onClick() }
            .background(color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .fillMaxWidth()
            .padding(MARGIN_DEFAULT)

    ) {
        Icon(
            icon,
            contentDescription = stringResource(id = titleResId)
        )
        Spacer(modifier = modifier.width(MARGIN_SMALLER))
        Text(
            text = stringResource(id = titleResId),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Spacer(modifier = modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = color)
        Spacer(modifier = modifier.width(2.dp))
    }
}