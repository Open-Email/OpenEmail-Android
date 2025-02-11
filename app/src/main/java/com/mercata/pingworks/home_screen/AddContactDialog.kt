package com.mercata.pingworks.home_screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.R

@Composable
fun AddContactDialog(
    modifier: Modifier = Modifier,
    state: HomeState,
    viewModel: HomeViewModel,
    navController: NavController
) {
    val addressFocusRequester = remember { FocusRequester() }
    var focusRequested = remember { false }
    val focusManager = LocalFocusManager.current
    val addressPresented: Boolean = state.existingContactFound?.address?.let { newAddress ->
        viewModel.contactPresented(newAddress)
    } ?: false
    val samePerson =
        state.currentUser?.address?.lowercase() == state.newContactAddressInput.lowercase().trim()
    val titleResId = if (samePerson) {
        R.string.cannot_add_yourself
    } else {
        if (state.existingContactFound == null) {
            R.string.add_contact
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

    LaunchedEffect(state.existingContactFound) {
        if (state.existingContactFound != null) {
            viewModel.toggleSearchAddressDialog(false)
            navController.navigate(
                "ContactDetailsScreen/${state.existingContactFound.address}/${true}"
            )
        }
    }

    AlertDialog(
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
        title = {
            Text(
                text = stringResource(id = titleResId),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            OutlinedTextField(
                shape = CircleShape,
                value = state.newContactAddressInput,
                onValueChange = { str -> viewModel.onNewContactAddressInput(str) },
                singleLine = true,
                enabled = !state.loading,
                isError = state.addressNotFoundError,
                supportingText = {
                    if (state.addressNotFoundError) {
                        Text(
                            text = stringResource(id = R.string.address_not_found_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
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
        },
        onDismissRequest = {
            viewModel.toggleSearchAddressDialog(false)
        },
        confirmButton = {
            if (!addressPresented && !samePerson) {
                TextButton(enabled = !state.loading && state.searchButtonActive,
                    onClick = {
                        viewModel.searchNewContact()
                    }
                ) {
                    Text(stringResource(id = R.string.search_address))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.toggleSearchAddressDialog(false)
                }
            ) {
                Text(stringResource(id = R.string.cancel_button))
            }
        }
    )
}