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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.mercata.pingworks.home_screen.HomeScreen


@Composable
fun NavigationDrawerBody(
    navController: NavController,
    modifier: Modifier = Modifier,
    onItemClick: (screen: HomeScreen) -> Unit,
    selected: HomeScreen,
    unread: Map<HomeScreen, Int>
) {
    Column(modifier = modifier.padding(12.dp)) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier.padding(MARGIN_DEFAULT),
            fontWeight = FontWeight.Bold
        )
        HomeScreen.entries.forEach { screen ->
            NavigationItem(
                onClick = {
                    onItemClick(screen)
                },
                isSelected = selected == screen,
                icon = screen.icon,
                titleResId = screen.titleResId,
                count = unread[screen]
            )
        }
        Spacer(modifier = modifier.weight(1f))
        TextButton(onClick = { navController.navigate("ContactsScreen") }) {
            Row(
                modifier = modifier.fillMaxWidth()

            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(id = R.string.contacts_title)
                )
                Spacer(modifier = modifier.width(MARGIN_SMALLER))
                Text(
                    text = stringResource(id = R.string.contacts_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = modifier.width(2.dp))
            }
        }
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
    count: Int?,
    onClick: () -> Unit
) {
    val color =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clip(shape = CircleShape)
            .clickable { onClick() }
            .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
        if (count != null) {
            Spacer(modifier = modifier.weight(1f))
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = color)
            Spacer(modifier = modifier.width(2.dp))
        }
    }
}