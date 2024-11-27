@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterialApi::class
)

package com.mercata.pingworks.contacts_screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.CONTACT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.CONTACT_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.db.notifications.DBNotification
import com.mercata.pingworks.home_screen.SwipeContainer
import com.mercata.pingworks.utils.getProfilePictureUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.ContactsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val topAppBarState = rememberTopAppBarState()
    val context = LocalContext.current as FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshState = rememberPullRefreshState(state.refreshing, onRefresh = {
        viewModel.refresh()
    })


    BackHandler(enabled = state.selectedContacts.isNotEmpty()) {
        state.selectedContacts.clear()
    }

    LaunchedEffect(state.itemToDelete) {
        state.itemToDelete?.let {
            coroutineScope.launch {
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.contact_deleted),
                    actionLabel = context.getString(R.string.undo_button),
                    duration = SnackbarDuration.Short
                )
                when (snackbarResult) {
                    SnackbarResult.Dismissed -> {
                        viewModel.onSnackBarCountdownFinished()
                    }

                    SnackbarResult.ActionPerformed -> {
                        viewModel.onUndoDeletePressed()
                    }
                }
            }
        }
    }

    fun openComposingScreen() {
        navController.navigate("ComposingScreen/${
            state.selectedContacts.joinToString(
                ","
            ) { it.address }
        }/null/null")
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (state.selectedContacts.isEmpty()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                    titleContentColor = if (state.selectedContacts.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = if (state.selectedContacts.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = if (state.selectedContacts.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Text(
                        stringResource(id = R.string.contacts_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedContacts.isNotEmpty()) {
                            state.selectedContacts.clear()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button)
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    if (state.selectedContacts.isEmpty()) {
                        viewModel.toggleSearchAddressDialog(true)
                    } else {
                        val unapprovedRequests =
                            state.selectedContacts.filterIsInstance<DBNotification>()

                        if (unapprovedRequests.isEmpty()) {
                            openComposingScreen()
                        } else {
                            //TODO confirm adding selected requests to contacts
                            coroutineScope.launch(Dispatchers.IO) {
                                viewModel.addSelectedNotificationsToContacts()
                            }.invokeOnCompletion {
                                viewModel.syncWithServer()
                                openComposingScreen()
                            }
                        }
                    }
                }) {
                if (state.selectedContacts.isEmpty()) {
                    Icon(Icons.Filled.Add, stringResource(id = R.string.add_new_contact))
                } else {
                    Icon(Icons.Filled.Edit, stringResource(id = R.string.create_new_message))
                }
            }
        }
    ) { padding ->
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
                ),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(items = (state.notifications + state.contacts).filter { it != state.itemToDelete },
                    key = { contact -> contact.address }) { item ->
                    SwipeContainer(
                        modifier = modifier.animateItem(),
                        item = item,
                        onDelete = {
                            viewModel.removeItem(it)
                        }) {
                        ContactViewHolder(
                            modifier = modifier,
                            animatedVisibilityScope = animatedVisibilityScope,
                            person = item,
                            uploading = false,
                            isSelected = state.selectedContacts.contains(item),
                            onSelect = { person ->
                                viewModel.toggleSelect(person)
                            },
                            onClick = { person ->
                                if (state.selectedContacts.isEmpty()) {
                                    navController.navigate(
                                        "ContactDetailsScreen/${person.address}"
                                    )
                                } else {
                                    viewModel.toggleSelect(person)
                                }
                            }
                        )
                    }
                }
            }

            if (state.newContactSearchDialogShown) {
                AddContactDialog(modifier = modifier, state = state, viewModel = viewModel)
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.ContactViewHolder(
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    person: ContactItem,
    uploading: Boolean,
    isSelected: Boolean,
    onSelect: ((person: ContactItem) -> Unit)?,
    onClick: (person: ContactItem) -> Unit
) {

    val transparencyModifier: Modifier = modifier.let {
        when (person) {
            is DBNotification -> it.alpha(0.6f)
            else -> it
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "contact_bounds/${person.address}"
                ),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .fillMaxSize()
            .height(CONTACT_LIST_ITEM_HEIGHT)
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = {
                    onClick(person)
                },
                onLongClick = {
                    onSelect?.invoke(person)
                },
            )
            .padding(horizontal = MARGIN_DEFAULT)
    ) {
        if (uploading) {
            CircularProgressIndicator(modifier.size(CONTACT_LIST_ITEM_IMAGE_SIZE))
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = transparencyModifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "contact_image/${person.address}"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .clip(CircleShape)
                    .clickable {
                        onSelect?.invoke(person)
                    }
                    .size(CONTACT_LIST_ITEM_IMAGE_SIZE)
                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        stringResource(id = R.string.selected_label),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    ProfileImage(
                        modifier = transparencyModifier,
                        person.address.getProfilePictureUrl(),
                        onError = { _ ->
                            Text(
                                text = "${person.name?.firstOrNull() ?: person.address.first()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        })
                }
            }
        }
        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
        Column {
            person.name?.let {
                Text(
                    text = person.name!!,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = transparencyModifier
                )
            }
            Text(
                text = person.address,
                style = MaterialTheme.typography.bodyMedium,
                modifier = transparencyModifier
            )
        }
    }
}

@Composable
fun AddContactDialog(
    modifier: Modifier = Modifier,
    state: ContactsState,
    viewModel: ContactsViewModel
) {
    val addressFocusRequester = remember { FocusRequester() }
    var focusRequested = remember { false }
    val focusManager = LocalFocusManager.current
    val addressPresented =
        state.contacts.any { it.address == state.existingContactFound?.address }
    val samePerson = state.loggedInPersonAddress == state.newContactAddressInput.lowercase().trim()
    val titleResId = if (samePerson) {
        R.string.cannot_add_yourself
    } else {
        if (state.existingContactFound == null) {
            R.string.add_new_contact
        } else {
            if (addressPresented) {
                R.string.account_in_contacts
            } else {
                R.string.account_not_in_contacts
            }
        }
    }

    LaunchedEffect(focusRequested) {
        if (!focusRequested) {
            addressFocusRequester.requestFocus()
            focusRequested = true
        }
    }

    AlertDialog(
        icon = {
            if (state.loading) {
                CircularProgressIndicator(modifier = modifier.size(24.0.dp))
            } else {
                ProfileImage(
                    modifier
                        .clip(CircleShape)
                        .size(24.0.dp),
                    state.existingContactFound?.address?.getProfilePictureUrl() ?: "",
                    onError = { modifier ->
                        Icon(
                            Icons.Default.Person,
                            modifier = modifier.size(24.0.dp),
                            contentDescription = stringResource(id = R.string.add_new_contact)
                        )
                    })
            }
        },
        title = {
            Text(text = stringResource(id = titleResId), textAlign = TextAlign.Center)
        },
        text = {
            if (state.existingContactFound == null) {
                OutlinedTextField(
                    value = state.newContactAddressInput,
                    onValueChange = { str -> viewModel.onNewContactAddressInput(str) },
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = modifier
                        .fillMaxWidth()

                        .focusRequester(addressFocusRequester),
                    label = {
                        Text(
                            text = stringResource(id = R.string.input_contact_address_hint)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Search,
                        showKeyboardOnFocus = true,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            viewModel.searchNewContact()
                        }
                    ),
                )
            } else {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.existingContactFound.fullName)
                    Text(state.existingContactFound.address)
                }
            }
        },
        onDismissRequest = {
            viewModel.toggleSearchAddressDialog(false)
        },
        confirmButton = {
            if (!addressPresented && !samePerson) {
                TextButton(enabled = !state.loading && state.searchButtonActive,
                    onClick = {
                        if (state.existingContactFound == null) {
                            viewModel.searchNewContact()
                        } else {
                            viewModel.addContact()
                        }
                    }
                ) {
                    Text(stringResource(id = if (state.existingContactFound == null) R.string.search_address else R.string.add_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (state.existingContactFound == null) {
                        viewModel.toggleSearchAddressDialog(false)
                    } else {
                        viewModel.clearFoundContact()
                    }
                }
            ) {
                Text(stringResource(id = R.string.cancel_button))
            }
        }
    )
}