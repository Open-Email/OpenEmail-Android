@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.mercata.pingworks.contacts_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.CONTACT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.models.Person

@Composable
fun ContactsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    Scaffold(
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
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = state.contacts,
                    key = { it.id }) { item ->
                    ContactViewHolder(modifier = modifier, person = item)
                }
            }

            if (state.newContactSearchDialogShown) {
                AddContactDialog(modifier = modifier, state = state, viewModel = viewModel)
            }
        }
    }
}


@Composable
fun ContactViewHolder(modifier: Modifier = Modifier, person: Person) {
    Column(
        modifier
            .fillMaxSize()
            .height(CONTACT_LIST_ITEM_HEIGHT)
    ) {
        person.name?.let {
            Text(text = person.name)
        }
        Text(text = person.address)
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
    val titleResId = if (state.newContactFound == null) R.string.add_new_contact else {
        if (state.contacts.any { it.address == state.newContactFound.address }) {
            R.string.account_in_contacts
        } else {
            R.string.accont_not_in_contacts
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
            Icon(
                Icons.Default.Person,
                contentDescription = stringResource(id = R.string.add_new_contact)
            )
        },
        title = {
            Text(text = stringResource(id = titleResId), textAlign = TextAlign.Center)
        },
        text = {
            if (state.newContactFound == null) {
                OutlinedTextField(
                    value = state.newContactAddressInput ?: "",
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
                Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.newContactFound.fullName)
                    Text(state.newContactFound.address)
                }
            }
        },
        onDismissRequest = {
            viewModel.updateContactSearchDialog(false)
        },
        confirmButton = {
            TextButton(enabled = !state.loading && state.searchButtonActive,
                onClick = {
                    if (state.newContactFound == null) {
                        viewModel.searchNewContact()
                    } else {
                        viewModel.addContact()
                        viewModel.updateContactSearchDialog(false)
                    }
                }
            ) {
                Text(stringResource(id = if (state.newContactFound == null) R.string.search_address else R.string.add_button))
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