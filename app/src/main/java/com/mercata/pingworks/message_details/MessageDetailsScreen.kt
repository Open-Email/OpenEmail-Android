@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks.message_details

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.ATTACHMENT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.CONTACT_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.DEFAULT_DATE_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.composing_screen.AddressChip
import com.mercata.pingworks.db.messages.FusedAttachment
import com.mercata.pingworks.message_details.AttachmentDownloadStatus.Downloaded
import com.mercata.pingworks.message_details.AttachmentDownloadStatus.Downloading
import com.mercata.pingworks.message_details.AttachmentDownloadStatus.NotDownloaded
import com.mercata.pingworks.utils.Indefinite
import com.mercata.pingworks.utils.getProfilePictureUrl
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@ExperimentalLayoutApi
@Composable
fun SharedTransitionScope.MessageDetailsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: MessageDetailsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.shared()
    }

    val messageWithAttachments = state.message
    val messageWithAuthor = messageWithAttachments?.message
    val message = messageWithAuthor?.message
    val outbox: Boolean = state.outboxAddresses != null

    val date: String = message?.timestamp?.let {
        ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(it), ZoneId.systemDefault()
        ).format(DEFAULT_DATE_FORMAT)
    } ?: ""

    val subject = message?.subject ?: ""

    LaunchedEffect(state.shareIntent) {
        if (state.shareIntent != null) {
            val shareIntent = Intent.createChooser(state.shareIntent, null)
            launcher.launch(shareIntent)
        }
    }

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
                actions = {
                    if (!state.noReply) {
                        IconButton(onClick = {
                            navController.navigate("ComposingScreen/${state.message?.message?.message?.authorAddress}/${state.message?.getMessageId()}/null")
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.reply),
                                contentDescription = stringResource(R.string.reply_button)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
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
                .padding(vertical = MARGIN_DEFAULT)
        ) {
            SelectionContainer {
                Text(
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "message_subject/${state.messageId}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    text = subject,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))

            if (outbox) {
                Column {
                    SelectionContainer(modifier = modifier.padding(horizontal = MARGIN_DEFAULT)) {
                        Text(date)
                    }
                    FlowRow(modifier = modifier.padding(horizontal = MARGIN_DEFAULT/2)) {
                        state.outboxAddresses!!.map {
                            AddressChip(modifier = modifier, user = it, onClick = { user ->
                                navController.navigate(
                                    "ContactDetailsScreen/${user.address}"
                                )
                            })
                        }
                    }
                }
            } else {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARGIN_DEFAULT)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                            .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {

                        ProfileImage(
                            modifier = modifier,
                            imageUrl =  messageWithAuthor?.author?.address?.getProfilePictureUrl() ?: "",
                            onError = {
                                Text(
                                    text = "${
                                        messageWithAuthor?.author?.name?.firstOrNull() ?: messageWithAuthor?.author?.address?.first()
                                    }",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            })
                    }

                    Spacer(modifier = modifier.width(MARGIN_DEFAULT))

                    messageWithAuthor?.author?.let { author ->
                        Column {
                            author.name?.let { name ->
                                SelectionContainer {
                                    Text(
                                        text = name,
                                        maxLines = 2,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = author.address,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.bodyMedium,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier.weight(1f))
                    SelectionContainer {
                        Text(date)
                    }

                }
            }
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            SelectionContainer {
                Text(
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "message_body/${state.messageId}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    text = message?.textBody ?: ""
                )
            }
            messageWithAttachments?.getAttachments()?.takeIf { it.isNotEmpty() }
                ?.let { attachments ->
                    Column {
                        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                        Text(
                            text = stringResource(id = R.string.attachments_label),
                            modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                        )
                        attachments.map { attachment ->
                            AttachmentViewHolder(
                                modifier = modifier,
                                attachment = attachment,
                                viewModel = viewModel,
                                state = state,
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun AttachmentViewHolder(
    modifier: Modifier = Modifier,
    attachment: FusedAttachment,
    state: MessageDetailsState,
    viewModel: MessageDetailsViewModel
) {
    val type = attachment.fileType.lowercase()
    val imageResource = if (type.contains("pdf")) {
        R.drawable.pdf
    } else if (type.contains("image")) {
        R.drawable.image
    } else if (type.contains("audio")) {
        R.drawable.sound
    } else if (type.contains("video")) {
        R.drawable.video
    } else {
        R.drawable.file
    }

    val status = if (state.attachmentsWithStatus[attachment] == null) {
        NotDownloaded
    } else {
        if (state.attachmentsWithStatus[attachment]!!.file == null) {
            Downloading
        } else {
            Downloaded
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .height(ATTACHMENT_LIST_ITEM_HEIGHT)
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                when (status) {
                    NotDownloaded -> viewModel.downloadFile(attachment)
                    Downloaded -> {
                        viewModel.share(attachment)
                    }

                    Downloading -> {
                        //ignore
                    }
                }

            }
            .padding(horizontal = MARGIN_DEFAULT)
    ) {
        when (status) {
            NotDownloaded -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .clip(CircleShape)
                        .size(CONTACT_LIST_ITEM_IMAGE_SIZE)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(id = imageResource),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Downloaded -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .clip(CircleShape)
                        .size(CONTACT_LIST_ITEM_IMAGE_SIZE)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Downloading -> {
                if (state.attachmentsWithStatus[attachment]!!.status is Indefinite) {
                    CircularProgressIndicator(modifier = modifier.size(CONTACT_LIST_ITEM_IMAGE_SIZE))
                } else {
                    CircularProgressIndicator(
                        modifier = modifier.size(CONTACT_LIST_ITEM_IMAGE_SIZE),
                        progress = { state.attachmentsWithStatus[attachment]!!.status.percent!! / 100f })
                }
            }
        }

        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
        Text(
            text = attachment.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class AttachmentDownloadStatus {
    NotDownloaded,
    Downloaded,
    Downloading
}