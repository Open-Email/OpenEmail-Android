@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks.message_details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mercata.pingworks.CONTACT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.DEFAULT_DATE_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@Composable
fun SharedTransitionScope.MessageDetailsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: MessageDetailsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    val messageWithAttachments = state.message
    val messageWithAuthor = messageWithAttachments?.message
    val message = messageWithAuthor?.message

    val date: String = message?.timestamp?.let {
        ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(it), ZoneId.systemDefault()
        ).format(DEFAULT_DATE_FORMAT)
    } ?: ""

    val subject = message?.subject ?: ""

    Scaffold(
        modifier = modifier
            .sharedBounds(
                rememberSharedContentState(
                    key = "message_bounds/${state.messageId}"
                ),
                animatedVisibilityScope,
            ),
        topBar = {
            TopAppBar(
                modifier = modifier.shadow(elevation = if (scrollState.value == 0) 0.dp else 16.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (scrollState.value == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    AnimatedVisibility(visible = scrollState.value != 0,
                        enter = fadeIn() + slideInVertically { 100.dp.value.roundToInt() },
                        exit = fadeOut() + slideOutVertically { 100.dp.value.roundToInt() }
                    ) {
                        Text(
                            subject,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(MARGIN_DEFAULT)
        ) {
            Text(
                text = subject,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))

            Row(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "message_image/${state.messageId}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                        .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    if (messageWithAuthor?.author?.imageUrl == null) {
                        Text(
                            text = "${
                                if (messageWithAuthor?.author == null) {
                                    //outbox message
                                    state.currentUser?.name?.first() ?: ""
                                } else {
                                    messageWithAuthor.author.name?.firstOrNull() ?: messageWithAuthor.author.address.first()
                                }
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        AsyncImage(
                            contentScale = ContentScale.Crop,
                            modifier = modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        key = "message_image/${state.messageId}"
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                                .size(CONTACT_LIST_ITEM_HEIGHT)
                                .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS)),
                            model = messageWithAuthor.author.imageUrl,
                            contentDescription = stringResource(id = R.string.profile_image)
                        )
                    }
                }
                Spacer(modifier = modifier.width(MARGIN_DEFAULT))
                messageWithAuthor?.author?.let { author ->
                    Column {
                        author.name?.let { name ->
                            Text(
                                text = name,
                                maxLines = 2,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = author.address,
                            maxLines = 2,
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier.weight(1f))

                Text(date)
            }
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            Text(text = message?.textBody ?: "")
        }
    }
}