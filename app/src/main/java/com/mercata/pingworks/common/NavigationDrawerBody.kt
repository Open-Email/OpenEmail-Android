package com.mercata.pingworks.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    Column(modifier = modifier
        .padding(MARGIN_DEFAULT).background(MaterialTheme.colorScheme.surface)) {
        Logo(modifier = modifier.padding(horizontal = MARGIN_DEFAULT), size = LogoSize.Small)
        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
        HomeScreen.entries.forEach { screen ->
            NavigationItem(
                modifier = modifier,
                onClick = {
                    onItemClick(screen)
                },
                isSelected = selected == screen,
                painter = painterResource(id = screen.iconResId),
                titleResId = screen.titleResId,
                count = unread[screen]?.takeIf { it > 0 }
            )
        }
        Divider(modifier = modifier.fillMaxWidth().padding(vertical = MARGIN_DEFAULT), color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        NavigationItem(
            modifier = modifier,
            onClick = {
                navController.navigate("SettingsScreen")
            },
            isSelected = false,
            painter = painterResource(id = R.drawable.settings),
            titleResId = R.string.settings_title,
            count = null
        )
    }
}

@Composable
fun NavigationItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    painter: Painter,
    titleResId: Int,
    count: Int?,
    onClick: () -> Unit
) {
    val color =
        if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    Box(modifier.padding(vertical = MARGIN_DEFAULT / 4)) {
        Row(
            modifier = modifier
                .clip(shape = CircleShape)
                .clickable { onClick() }
                .background(color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .fillMaxWidth()
                .padding(MARGIN_DEFAULT)

        ) {
            Icon(
                painter = painter,
                tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                contentDescription = stringResource(id = titleResId)
            )
            Spacer(modifier = modifier.width(MARGIN_SMALLER))
            Text(
                text = stringResource(id = titleResId),
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
            if (count != null) {
                Spacer(modifier = modifier.weight(1f))
                Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = color)
                Spacer(modifier = modifier.width(2.dp))
            }
        }
    }
}