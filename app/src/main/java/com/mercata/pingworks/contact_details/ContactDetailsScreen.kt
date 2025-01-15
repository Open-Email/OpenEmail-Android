@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.mercata.pingworks.contact_details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.DEFAULT_DATE_TIME_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.utils.getProfilePictureUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    Scaffold(
        modifier = modifier.sharedBounds(
            rememberSharedContentState(
                key = "message_bounds/${state.address}"
            ),
            animatedVisibilityScope,
        ),
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.profile))
            },
                navigationIcon = {
                    IconButton(content = {
                        Icon(
                            painterResource(R.drawable.back),
                            contentDescription = stringResource(R.string.back_button),
                            //tint = colorScheme.onSurface
                        )
                    }, onClick = {
                        navController.popBackStack()
                    })
                },
                actions = {
                    if (state.isNotification) {
                        Button (enabled = !state.loading, onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                viewModel.approveRequest()
                            }
                        }) {
                            Text(stringResource(R.string.add_contact))
                        }
                    }
                })
        },
        floatingActionButton = {
            if (!state.loading) {
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
                        if (state.isNotification) {

                            //TODO approve auto adding to contacts dialog
                            coroutineScope.launch(Dispatchers.IO) {
                                viewModel.approveRequest()
                            }
                        }
                        navController.navigate("ComposingScreen/${state.address}/null/null")
                    }) {
                    Row {
                        Icon(Icons.Filled.Edit, stringResource(id = R.string.create_new_message))
                        Spacer(modifier.width(MARGIN_DEFAULT / 2))
                        Text(
                            stringResource(R.string.create_message),
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
                    )*/
                ,
                state.address.getProfilePictureUrl() ?: "",
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
            Spacer(modifier.height(MARGIN_DEFAULT / 2))
            if (!state.isNotification) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleBroadcast() }
                        .padding(
                            horizontal = MARGIN_DEFAULT,
                        )
                ) {
                    Text(
                        stringResource(id = R.string.receive_broadcasts),
                        style = typography.titleMedium,
                        softWrap = true
                    )
                    Spacer(modifier = modifier.weight(1f))
                    Switch(
                        checked = state.dbContact?.receiveBroadcasts ?: false,
                        onCheckedChange = { viewModel.toggleBroadcast() })
                }
                Spacer(modifier.height(MARGIN_DEFAULT / 2))
                ContactDivider(modifier.padding(horizontal = MARGIN_DEFAULT))
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
    Divider(
        modifier
            .fillMaxWidth()
            .height(1.dp),
        thickness = 1.dp,
        color = colorScheme.outline
    )
}