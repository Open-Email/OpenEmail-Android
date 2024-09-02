@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class
)

package com.mercata.pingworks.contacts_screen

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.mercata.pingworks.CONTACT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.home_screen.SwipeContainer
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.ContactsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current as FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(state.showUndoDeleteSnackBar) {
        if (state.showUndoDeleteSnackBar) {
            coroutineScope.launch {
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.contact_deleted),
                    actionLabel = context.getString(R.string.undo_button),
                    duration = SnackbarDuration.Short
                )
                when (snackbarResult) {
                    SnackbarResult.Dismissed -> {
                        viewModel.onDeleteWaitComplete()
                    }

                    SnackbarResult.ActionPerformed -> {
                        viewModel.onUndoDeletePressed()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
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
                        navController.popBackStack()
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
                    viewModel.updateContactSearchDialog(true)
                }) {
                Icon(Icons.Filled.Add, stringResource(id = R.string.add_new_contact))
            }
        }
    ) { padding ->
        Box {
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
                itemsIndexed(items = state.contacts,
                    key = { _, contact -> contact.address }) { index, item ->

                    SwipeContainer(
                        modifier = modifier.animateItem(),
                        item = item,
                        onDelete = {
                            viewModel.removeItem(index)
                        }) {
                        ContactViewHolder(
                            modifier = modifier,
                            navController = navController,
                            animatedVisibilityScope = animatedVisibilityScope,
                            person = item,
                            uploading = item.address == state.loadingContactAddress
                        )
                    }
                }
            }

            if (state.newContactSearchDialogShown) {
                AddContactDialog(modifier = modifier, state = state, viewModel = viewModel)
            }
        }
    }
}


@Composable
fun SharedTransitionScope.ContactViewHolder(
    modifier: Modifier = Modifier,
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    person: DBContact,
    uploading: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically,
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
            .clickable {
                navController.navigate(
                    "ContactDetailsScreen/${person.address}"
                )
            }
            .padding(horizontal = MARGIN_DEFAULT)
    ) {
        if (uploading) {
            CircularProgressIndicator(modifier.size(40.0.dp))
        } else {
            if (person.imageUrl == null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "contact_image/${person.address}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .clip(CircleShape)
                        .size(40.0.dp)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "${person.name?.firstOrNull() ?: person.address.first()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                AsyncImage(
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "contact_image/${person.address}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .size(40.0.dp)
                        .clip(CircleShape),
                    model = person.imageUrl,
                    contentDescription = stringResource(id = R.string.profile_image)
                )
            }
        }

        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
        Column {
            person.name?.let {
                Text(text = person.name, style = MaterialTheme.typography.bodyLarge)
            }
            Text(text = person.address, style = MaterialTheme.typography.bodyMedium)
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
        state.contacts.any { it.address == state.newContactFound?.address }
    val samePerson = state.loggedInPersonAddress == state.newContactAddressInput.lowercase().trim()
    val titleResId = if (samePerson) {
        R.string.cannot_add_yourself
    } else {
        if (state.newContactFound == null) {
            R.string.add_new_contact
        } else {
            if (addressPresented) {
                R.string.account_in_contacts
            } else {
                R.string.accont_not_in_contacts
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
            if (state.loading) CircularProgressIndicator(modifier = modifier.size(24.0.dp)) else Icon(
                Icons.Default.Person,
                modifier = modifier.size(24.0.dp),
                contentDescription = stringResource(id = R.string.add_new_contact)
            )
        },
        title = {
            Text(text = stringResource(id = titleResId), textAlign = TextAlign.Center)
        },
        text = {
            if (state.newContactFound == null) {
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
                        keyboardType = KeyboardType.Unspecified,
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
                    Text(state.newContactFound.fullName)
                    Text(state.newContactFound.address)
                }
            }
        },
        onDismissRequest = {
            viewModel.updateContactSearchDialog(false)
        },
        confirmButton = {
            if (!addressPresented && !samePerson) {
                TextButton(enabled = !state.loading && state.searchButtonActive,
                    onClick = {
                        if (state.newContactFound == null) {
                            viewModel.searchNewContact()
                        } else {
                            viewModel.addContact()
                        }
                    }
                ) {
                    Text(stringResource(id = if (state.newContactFound == null) R.string.search_address else R.string.add_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (state.newContactFound == null) {
                        viewModel.updateContactSearchDialog(false)
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