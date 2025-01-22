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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.DEFAULT_LIST_ITEM_STATUS_ICON_SIZE
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import com.mercata.pingworks.common.NavigationDrawerBody
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.db.HomeItem
import com.mercata.pingworks.db.contacts.ContactItem
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.drafts.DBDraftWithReaders
import com.mercata.pingworks.db.messages.DBMessageWithDBAttachments
import com.mercata.pingworks.db.notifications.DBNotification
import com.mercata.pingworks.models.CachedAttachment
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.getProfilePictureUrl
import com.mercata.pingworks.utils.measureTextWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SharedTransitionScope.HomeScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {

    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val listState = rememberLazyListState()
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

    fun openComposingScreen() {
        navController.navigate(
            "ComposingScreen/${
                viewModel.selectedItems.mapNotNull { it.getAddressValue() }.joinToString(",")
            }/null/null"
        )
    }

    val fabWidth by animateDpAsState(
        targetValue = if (listState.isScrollInProgress) 56.dp else 56.dp + MARGIN_DEFAULT + measureTextWidth(
            stringResource(if (viewModel.selectedItems.isEmpty()) state.screen.fabTitleRes else R.string.create_message),
            typography.labelLarge
        )
    )

    val refreshState = rememberPullRefreshState(state.refreshing, onRefresh = {
        viewModel.refresh()
    })

    BackHandler(enabled = viewModel.selectedItems.isNotEmpty()) {
        viewModel.selectedItems.clear()
    }

    LaunchedEffect(state.snackBar) {
        if (state.snackBar != null) {
            coroutineScope.launch {
                state.snackBar?.let {
                    snackbarHostState.showSnackbar(
                        message = context.getString(it.titleResId),
                        duration = SnackbarDuration.Short
                    )
                }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colorScheme.surface,
                drawerTonalElevation = 0.dp,
                drawerShape = RectangleShape
            ) {
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
                    unread = viewModel.unread,
                )
            }
        }) {
        Scaffold(snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }, topBar = {
            Column(
                modifier = modifier.background(if (viewModel.selectedItems.isEmpty()) colorScheme.surface else colorScheme.primary)

            ) {
                Spacer(
                    modifier = modifier.height(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
                )
                OutlinedTextField(
                    value = state.query,
                    shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS / 2),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        showKeyboardOnFocus = true,
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                    }),
                    singleLine = true,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(MARGIN_DEFAULT)
                        .focusRequester(searchFocusRequester),
                    textStyle = typography.bodyLarge,
                    leadingIcon = {
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
                                imageVector = Icons.Rounded.Menu,
                                tint = colorScheme.onSurfaceVariant,
                                contentDescription = stringResource(id = R.string.navigation_menu_title)
                            )
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = if (viewModel.selectedItems.isEmpty())
                            colorScheme.surfaceVariant
                        else
                            colorScheme.onPrimary,
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    ),
                    trailingIcon = {

                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.onSearchQuery("")
                            }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    stringResource(id = R.string.clear_button),
                                    tint = colorScheme.onSurface
                                )
                            }
                        } else {
                            Box(modifier.padding(end = MARGIN_DEFAULT)) {
                                ProfileImage(
                                    modifier
                                        .clickable {
                                            navController.navigate("ProfileScreen")
                                        }
                                        .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                                        .clip(CircleShape),
                                    //TODO uncomment
                                    state.currentUser?.address?.getProfilePictureUrl() ?: "",
                                    onError = {
                                        Box(
                                            modifier
                                                .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                                                .background(color = colorScheme.surface)
                                                .border(
                                                    width = 1.dp,
                                                    color = colorScheme.outline,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${state.currentUser?.name?.firstOrNull() ?: ""}${
                                                    state.currentUser?.name?.getOrNull(
                                                        1
                                                    ) ?: ""
                                                }",
                                                style = typography.titleMedium,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                    })
                            }
                        }
                    },

                    placeholder = {
                        Text(
                            stringResource(R.string.search),
                            style = typography.bodyLarge
                        )
                    },
                    onValueChange = {
                        viewModel.onSearchQuery(it)
                    })
                Text(
                    stringResource(state.screen.titleResId),
                    style = typography.labelLarge.copy(
                        color =
                        if (viewModel.selectedItems.isEmpty())
                            colorScheme.onSurface
                        else
                            colorScheme.onPrimary
                    ),
                    modifier = modifier.padding(
                        vertical = MARGIN_DEFAULT / 2, horizontal = MARGIN_DEFAULT
                    )
                )
                HorizontalDivider(color = colorScheme.outline)
            }
        }, floatingActionButton = {
            Row(
                modifier = modifier
                    .height(56.dp)
                    .width(fabWidth)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS))

                    .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                    .background(color = colorScheme.primary)

                    .clickable {
                        if (state.screen == HomeScreen.Contacts) {
                            if (viewModel.selectedItems.isEmpty()) {
                                viewModel.toggleSearchAddressDialog(true)
                            } else {
                                val unapprovedRequests =
                                    viewModel.selectedItems.filterIsInstance<DBNotification>()
                                if (unapprovedRequests.isEmpty()) {
                                    openComposingScreen()
                                } else {
                                    viewModel.showRequestApprovingConfirmationDialog()
                                }
                            }
                        } else {
                            navController.navigate("ComposingScreen/null/null/null")
                        }
                    }
                    .sharedBounds(
                        rememberSharedContentState(
                            key = "composing_bounds"
                        ), animatedVisibilityScope
                    ),

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Absolute.Center
            ) {
                Icon(
                    painterResource(if (viewModel.selectedItems.isEmpty()) state.screen.fabIcon else R.drawable.edit),
                    null,
                    tint = colorScheme.onPrimary
                )
                AnimatedVisibility(visible = !listState.isScrollInProgress) {
                    Row {
                        Spacer(modifier.width(MARGIN_DEFAULT / 2))
                        Text(
                            stringResource(if (viewModel.selectedItems.isEmpty()) state.screen.fabTitleRes else R.string.create_message),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            style = typography.labelLarge.copy(color = colorScheme.onPrimary)
                        )
                    }
                }
            }

        }) { padding ->
            Box(
                modifier = modifier.pullRefresh(
                    state = refreshState, enabled = topAppBarState.collapsedFraction == 0F
                )
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + (MARGIN_DEFAULT.value / 2).dp,
                        start = padding.calculateLeftPadding(LocalLayoutDirection.current),
                        end = padding.calculateRightPadding(LocalLayoutDirection.current),
                        bottom = padding.calculateBottomPadding() + (MARGIN_DEFAULT.value * 1.5).dp + 52.dp
                    ), modifier = Modifier.fillMaxSize()
                ) {
                    items(items = viewModel.items,
                        key = { item -> item.getMessageId() }) { item ->
                        if (item is Separator) {
                            Text(
                                item.getSeparatorTitle(context),
                                style = typography.labelLarge,
                                modifier = modifier.padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT / 2,
                                    start = MARGIN_DEFAULT,
                                    end = MARGIN_DEFAULT
                                )
                            )
                        } else {
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

                                    is CachedAttachment, is DBDraftWithReaders, is ContactItem -> {
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
                                Column {
                                    MessageViewHolder(item = item,
                                        currentUser = state.currentUser!!,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onMessageSelected = when (item) {
                                            is DBMessageWithDBAttachments -> {
                                                when (state.screen) {
                                                    HomeScreen.Outbox -> { attachment ->
                                                        viewModel.toggleSelectItem(attachment)
                                                    }

                                                    else -> null
                                                }

                                            }

                                            is CachedAttachment, is ContactItem -> { attachment ->
                                                viewModel.toggleSelectItem(attachment)
                                            }

                                            else -> null
                                        },
                                        isSelected = viewModel.selectedItems.contains(item),
                                        onMessageClicked = { message ->
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    state.itemToDelete?.run {
                                                        viewModel.onDeleteWaitComplete()
                                                    }
                                                }
                                                withContext(Dispatchers.Main) {
                                                    when (message) {
                                                        is CachedAttachment -> {
                                                            val attachment =
                                                                item as CachedAttachment
                                                            val intent: Intent = Intent().apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(
                                                                    Intent.EXTRA_STREAM,
                                                                    attachment.uri
                                                                )
                                                                type = attachment.type
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            val shareIntent =
                                                                Intent.createChooser(intent, null)
                                                            launcher.launch(shareIntent)
                                                        }

                                                        is DBDraftWithReaders -> navController.navigate(
                                                            "ComposingScreen/null/${state.screen.outbox}/${message.draft.draftId}",
                                                        )

                                                        is DBContact -> {
                                                            navController.navigate(
                                                                "ContactDetailsScreen/${item.getAddressValue()}/${false}"
                                                            )
                                                        }

                                                        is DBNotification -> {
                                                            navController.navigate(
                                                                "ContactDetailsScreen/${item.getAddressValue()}/${true}"
                                                            )
                                                        }

                                                        else -> navController.navigate(
                                                            "MessageDetailsScreen/${message.getMessageId()}/${state.screen.outbox}/${
                                                                message.getAddressValue().equals(
                                                                    state.currentUser!!.address,
                                                                    false
                                                                )
                                                            }",
                                                        )
                                                    }
                                                }
                                            }


                                        })
                                    HorizontalDivider(color = colorScheme.outline)
                                }
                            }
                        }
                    }
                }
                if (viewModel.items.isEmpty()) {
                    EmptyPlaceholder(
                        modifier = modifier.align(Alignment.Center), screen = state.screen
                    )
                }

                if (state.newContactSearchDialogShown) {
                    AddContactDialog(
                        modifier = modifier,
                        state = state,
                        viewModel = viewModel,
                        navController = navController
                    )
                }

                if (state.addRequestsToContactsDialogShown) {
                    AlertDialog(
                        tonalElevation = 0.dp,
                        shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
                        icon = {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = stringResource(id = R.string.warning),
                                tint = colorScheme.primary
                            )
                        },
                        title = {
                            Text(text = stringResource(R.string.warning))
                        },
                        text = {
                            Column {
                                Text(
                                    text = stringResource(R.string.add_selected_requests_warning),
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                                Button(modifier = modifier.fillMaxWidth(), onClick = {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        viewModel.addSelectedNotificationsToContacts()
                                    }.invokeOnCompletion {
                                        viewModel.syncWithServer()
                                        openComposingScreen()
                                    }
                                }) {
                                    Text(
                                        stringResource(id = R.string.add_contacts_and_proceed),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (viewModel.selectedItems.filterIsInstance<DBContact>()
                                        .isNotEmpty()
                                ) {
                                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                                    OutlinedButton(modifier = modifier.fillMaxWidth(), onClick = {
                                        viewModel.clearSelection()
                                        openComposingScreen()
                                    }) {
                                        Text(
                                            stringResource(id = R.string.proceed_with_existing_contacts_only),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        },
                        onDismissRequest = { viewModel.hideRequestApprovingConfirmationDialog() },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.hideRequestApprovingConfirmationDialog()
                                }
                            ) {
                                Text(stringResource(id = R.string.cancel_button))
                            }
                        },
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
    val primaryColor = colorScheme.primary

    Box {
        Row(verticalAlignment = Alignment.Top, modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "message_bounds/${item.getMessageId()}"
                ),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .background(color = colorScheme.surface)
            //.height(MESSAGE_LIST_ITEM_HEIGHT)
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
            .padding(MARGIN_DEFAULT)

        ) {
            Box(contentAlignment = Alignment.TopStart) {
                Box(contentAlignment = Alignment.Center,
                    modifier = modifier
                        .clip(CircleShape)
                        .clickable {
                            onMessageSelected?.invoke(item)
                        }
                        .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                        .background(if (isSelected) colorScheme.primary else colorScheme.surface)) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            stringResource(id = R.string.selected_label),
                            tint = colorScheme.onPrimary
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
                                    tint = colorScheme.onPrimary
                                )
                            }

                            else -> {
                                ProfileImage(
                                    modifier
                                        //Elevation bug under the navigation drawer
                                        /*.sharedBounds(
                                            sharedContentState = rememberSharedContentState(
                                                key = "message_image/${item.getMessageId()}"
                                            ),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                        )*/
                                        .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                                        .clip(CircleShape),
                                    item.getAddressValue()?.getProfilePictureUrl() ?: "",
                                    onError = {
                                        Box(
                                            modifier
                                                .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                                                .background(color = colorScheme.surface)
                                                .border(
                                                    width = 1.dp,
                                                    color = colorScheme.outline,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (item.getContacts().isEmpty()) {
                                                    "${currentUser.name.firstOrNull() ?: ""}${
                                                        currentUser.name.getOrNull(
                                                            1
                                                        ) ?: ""
                                                    }"
                                                } else {
                                                    "${
                                                        item.getContacts()
                                                            .first().fullName.firstOrNull() ?: ""
                                                    }${
                                                        item.getContacts()
                                                            .first().fullName.getOrNull(1) ?: ""
                                                    }"
                                                },
                                                style = typography.titleMedium,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                    })
                            }
                        }
                    }
                }
                if (item.isUnread()) {
                    Canvas(
                        modifier = modifier.requiredSize(
                            DEFAULT_LIST_ITEM_STATUS_ICON_SIZE
                        )
                    ) {
                        drawCircle(primaryColor, radius = 6.dp.toPx())
                    }
                }
            }
            Spacer(modifier = modifier.width(MARGIN_DEFAULT))
            Column {
                Row {
                    if (item.getTitle().isNotEmpty()) {
                        Text(
                            text = item.getTitle(),
                            modifier = modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = typography.titleMedium,
                        )
                    } else {
                        Spacer(modifier = modifier.weight(1f))
                    }
                    item.getTimestamp()?.let { timestamp ->
                        val current = LocalDateTime.now()
                        val localDateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            ZoneId.systemDefault()
                        )
                        val today =
                            current.year == localDateTime.year && current.dayOfYear == localDateTime.dayOfYear
                        val formatter =
                            if (today) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofPattern(
                                "dd.MM.yyyy"
                            )
                        Text(
                            formatter.format(localDateTime),
                            modifier = modifier.padding(start = MARGIN_DEFAULT),
                            style = typography.titleSmall.copy(color = if (item.isUnread()) colorScheme.onSurface else colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier.height(MARGIN_DEFAULT / 2))
                item.getSubtitle()?.let { subject ->
                    Text(
                        modifier = modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "message_subject/${item.getMessageId()}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                        text = subject,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = typography.titleSmall,
                    )
                }
                Text(
                    modifier = modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "message_body/${item.getMessageId()}"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                    text = item.getTextBody(),
                    maxLines = 2,
                    style = typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                )
                item.getAttachmentsAmount()?.takeIf { it > 0 }?.let { attachmentsAmount ->
                    Spacer(modifier.height(MARGIN_DEFAULT / 2))
                    Row(
                        modifier = modifier
                            .clip(RoundedCornerShape(100.dp))
                            .border(
                                width = 1.dp,
                                color = colorScheme.outline,
                                shape = RoundedCornerShape(100.dp)
                            )
                            .padding(
                                start = MARGIN_DEFAULT / 2,
                                top = MARGIN_DEFAULT / 2,
                                bottom = MARGIN_DEFAULT / 2,
                                end = MARGIN_DEFAULT * 0.75f
                            ),
                    ) {
                        Icon(
                            painterResource(R.drawable.attach),
                            contentDescription = null,
                            tint = colorScheme.onSurface,
                            modifier = modifier.size(16.dp)
                        )
                        Spacer(modifier.width(MARGIN_DEFAULT / 4))
                        Text(
                            String.format(
                                pluralStringResource(
                                    R.plurals.attachments,
                                    attachmentsAmount
                                ), attachmentsAmount
                            ),
                            style = typography.bodyMedium
                        )
                    }
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
        SwipeToDismissBoxValue.StartToEnd -> colorScheme.primary
        SwipeToDismissBoxValue.EndToStart -> colorScheme.error
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
            tint = if (deleteSwipe) colorScheme.onError else colorScheme.onPrimary,
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
        Icon(
            painter = painterResource(id = screen.iconResId),
            tint = colorScheme.outlineVariant,
            modifier = modifier.size(40.dp),
            contentDescription = null
        )
        Spacer(modifier = modifier.height(MARGIN_DEFAULT * 0.75f))
        Text(
            stringResource(id = screen.placeholderDescriptionResId),
            textAlign = TextAlign.Center,
            style = typography.labelLarge.copy(color = colorScheme.outlineVariant)
        )
    }
}