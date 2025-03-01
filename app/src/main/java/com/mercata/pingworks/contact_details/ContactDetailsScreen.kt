@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.mercata.pingworks.contact_details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.DEFAULT_DATE_TIME_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.common.SwitchViewHolder
import com.mercata.pingworks.utils.getProfilePictureUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun SharedTransitionScope.ContactDetailsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = viewModel()
) {

    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val imageSize = remember { 300.dp }
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackBarResId) {
        if (state.snackBarResId != null) {
            coroutineScope.launch {
                state.snackBarResId?.let {
                    snackBarHostState.showSnackbar(
                        message = context.getString(it),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.sharedBounds(
            rememberSharedContentState(
                key = "message_bounds/${state.address}"
            ),
            animatedVisibilityScope,
        ),
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        },
        topBar = {
            TopAppBar(title = {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = modifier.size(28.dp),
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    Text(
                        state.dbContact?.name ?: state.contact?.fullName
                        ?: stringResource(R.string.profile)
                    )
                }
            },
                navigationIcon = {
                    IconButton(content = {
                        Icon(
                            painterResource(R.drawable.back),
                            contentDescription = stringResource(R.string.back_button),
                        )
                    }, onClick = {
                        navController.popBackStack()
                    })
                },
                actions = {
                    when (state.type) {
                        ContactType.CurrentUser -> {
                            Button(
                                modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                                enabled = !state.loading,
                                onClick = {
                                    navController.navigate("ProfileScreen")
                                }) {
                                Text(stringResource(R.string.edit))
                            }
                        }

                        ContactType.ContactNotification -> {
                            Button(
                                modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                                enabled = !state.loading,
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        viewModel.approveRequest()
                                    }
                                }) {
                                Text(stringResource(R.string.add_contact))
                            }
                        }

                        else -> {
                            //ignore
                        }
                    }
                })
        },
        floatingActionButton = {
            if (!state.loading && state.type != ContactType.CurrentUser && state.type != ContactType.DetailsOnly) {
                ExtendedFloatingActionButton(
                    modifier = modifier.sharedBounds(
                        rememberSharedContentState(
                            key = "composing_bounds"
                        ),
                        animatedVisibilityScope
                    ),
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    onClick = {
                        if (state.type == ContactType.ContactNotification) {
                            viewModel.showRequestApprovingConfirmationDialog()
                        } else {
                            navController.navigate("ComposingScreen/${state.address}/null/null")
                        }

                    }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, stringResource(id = R.string.create_message))
                        Spacer(modifier.width(MARGIN_DEFAULT / 2))
                        Text(
                            stringResource(if (state.type == ContactType.ContactNotification) R.string.message else R.string.create_message),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            style = typography.labelLarge.copy(color = colorScheme.onPrimary)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            ProfileImage(
                modifier
                    .height(imageSize)
                //Elevation bug under the navigation drawer
                /*.sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "message_image/${state.address}"
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                )*/,
                imageUrl = state.address.getProfilePictureUrl(),
                onError = {
                    Icon(
                        painterResource(R.drawable.contacts),
                        modifier = Modifier.size(100.dp),
                        contentDescription = null,
                        tint = colorScheme.outline
                    )
                })

            Spacer(modifier.height(MARGIN_DEFAULT * 2))
            state.contact?.fullName?.let { name ->
                Text(
                    name,
                    modifier.padding(horizontal = MARGIN_DEFAULT),
                    style = typography.titleMedium
                )
            }
            Spacer(modifier.height(MARGIN_DEFAULT / 4))
            Text(
                state.address,
                modifier.padding(horizontal = MARGIN_DEFAULT),
                style = typography.bodyMedium.copy(color = colorScheme.outlineVariant),
            )
            Spacer(modifier.height(MARGIN_DEFAULT))
            ContactDivider(modifier.padding(horizontal = MARGIN_DEFAULT))
            if (state.type == ContactType.SavedContact) {
                SwitchViewHolder(
                    isChecked = state.dbContact?.receiveBroadcasts ?: false,
                    title = R.string.receive_broadcasts
                ) { viewModel.toggleBroadcast() }
                ContactDivider(modifier.padding(horizontal = MARGIN_DEFAULT))
                Spacer(modifier.height(MARGIN_DEFAULT / 2))
            }

            state.contact?.run {
                Column(modifier.padding(MARGIN_DEFAULT)) {
                    lastSeen?.let {
                        DataRow(
                            title = stringResource(R.string.last_seen),
                            description = ZonedDateTime.ofInstant(
                                it, ZoneId.systemDefault()
                            ).format(DEFAULT_DATE_TIME_FORMAT)
                        )
                        ContactDivider(modifier.padding(top = MARGIN_DEFAULT))
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
            Spacer(modifier = modifier.height(padding.calculateBottomPadding() + 72.dp))
        }
        if (state.requestApprovalDialogShown) {
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
                    Text(
                        text = String.format(
                            stringResource(R.string.placeholder_is_not_in_your_contacts),
                            state.contact?.fullName ?: state.address
                        )
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.add_to_contacts_and_message),
                    )
                },
                onDismissRequest = { viewModel.hideRequestApprovingConfirmationDialog() },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.hideRequestApprovingConfirmationDialog()
                        }
                    ) {
                        Text(stringResource(id = R.string.cancel_button))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.hideRequestApprovingConfirmationDialog()
                                viewModel.approveRequest()
                                withContext(Dispatchers.Main) {
                                    navController.navigate("ComposingScreen/${state.address}/null/null")
                                }
                            }
                        }
                    ) {
                        Text(stringResource(id = R.string.add_contact))
                    }
                },
            )
        }
    }
}

@Composable
fun DataRow(modifier: Modifier = Modifier, title: String, description: String) {
    Row {
        Text(
            title,
            style = typography.titleMedium
        )
        Spacer(modifier.weight(1f))
        Text(
            description, style = typography.bodyMedium
        )
    }
}

@Composable
fun ContactDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier
            .fillMaxWidth()
            .height(1.dp),
        thickness = 1.dp,
        color = colorScheme.outline
    )
}