package com.mercata.pingworks.home_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.DEFAULT_DATE_TIME_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.utils.getProfilePictureUrl
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun AddContactDialog(
    modifier: Modifier = Modifier,
    state: HomeState,
    viewModel: HomeViewModel
) {
    val addressFocusRequester = remember { FocusRequester() }
    var focusRequested = remember { false }
    val focusManager = LocalFocusManager.current
    val addressPresented: Boolean = state.existingContactFound?.address?.let { newAddress ->
        viewModel.contactPresented(newAddress)
    } ?: false
    val samePerson = state.currentUser?.address?.lowercase() == state.newContactAddressInput.lowercase().trim()
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
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
        title = {
            Text(text = stringResource(id = titleResId), textAlign = TextAlign.Start, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            if (state.existingContactFound == null) {
                OutlinedTextField(
                    shape = CircleShape,
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
                    modifier = modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    with(state.existingContactFound) {
                        Text(fullName, modifier.align(alignment = Alignment.CenterHorizontally))
                        Text(address, modifier.align(alignment = Alignment.CenterHorizontally))
                        lastSeen?.let {

                            Text(
                                "${stringResource(R.string.last_seen)}: ${
                                    ZonedDateTime.ofInstant(
                                        it, ZoneId.systemDefault()
                                    ).format(DEFAULT_DATE_TIME_FORMAT)
                                }",
                                modifier.align(alignment = Alignment.CenterHorizontally)
                            )
                        }
                        if (!status.isNullOrBlank() || !about.isNullOrBlank()) {
                            Text(
                                stringResource(R.string.current),
                                modifier = modifier.padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT / 2
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!status.isNullOrBlank()) {
                            Text("${stringResource(R.string.status)}: $status")
                        }
                        if (!about.isNullOrBlank()) {
                            Text("${stringResource(R.string.about)}: $about")
                        }
                        if (!work.isNullOrBlank() || !organization.isNullOrBlank() || !department.isNullOrBlank() || !jobTitle.isNullOrBlank()) {
                            Text(
                                stringResource(R.string.work),
                                modifier = modifier.padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT / 2
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!work.isNullOrBlank()) {
                            Text("${stringResource(R.string.work)}: $work")
                        }
                        if (!organization.isNullOrBlank()) {
                            Text("${stringResource(R.string.organization)}: $organization")
                        }
                        if (!department.isNullOrBlank()) {
                            Text("${stringResource(R.string.department)}: $department")
                        }
                        if (!jobTitle.isNullOrBlank()) {
                            Text("${stringResource(R.string.jobTitle)}: $jobTitle")
                        }
                        if (!gender.isNullOrBlank() || !relationshipStatus.isNullOrBlank() || !education.isNullOrBlank() || !language.isNullOrBlank() || !placesLived.isNullOrBlank() || !notes.isNullOrBlank()) {
                            Text(
                                stringResource(R.string.personal),
                                modifier = modifier.padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT / 2
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!gender.isNullOrBlank()) {
                            Text("${stringResource(R.string.gender)}: $gender")
                        }
                        if (!relationshipStatus.isNullOrBlank()) {
                            Text("${stringResource(R.string.relationshipStatus)}: $relationshipStatus")
                        }
                        if (!education.isNullOrBlank()) {
                            Text("${stringResource(R.string.education)}: $education")
                        }
                        if (!language.isNullOrBlank()) {
                            Text("${stringResource(R.string.language)}: $language")
                        }
                        if (!placesLived.isNullOrBlank()) {
                            Text("${stringResource(R.string.placesLived)}: $placesLived")
                        }
                        if (!notes.isNullOrBlank()) {
                            Text("${stringResource(R.string.notes)}: $notes")
                        }
                        if (!interests.isNullOrBlank() || !books.isNullOrBlank() || !music.isNullOrBlank() || !movies.isNullOrBlank() || !sports.isNullOrBlank()) {
                            Text(
                                stringResource(R.string.interests),
                                modifier = modifier.padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT / 2
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!interests.isNullOrBlank()) {
                            Text("${stringResource(R.string.interests)}: $interests")
                        }
                        if (!books.isNullOrBlank()) {
                            Text("${stringResource(R.string.books)}: $books")
                        }
                        if (!movies.isNullOrBlank()) {
                            Text("${stringResource(R.string.movies)}: $movies")
                        }
                        if (!music.isNullOrBlank()) {
                            Text("${stringResource(R.string.music)}: $music")
                        }
                        if (!sports.isNullOrBlank()) {
                            Text("${stringResource(R.string.sports)}: $sports")
                        }
                        if (!website.isNullOrBlank() || !location.isNullOrBlank() || !mailingAddress.isNullOrBlank() || !phone.isNullOrBlank() || !streams.isNullOrBlank()) {
                            Text(
                                stringResource(R.string.contacts),
                                modifier = modifier.padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT / 2
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!website.isNullOrBlank()) {
                            Text("${stringResource(R.string.website)}: $website")
                        }
                        if (!location.isNullOrBlank()) {
                            Text("${stringResource(R.string.location)}: $location")
                        }
                        if (!mailingAddress.isNullOrBlank()) {
                            Text("${stringResource(R.string.mailingAddress)}: $mailingAddress")
                        }
                        if (!phone.isNullOrBlank()) {
                            Text("${stringResource(R.string.phone)}: $phone")
                        }
                        if (!streams.isNullOrBlank()) {
                            Text("${stringResource(R.string.streams)}: $streams")
                        }
                    }
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