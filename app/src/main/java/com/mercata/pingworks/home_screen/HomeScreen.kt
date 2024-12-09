@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterialApi::class
)

package com.mercata.pingworks.home_screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.DEFAULT_LIST_ITEM_STATUS_ICON_SIZE
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_HEIGHT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import com.mercata.pingworks.common.NavigationDrawerBody
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.drafts.DBDraftWithReaders
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.models.CachedAttachment
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.theme.bodyFontFamily
import com.mercata.pingworks.theme.displayFontFamily
import com.mercata.pingworks.utils.getProfilePictureUrl
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current as FragmentActivity
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        //ignore
    }

    val state by viewModel.state.collectAsState()

    val refreshState = rememberPullRefreshState(state.refreshing, onRefresh = {
        viewModel.refresh()
    })

    BackHandler(enabled = state.selectedItems.isNotEmpty()) {
        state.selectedItems.clear()
    }

    LaunchedEffect(state.searchOpened) {
        if (state.searchOpened) {
            searchFocusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(state.sendingSnackBar) {
        if (state.sendingSnackBar) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.message_sending),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(state.undoDelete) {
        state.undoDelete?.let { undoDeleteLabel ->
            coroutineScope.launch {
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = context.getString(undoDeleteLabel),
                    actionLabel = context.getString(R.string.undo_button),
                    duration = SnackbarDuration.Short
                )
                when (snackbarResult) {
                    SnackbarResult.Dismissed -> {
                        viewModel.onCountdownSnackBarFinished()
                    }

                    SnackbarResult.ActionPerformed -> {
                        viewModel.onUndoDeletePressed()
                    }
                }
            }
        }
    }

    ModalNavigationDrawer(drawerState = drawerState,
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
        Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                LargeTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        scrolledContainerColor = if (state.selectedItems.isNotEmpty()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                        containerColor = if (state.selectedItems.isNotEmpty()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                        titleContentColor = if (state.selectedItems.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = if (state.selectedItems.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = if (state.selectedItems.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                    ),
                    title = {
                        Text(
                            stringResource(id = state.screen.titleResId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }, navigationIcon = {
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
                    }, actions = {
                        if (state.selectedItems.isEmpty()) {
                            Row {
                                AnimatedVisibility(
                                    visible = state.searchOpened, modifier = modifier.weight(1f)
                                ) {
                                    Row(modifier = modifier.fillMaxWidth()) {
                                        Spacer(modifier = modifier.width(MARGIN_DEFAULT + 42.dp))
                                        Box(
                                            modifier = modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            BasicTextField(keyboardOptions = KeyboardOptions(
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
                                            IconButton(onClick = {
                                                if (state.query.isEmpty()) {
                                                    viewModel.toggleSearch()
                                                } else {
                                                    viewModel.onSearchQuery("")
                                                }
                                            }) {
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
                        } else {
                            IconButton(onClick = {
                                viewModel.deleteSelected()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }, scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                FloatingActionButton(modifier = modifier.sharedBounds(
                    rememberSharedContentState(
                        key = "composing_bounds"
                    ), animatedVisibilityScope
                ),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        navController.navigate("ComposingScreen/null/null/null/null")
                    }) {
                    Icon(Icons.Filled.Edit, stringResource(id = R.string.create_new_message))
                }
            }) { padding ->
            Box(
                modifier = modifier.pullRefresh(
                    state = refreshState, enabled = topAppBarState.collapsedFraction == 0F
                )
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + (MARGIN_DEFAULT.value / 2).dp,
                        start = padding.calculateLeftPadding(LayoutDirection.Ltr),
                        end = padding.calculateRightPadding(LayoutDirection.Ltr),
                        bottom = padding.calculateBottomPadding() + (MARGIN_DEFAULT.value * 1.5).dp + 52.dp
                    ), modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.items.filter { it != state.itemToDelete },
                        key = { it.getMessageId() }) { item ->
                        SwipeContainer(modifier = modifier.animateItem(),
                            item = item,
                            onDelete = when (item) {
                                is DBMessageWithDBAttachments -> {
                                    if (state.screen == HomeScreen.Outbox) {
                                        {
                                            viewModel.deleteItem(item)
                                        }
                                    } else {
                                        null
                                    }
                                }

                                is CachedAttachment,
                                is DBDraftWithReaders -> {
                                    {
                                        viewModel.deleteItem(item)
                                    }
                                }

                                else -> null
                            },
                            onUpdateReadState = when (state.screen) {
                                HomeScreen.Inbox, HomeScreen.Broadcast -> {
                                    { message ->
                                        viewModel.updateRead(message as DBMessageWithDBAttachments)
                                    }
                                }

                                else -> null
                            }) {
                            MessageViewHolder(item = item,
                                currentUser = state.currentUser!!,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onMessageSelected = when (item) {
                                    is CachedAttachment -> { attachment ->
                                        viewModel.toggleSelectItem(attachment)
                                    }

                                    else -> null
                                },
                                isSelected = state.selectedItems.contains(item),
                                onMessageClicked = { message ->
                                    when (message) {
                                        is CachedAttachment -> {
                                            val attachment = item as CachedAttachment
                                            val intent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_STREAM, attachment.uri)
                                                type = attachment.type
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            val shareIntent = Intent.createChooser(intent, null)
                                            launcher.launch(shareIntent)
                                        }

                                        is DBDraftWithReaders -> navController.navigate(
                                            "ComposingScreen/null/${state.screen.outbox}/${message.draft.draftId}/null",
                                        )

                                        else -> navController.navigate(
                                            "MessageDetailsScreen/${message.getMessageId()}/${state.screen.outbox}",
                                        )
                                    }
                                })
                        }
                    }
                }
                if (state.items.isEmpty()) {
                    EmptyPlaceholder(
                        modifier = modifier.align(Alignment.Center),
                        screen = state.screen
                    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.MessageViewHolder(
    currentUser: UserData,
    item: HomeItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMessageClicked: (message: HomeItem) -> Unit,
    onMessageSelected: ((message: HomeItem) -> Unit)?
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "message_bounds/${item.getMessageId()}"
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .background(color = MaterialTheme.colorScheme.surface)
                .height(MESSAGE_LIST_ITEM_HEIGHT)
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        onMessageClicked(item)
                    },
                    onLongClick = if (onMessageSelected == null) {
                        null
                    } else {
                        {
                            onMessageSelected(item)
                        }
                    },
                )
                .padding(horizontal = MARGIN_DEFAULT)

        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = modifier
                    .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                    .clickable {
                        onMessageSelected?.invoke(item)
                    }
                    .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        stringResource(id = R.string.selected_label),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    when (item) {
                        is CachedAttachment -> {
                            val resId = if (item.type?.contains("image") == true) {
                                R.drawable.image
                            } else if (item.type?.contains("video") == true) {
                                R.drawable.video
                            } else if (item.type?.contains("audio") == true) {
                                R.drawable.sound
                            } else {
                                R.drawable.file
                            }

                            Icon(
                                painter = painterResource(resId),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        else -> {
                            ProfileImage(
                                modifier.size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                                    .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS)),
                                item.getContacts().firstOrNull()?.address?.getProfilePictureUrl() ?: "",
                                onError = { _ ->
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
                                })
                        }
                    }
                }
            }
            Spacer(modifier = modifier.width(MARGIN_DEFAULT))
            Column {
                Text(
                    modifier = modifier.sharedBounds(
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
            Spacer(
                modifier
                    .widthIn(min = MARGIN_DEFAULT)
                    .fillMaxWidth()
            )
            Column {
                if (item.isUnread()) {
                    Canvas(modifier = modifier.requiredSize(DEFAULT_LIST_ITEM_STATUS_ICON_SIZE)) {
                        drawCircle(primaryColor, radius = 4.dp.toPx())
                    }
                }
                if (item.hasAttachments()) {
                    Icon(
                        modifier = modifier.requiredSize(DEFAULT_LIST_ITEM_STATUS_ICON_SIZE),
                        painter = painterResource(R.drawable.attach),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

enum class SwipeAction {
    Deleted, UpdatedRead, Idle
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

    val state = rememberSwipeToDismissBoxState(positionalThreshold = { _ -> 0f },
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
        })

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
            imageVector = if (deleteSwipe) Icons.Default.Delete else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (deleteSwipe) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
fun EmptyPlaceholder(modifier: Modifier = Modifier, screen: HomeScreen) {
    Column(
        modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (screen.icon != null) {
            Icon(
                imageVector = screen.icon,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier.size(100.dp),
                contentDescription = null
            )
        } else if (screen.iconResId != null) {
            Icon(
                painter = painterResource(id = screen.iconResId),
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier.size(100.dp),
                contentDescription = null
            )
        }
        Text(
            stringResource(id = screen.titleResId),
            fontFamily = displayFontFamily,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
        Text(
            stringResource(id = screen.placeholderDescriptionResId),
            fontFamily = bodyFontFamily,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}