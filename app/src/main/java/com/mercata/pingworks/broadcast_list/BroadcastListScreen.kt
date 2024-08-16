@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks.broadcast_list

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mercata.pingworks.BODY_TEXT_SIZE
import com.mercata.pingworks.HEADER_TEXT_SIZE
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_HEIGHT
import com.mercata.pingworks.models.BroadcastMessage
import com.mercata.pingworks.models.Message
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.BroadcastListScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    broadcastViewModel: BroadcastListViewModel = viewModel(),
) {

    val state by broadcastViewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet() { /* Drawer content */ }
        }) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            "Inbox",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (drawerState.isOpen) {
                                    drawerState.close()
                                } else {
                                    drawerState.open()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Localized description"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* do something */ }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Localized description"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        println()
                    }) {
                    Icon(Icons.Filled.Edit, "Create message")
                }
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + (MARGIN_DEFAULT.value / 2).dp,
                    start = padding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = padding.calculateRightPadding(LayoutDirection.Ltr),
                    bottom = padding.calculateBottomPadding() + (MARGIN_DEFAULT.value * 1.5).dp + 52.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.messages) { item ->
                    MessageViewHolder(
                        item = item,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onMessageClicked = { message ->
                            navController.navigate(
                                "MessageDetailsScreen/${message.id}"
                            )
                        })

                }
            }
        }
    }

}

@Composable
fun SharedTransitionScope.MessageViewHolder(
    item: BroadcastMessage,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMessageClicked: (message: Message) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "bounds/${item.id}"
                ),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .height(MESSAGE_LIST_ITEM_HEIGHT)
            .fillMaxWidth()
            .padding(horizontal = MARGIN_DEFAULT)
            .clickable {
                onMessageClicked(item)
            }
    ) {
        AsyncImage(
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(width = 72.0.dp, height = 72.0.dp)
                .clip(RoundedCornerShape(16.0.dp)),
            model = item.person.imageUrl,
            contentDescription = null
        )
        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
        Column {
            Text(
                text = item.subject,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontSize = HEADER_TEXT_SIZE,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.body,
                maxLines = 2,
                fontSize = BODY_TEXT_SIZE,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}