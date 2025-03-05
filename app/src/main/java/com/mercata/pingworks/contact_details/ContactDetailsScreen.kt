@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.mercata.pingworks.contact_details

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
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
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.PROFILE_IMAGE_HEIGHT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.common.SwitchViewHolder
import com.mercata.pingworks.utils.getProfilePictureUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
fun SharedTransitionScope.ContactDetailsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = viewModel()
) {

    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
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
                AnimatedVisibility(visible = state.loading) {
                    CircularProgressIndicator(
                        modifier = modifier.size(28.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
                AnimatedVisibility(visible = state.loading.not()) {
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
                    .height(PROFILE_IMAGE_HEIGHT)
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

            AnimatedVisibility(visible = state.contact != null) {
                Column {
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
                        style = typography.bodyMedium,
                    )
                    Spacer(modifier.height(MARGIN_DEFAULT))
                    ContactDivider(modifier.padding(horizontal = MARGIN_DEFAULT))
                }
            }
            if (state.type == ContactType.SavedContact) {
                SwitchViewHolder(
                    isChecked = state.dbContact?.receiveBroadcasts ?: false,
                    title = R.string.receive_broadcasts
                ) { viewModel.toggleBroadcast() }
                ContactDivider(modifier.padding(horizontal = MARGIN_DEFAULT))
                Spacer(modifier.height(MARGIN_DEFAULT / 2))
            }

            state.contact.let {
                Column(modifier.padding(MARGIN_DEFAULT)) {
                    AnimatedVisibility(visible = it?.lastSeen != null) {
                        Column {
                            DataRow(
                                title = stringResource(R.string.last_seen),
                                description = DateUtils.getRelativeTimeSpanString(
                                    (it?.lastSeen ?: Instant.now()).toEpochMilli(),
                                ).toString()
                            )
                            ContactDivider(modifier.padding(top = MARGIN_DEFAULT))
                        }
                    }

                    AnimatedVisibility(visible = !it?.status.isNullOrBlank() || !it?.about.isNullOrBlank()) {
                        Text(
                            stringResource(R.string.current),
                            modifier = modifier.padding(
                                top = MARGIN_DEFAULT,
                                bottom = MARGIN_DEFAULT / 2
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AnimatedVisibility(visible = !it?.status.isNullOrBlank()) {
                        TitledData(titleRes = R.string.status, data = it?.status ?: "")
                    }
                    AnimatedVisibility(visible = !it?.about.isNullOrBlank()) {
                        TitledData(titleRes = R.string.about, data = it?.about ?: "")
                    }
                    AnimatedVisibility(visible = !it?.work.isNullOrBlank() || !it?.organization.isNullOrBlank() || !it?.department.isNullOrBlank() || !it?.jobTitle.isNullOrBlank()) {
                        Text(
                            stringResource(R.string.work),
                            modifier = modifier.padding(
                                top = MARGIN_DEFAULT,
                                bottom = MARGIN_DEFAULT / 2
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AnimatedVisibility(visible = !it?.work.isNullOrBlank()) {
                        TitledData(titleRes = R.string.work, data = it?.work ?: "")
                    }
                    AnimatedVisibility(visible = !it?.organization.isNullOrBlank()) {
                        TitledData(titleRes = R.string.organization, data = it?.organization ?: "")
                    }
                    AnimatedVisibility(visible = !it?.department.isNullOrBlank()) {
                        TitledData(titleRes = R.string.department, data = it?.department ?: "")
                    }
                    AnimatedVisibility(visible = !it?.jobTitle.isNullOrBlank()) {
                        TitledData(titleRes = R.string.jobTitle, data = it?.jobTitle ?: "")
                    }
                    AnimatedVisibility(visible = !it?.gender.isNullOrBlank() || !it?.relationshipStatus.isNullOrBlank() || !it?.education.isNullOrBlank() || !it?.language.isNullOrBlank() || !it?.placesLived.isNullOrBlank() || !it?.notes.isNullOrBlank()) {
                        Text(
                            stringResource(R.string.personal),
                            modifier = modifier.padding(
                                top = MARGIN_DEFAULT,
                                bottom = MARGIN_DEFAULT / 2
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AnimatedVisibility(visible = !it?.gender.isNullOrBlank()) {
                        TitledData(titleRes = R.string.gender, data = it?.gender ?: "")
                    }
                    AnimatedVisibility(visible = !it?.relationshipStatus.isNullOrBlank()) {
                        TitledData(titleRes = R.string.relationshipStatus, data = it?.relationshipStatus ?: "")
                    }
                    AnimatedVisibility(visible = !it?.education.isNullOrBlank()) {
                        TitledData(titleRes = R.string.education, data = it?.education ?: "")
                    }
                    AnimatedVisibility(visible = !it?.language.isNullOrBlank()) {
                        TitledData(titleRes = R.string.language, data = it?.language ?: "")
                    }
                    AnimatedVisibility(visible = !it?.placesLived.isNullOrBlank()) {
                        TitledData(titleRes = R.string.placesLived, data = it?.placesLived ?: "")
                    }
                    AnimatedVisibility(visible = !it?.notes.isNullOrBlank()) {
                        TitledData(titleRes = R.string.notes, data = it?.notes ?: "")
                    }
                    AnimatedVisibility(visible = !it?.interests.isNullOrBlank() || !it?.books.isNullOrBlank() || !it?.music.isNullOrBlank() || !it?.movies.isNullOrBlank() || !it?.sports.isNullOrBlank()) {
                        Text(
                            stringResource(R.string.interests),
                            modifier = modifier.padding(
                                top = MARGIN_DEFAULT,
                                bottom = MARGIN_DEFAULT / 2
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AnimatedVisibility(visible = !it?.interests.isNullOrBlank()) {
                        TitledData(titleRes = R.string.interests, data = it?.interests ?: "")
                    }
                    AnimatedVisibility(visible = !it?.books.isNullOrBlank()) {
                        TitledData(titleRes = R.string.books, data = it?.books ?: "")
                    }
                    AnimatedVisibility(visible = !it?.movies.isNullOrBlank()) {
                        TitledData(titleRes = R.string.movies, data = it?.movies ?: "")
                    }
                    AnimatedVisibility(visible = !it?.music.isNullOrBlank()) {
                        TitledData(titleRes = R.string.music, data = it?.music ?: "")
                    }
                    AnimatedVisibility(visible = !it?.sports.isNullOrBlank()) {
                        TitledData(titleRes = R.string.sports, data = it?.sports ?: "")
                    }
                    AnimatedVisibility(visible = !it?.website.isNullOrBlank() || !it?.location.isNullOrBlank() || !it?.mailingAddress.isNullOrBlank() || !it?.phone.isNullOrBlank() || !it?.streams.isNullOrBlank()) {
                        Text(
                            stringResource(R.string.contacts),
                            modifier = modifier.padding(
                                top = MARGIN_DEFAULT,
                                bottom = MARGIN_DEFAULT / 2
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AnimatedVisibility(visible = !it?.website.isNullOrBlank()) {
                        TitledData(titleRes = R.string.website, data = it?.website ?: "")
                    }
                    AnimatedVisibility(visible = !it?.location.isNullOrBlank()) {
                        TitledData(titleRes = R.string.location, data = it?.location ?: "")
                    }
                    AnimatedVisibility(visible = !it?.mailingAddress.isNullOrBlank()) {
                        TitledData(titleRes = R.string.mailingAddress, data = it?.mailingAddress ?: "")
                    }
                    AnimatedVisibility(visible = !it?.phone.isNullOrBlank()) {
                        TitledData(titleRes = R.string.phone, data = it?.phone ?: "")
                    }
                    AnimatedVisibility(visible = !it?.streams.isNullOrBlank()) {
                        TitledData(titleRes = R.string.streams, data = it?.streams ?: "")
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
fun TitledData(modifier: Modifier = Modifier, titleRes: Int, data: String) {
    Column(modifier = modifier.padding(
        bottom = MARGIN_DEFAULT / 2
    )) {
        Text(
            stringResource(titleRes),
            style = typography.bodyMedium.copy(color = colorScheme.outlineVariant),
        )
        Text(data)
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