@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks.contact_details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.DEFAULT_DATE_TIME_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.theme.bodyFontFamily
import com.mercata.pingworks.utils.getProfilePictureUrl
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun SharedTransitionScope.ContactDetailsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val imageSize = remember { 300.dp }

    Scaffold(
        modifier = modifier.sharedBounds(
            rememberSharedContentState(
                key = "contact_bounds/${state.address}"
            ),
            animatedVisibilityScope,
        ),
        floatingActionButton = {
            FloatingActionButton(
                modifier = modifier.sharedBounds(
                    rememberSharedContentState(
                        key = "composing_bounds"
                    ),
                    animatedVisibilityScope
                ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    navController.navigate("ComposingScreen/${state.address}/null/null")
                }) {
                Icon(Icons.Filled.Edit, stringResource(id = R.string.create_new_message))
            }
        }
    ) { padding ->
        Column(modifier.verticalScroll(rememberScrollState())) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "contact_image/${state.address}"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .height(imageSize)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                ProfileImage(
                    modifier,
                    state.contact?.address?.getProfilePictureUrl() ?: "",
                    onError = { modifier ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = modifier
                                .height(imageSize)
                                .padding(horizontal = MARGIN_DEFAULT)
                                .fillMaxWidth()

                        ) {
                            Text(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimary,
                                text = state.contact?.name ?: state.address,
                                style = MaterialTheme.typography.displayLarge
                            )
                        }
                    })

                state.contact?.address?.let {
                    Text(
                        text = it,
                        modifier = modifier.padding(MARGIN_DEFAULT),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleBroadcast() }
                    .padding(
                        top = MARGIN_DEFAULT,
                        start = MARGIN_DEFAULT,
                        end = MARGIN_DEFAULT,
                        bottom = MARGIN_DEFAULT / 2
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cast),
                    contentDescription = stringResource(
                        id = R.string.receive_broadcasts
                    ),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = modifier.width(MARGIN_DEFAULT))
                Text(
                    stringResource(id = R.string.receive_broadcasts),
                    fontFamily = bodyFontFamily,
                    softWrap = true
                )
                Spacer(modifier = modifier.weight(1f))
                Switch(
                    checked = state.contact?.receiveBroadcasts ?: false,
                    onCheckedChange = { viewModel.toggleBroadcast() })
            }
            state.contact?.run {
                Column (modifier.padding(MARGIN_DEFAULT)){
                    lastSeen?.let {
                        Text(
                            "${stringResource(R.string.last_seen)}: ${
                                ZonedDateTime.ofInstant(
                                    Instant.parse(it), ZoneId.systemDefault()
                                ).format(DEFAULT_DATE_TIME_FORMAT)
                            }",
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
            Spacer(modifier = modifier.height(padding.calculateBottomPadding()))
        }
    }
}