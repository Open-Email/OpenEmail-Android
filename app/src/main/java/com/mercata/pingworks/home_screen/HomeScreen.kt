@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterialApi::class
)

package com.mercata.pingworks.home_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_HEIGHT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import com.mercata.pingworks.common.NavigationDrawerBody
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.registration.UserData
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.HomeScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {

    val coroutineScope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    val state by viewModel.state.collectAsState()

    val refreshState = rememberPullRefreshState(state.refreshing, onRefresh = {
        viewModel.refresh()
    })

    LaunchedEffect(key1 = state.searchOpened) {
        if (state.searchOpened) {
            searchFocusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerBody(
                    modifier = modifier,
                    navController = navController,
                    onItemClick = { homeScreen ->
                        coroutineScope.launch {
                            viewModel.selectScreen(homeScreen)
                            drawerState.close()
                        }
                    },
                    selected = state.screen,
                    unread = state.unread,
                )
            }
        }) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        scrolledContainerColor = MaterialTheme.colorScheme.primary
                    ),
                    title = {
                        Text(
                            stringResource(id = state.screen.titleResId),
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
                                contentDescription = stringResource(id = R.string.navigation_menu_title)
                            )
                        }
                    },
                    actions = {
                        Row {
                            AnimatedVisibility(
                                visible = state.searchOpened,
                                modifier = modifier.weight(1f)
                            ) {
                                Row(modifier = modifier.fillMaxWidth()) {
                                    Spacer(modifier = modifier.width(MARGIN_DEFAULT + 42.dp))
                                    Box(
                                        modifier = modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        BasicTextField(
                                            keyboardOptions = KeyboardOptions(
                                                imeAction = ImeAction.Search,
                                                showKeyboardOnFocus = true,
                                            ),
                                            singleLine = true,
                                            modifier = modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(
                                                    start = MARGIN_DEFAULT,
                                                    top = MARGIN_DEFAULT,
                                                    bottom = MARGIN_DEFAULT,
                                                    end = 48.dp
                                                )
                                                .focusRequester(searchFocusRequester),
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            value = state.query,
                                            onValueChange = {
                                                viewModel.onSearchQuery(it)
                                            })
                                        IconButton(onClick = { viewModel.onSearchQuery("") }) {
                                            Icon(
                                                Icons.Rounded.Clear,
                                                stringResource(id = R.string.clear_button),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = modifier.width(8.dp))
                                }
                            }
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(id = R.string.search)
                                )
                            }
                        }

                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    modifier = modifier.sharedBounds(
                        rememberSharedContentState(
                            key = "composing_bounds"
                        ),
                        animatedVisibilityScope
                    ),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        navController.navigate("ComposingScreen/null")
                    }) {
                    Icon(Icons.Filled.Edit, stringResource(id = R.string.create_new_message))
                }
            }
        ) { padding ->
            Box(
                modifier = modifier.pullRefresh(
                    state = refreshState,
                    enabled = topAppBarState.collapsedFraction == 0F
                )
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + (MARGIN_DEFAULT.value / 2).dp,
                        start = padding.calculateLeftPadding(LayoutDirection.Ltr),
                        end = padding.calculateRightPadding(LayoutDirection.Ltr),
                        bottom = padding.calculateBottomPadding() + (MARGIN_DEFAULT.value * 1.5).dp + 52.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = state.messages,
                        key = { it.getMessageId() }) { item ->
                        SwipeContainer(
                            modifier = modifier.animateItem(),
                            item = item,
                            onUpdateReadState = { i ->
                                //TODO change read state
                            }) {
                            MessageViewHolder(
                                item = item,
                                currentUser = state.currentUser!!,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onMessageClicked = { message ->
                                    navController.navigate(
                                        "MessageDetailsScreen/${message.getMessageId()}",
                                    )
                                })
                        }
                    }
                }
                PullRefreshIndicator(
                    modifier = modifier
                        .align(Alignment.TopCenter)
                        .padding(padding),
                    refreshing = state.refreshing,
                    state = refreshState,
                )
            }
        }
    }
}

@Composable
fun SharedTransitionScope.MessageViewHolder(
    currentUser: UserData,
    item: HomeItem,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMessageClicked: (message: HomeItem) -> Unit
) {

    //TODO public image
    val imageUrl = null//item.getContacts().firstOrNull().imageUrl ?: currentUser.avatarLink
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "message_bounds/${item.getMessageId()}"
                ),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .height(MESSAGE_LIST_ITEM_HEIGHT)
            .fillMaxWidth()
            .clickable {
                onMessageClicked(item)
            }
            .padding(horizontal = MARGIN_DEFAULT)

    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "message_image/${item.getMessageId()}"
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            if (imageUrl == null) {
                Text(
                    text = "${
                        if (item.getContacts().isEmpty()) {
                            currentUser.name.first()
                        } else {
                            item.getContacts()
                                .firstOrNull()?.fullName?.firstOrNull() ?: item.getContacts()
                                .firstOrNull()?.address?.first() ?: ""
                        }
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                AsyncImage(
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "message_image/${item.getMessageId()}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                        .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS)),
                    model = imageUrl,
                    contentDescription = stringResource(id = R.string.profile_image)
                )
            }
        }
        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
        Column {
            Text(
                modifier = modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "message_subject/${item.getMessageId()}"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                text = item.getSubject(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "message_body/${item.getMessageId()}"
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
                text = item.getTextBody(),
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class SwipeAction {
    Deleted,
    UpdatedRead,
    Idle
}

@Composable
fun <T> SwipeContainer(
    modifier: Modifier = Modifier,
    item: T,
    onDelete: ((T) -> Unit)? = null,
    onUpdateReadState: ((T) -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    var actionState by remember { mutableStateOf(SwipeAction.Idle) }

    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { _ -> 0f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    actionState = SwipeAction.UpdatedRead
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    actionState = SwipeAction.Deleted
                    true
                }

                SwipeToDismissBoxValue.Settled -> {
                    actionState = SwipeAction.Idle
                    false
                }

            }
        }
    )

    LaunchedEffect(key1 = actionState) {
        when (actionState) {
            SwipeAction.Deleted -> onDelete?.invoke(item)
            SwipeAction.UpdatedRead -> {
                state.snapTo(SwipeToDismissBoxValue.Settled)
                onUpdateReadState?.invoke(item)
            }

            SwipeAction.Idle -> state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        modifier = modifier,
        state = state,
        backgroundContent = {
            SwipeBackground(swipeValue = state.targetValue)
        },
        content = { content(item) },
        enableDismissFromEndToStart = onDelete != null,
        enableDismissFromStartToEnd = onUpdateReadState != null,
    )
}

@Composable
fun SwipeBackground(
    swipeValue: SwipeToDismissBoxValue
) {
    val deleteSwipe = swipeValue == SwipeToDismissBoxValue.EndToStart

    val color = when (swipeValue) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(MARGIN_DEFAULT),
        contentAlignment = if (deleteSwipe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Icon(
            imageVector = if (deleteSwipe) Icons.Default.Delete else Icons.Default.Favorite,
            contentDescription = null,
            tint = if (deleteSwipe) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
        )
    }
}