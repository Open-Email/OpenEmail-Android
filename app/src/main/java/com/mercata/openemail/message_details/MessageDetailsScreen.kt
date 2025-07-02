@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)

package com.mercata.openemail.message_details

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.openemail.ATTACHMENT_LIST_ITEM_HEIGHT
import com.mercata.openemail.DEFAULT_CORNER_RADIUS
import com.mercata.openemail.DEFAULT_DATE_TIME_FORMAT
import com.mercata.openemail.MARGIN_DEFAULT
import com.mercata.openemail.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.openemail.R
import com.mercata.openemail.common.ProfileImage
import com.mercata.openemail.contact_details.ContactType
import com.mercata.openemail.db.messages.DBMessageWithDBAttachments
import com.mercata.openemail.home_screen.HomeScreen
import com.mercata.openemail.message_details.AttachmentDownloadStatus.Downloaded
import com.mercata.openemail.message_details.AttachmentDownloadStatus.Downloading
import com.mercata.openemail.message_details.AttachmentDownloadStatus.NotDownloaded
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.Indefinite
import com.mercata.openemail.utils.getProfilePictureUrl
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt

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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.shared()
    }

    var contacts by remember { mutableStateOf(listOf<PublicUserData>()) }


    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    // this removes the heading‑break line under ALL heading levels
                    builder.headingBreakHeight(0)
                }
            })
            .build()
    }

    LaunchedEffect(state.message) {
        contacts = if (state.scope == HomeScreen.Outbox) {
            (state.message as? DBMessageWithDBAttachments)?.message?.readerAddresses?.split(",")
                ?.map { address ->
                    async {
                        when (val call = safeApiCall { getProfilePublicData(address) }) {
                            is HttpResult.Error -> null
                            is HttpResult.Success -> call.data
                        }
                    }
                }?.awaitAll()?.filterNotNull() ?: listOf()
        } else {
            state.message?.getContacts() ?: listOf()
        }
    }

    val subject = state.message?.getSubject() ?: ""

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

                modifier = modifier.shadow(elevation = 1.dp),
                title = {
                    AnimatedVisibility(
                        visible = scrollState.value != 0,
                        enter = fadeIn() + slideInVertically { 100.dp.value.roundToInt() },
                        exit = fadeOut() + slideOutVertically { 100.dp.value.roundToInt() }
                    ) {
                        Text(
                            subject,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = typography.labelMedium
                        )
                    }
                },
                actions = {
                    if (state.scope.deletable) {
                        OutlinedButton(
                            modifier = modifier.padding(end = MARGIN_DEFAULT),
                            onClick = {
                                if (!state.deleteConfirmationShown) {
                                    viewModel.toggleDeletionConfirmation(true)
                                }
                            }) {
                            Icon(
                                painterResource(R.drawable.delete),

                                tint = colorScheme.error,
                                contentDescription = stringResource(R.string.delete)
                            )
                        }
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
            )
        },
    ) { padding ->
        Box(modifier.fillMaxSize()) {
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
                        style = typography.titleLarge,
                    )
                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT * 1.5f))

                if (contacts.size <= 1) {
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(horizontal = MARGIN_DEFAULT)
                    ) {
                        ProfileImage(
                            modifier = modifier
                                .clip(CircleShape)
                                .size(MESSAGE_LIST_ITEM_IMAGE_SIZE),
                            imageUrl = state.message?.getAuthorAddressValue()
                                ?.getProfilePictureUrl()
                                ?: "",
                            onError = {
                                Box(
                                    modifier
                                        .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                                        .background(color = colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${state.currentUser?.name?.firstOrNull() ?: ""}${
                                            state.currentUser?.name?.getOrNull(
                                                1
                                            ) ?: ""
                                        }",
                                        style = typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                }
                            })

                        Spacer(modifier = modifier.width(MARGIN_DEFAULT * 0.75f))

                        contacts.firstOrNull()?.let { author ->
                            Column {
                                author.fullName.let { name ->
                                    Row {
                                        SelectionContainer {
                                            Text(
                                                text = name,
                                                maxLines = 1,
                                                style = typography.titleMedium,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        state.message?.getTimestamp()?.let { timestamp ->
                                            Spacer(modifier.weight(1f))
                                            SelectionContainer {
                                                Text(
                                                    text = ZonedDateTime.ofInstant(
                                                        Instant.ofEpochMilli(timestamp),
                                                        ZoneId.systemDefault()
                                                    ).format(DEFAULT_DATE_TIME_FORMAT),
                                                    maxLines = 1,
                                                    style = typography.bodySmall.copy(color = colorScheme.outlineVariant),
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                SelectionContainer {
                                    Text(
                                        text = author.address,
                                        maxLines = 1,
                                        style = typography.bodyMedium,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    MultiReadersView(
                        modifier = modifier
                            .padding(horizontal = MARGIN_DEFAULT),
                        readers = contacts,
                        navController = navController
                    )
                }

                Spacer(modifier = modifier.height(MARGIN_DEFAULT))

                if (state.scope != HomeScreen.Trash) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(
                            modifier = modifier.weight(1f),
                            thickness = 1.dp,
                            color = colorScheme.outline
                        )
                        OutlinedButton(onClick = {
                            navController.navigate("ComposingScreen/${state.message?.getAuthorAddressValue()}/${state.message?.getMessageId()}/null/null")
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painterResource(R.drawable.reply),
                                    contentDescription = stringResource(R.string.reply_button),
                                    tint = colorScheme.onSurface
                                )
                                Spacer(modifier.width(MARGIN_DEFAULT / 2))
                                Text(
                                    stringResource(R.string.reply_button),
                                    style = typography.labelLarge.copy(color = colorScheme.onSurface)
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = modifier.width(MARGIN_DEFAULT),
                            thickness = 1.dp,
                            color = colorScheme.outline
                        )
                    }
                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                AndroidView(
                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "message_body/${state.messageId}"
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor("#131B2A".toColorInt()) // onSurface color
                            setTextIsSelectable(true)
                        }
                    }
                ) { textView ->
                    markwon.setMarkdown(textView, state.message?.getTextBody() ?: "")
                }

                state.attachments.takeIf { it.isNotEmpty() }
                    ?.let { attachments ->
                        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                        HorizontalDivider(
                            modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                            thickness = 1.dp,
                            color = colorScheme.outline
                        )
                        Column {
                            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                            Text(
                                text = stringResource(id = R.string.attachments_label),
                                modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                                maxLines = 1,
                                style = typography.bodyMedium,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = modifier.height(MARGIN_DEFAULT * 3 / 4))
                            attachments.map { attachment ->
                                AttachmentViewHolder(
                                    modifier = modifier,
                                    attachment = attachment,
                                    viewModel = viewModel,
                                    context = context
                                )
                            }
                        }
                    }
            }

            if (state.deleteConfirmationShown) {
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
                        Text(text = stringResource(R.string.warning))
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.delete_message_question),
                            textAlign = TextAlign.Start
                        )
                    },
                    onDismissRequest = {
                        viewModel.toggleDeletionConfirmation(false)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.deleteMessage()
                                }.invokeOnCompletion {
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Text(
                                stringResource(id = R.string.delete),
                                color = colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.toggleDeletionConfirmation(false)
                            }
                        ) {
                            Text(stringResource(id = R.string.cancel_button))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiReadersView(
    modifier: Modifier = Modifier,
    readers: List<PublicUserData>,
    navController: NavController,
) {
    var expanded by remember { mutableStateOf(false) }
    val widthDp = animateDpAsState(
        targetValue = if (expanded) MARGIN_DEFAULT else -MESSAGE_LIST_ITEM_IMAGE_SIZE / 2,
    )
    val firstThreeReaders = if (readers.size > 5 && !expanded) readers.subList(0, 5) else readers
    FlowRow(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MARGIN_DEFAULT),
        horizontalArrangement = Arrangement.spacedBy(widthDp.value)
    ) {
        firstThreeReaders.forEach {
            ProfileImage(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        if (!expanded) {
                            expanded = true
                        } else {
                            navController.navigate(
                                "ContactDetailsScreen/${it.address}/${ContactType.SavedContact.id}"
                            )
                        }
                    }
                    .size(MESSAGE_LIST_ITEM_IMAGE_SIZE),
                imageUrl = it.address.getProfilePictureUrl(),
                onError = {
                    Box(
                        Modifier
                            .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                            .background(color = colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${it.fullName.firstOrNull() ?: ""}${
                                it.fullName.getOrNull(
                                    1
                                ) ?: ""
                            }",
                            style = typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                    }
                })
        }
    }
}

@Composable
fun AttachmentViewHolder(
    modifier: Modifier = Modifier,
    attachment: AttachmentDetails,
    context: Context,
    viewModel: MessageDetailsViewModel
) {
    val type = attachment.fileType.lowercase()
    val imageResource =
        if (type.contains("image")) {
            R.drawable.image
        } else if (type.contains("zip") || type.contains("rar")) {
            R.drawable.zip
        } else {
            R.drawable.file
        }

    Box(modifier.padding(horizontal = MARGIN_DEFAULT, vertical = MARGIN_DEFAULT / 4)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxSize()
                .height(ATTACHMENT_LIST_ITEM_HEIGHT)
                .background(colorScheme.surface)
                .border(
                    width = 1.dp, color = colorScheme.outline, shape = RoundedCornerShape(
                        DEFAULT_CORNER_RADIUS
                    )
                )
                .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                .clickable {
                    when (attachment.attachmentDownloadStatus) {
                        NotDownloaded -> viewModel.downloadFile(attachment)
                        Downloaded -> {
                            context.startActivity(viewModel.getOpenIntent(attachment))
                        }

                        Downloading -> {
                            //ignore
                        }
                    }

                }
                .padding(horizontal = MARGIN_DEFAULT / 2)
        ) {
            when (attachment.attachmentDownloadStatus) {
                NotDownloaded -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                            .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                            .background(colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            painter = painterResource(id = imageResource),
                            contentDescription = null,
                            tint = colorScheme.outlineVariant
                        )

                    }
                }

                Downloaded -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                            .clickable {
                                viewModel.share(attachment)
                            }
                            .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                            .background(colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = colorScheme.primary
                        )
                    }
                }

                Downloading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                            .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                            .background(colorScheme.surfaceVariant)
                    ) {
                        if (attachment.downloadStatus is Indefinite) {
                            CircularProgressIndicator(
                                modifier = modifier.size(MESSAGE_LIST_ITEM_IMAGE_SIZE / 2),
                                strokeCap = StrokeCap.Round
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = modifier.size(MESSAGE_LIST_ITEM_IMAGE_SIZE / 2),
                                strokeCap = StrokeCap.Round,
                                progress = { attachment.downloadStatus.percent!! / 100f }
                            )

                        }
                    }

                }
            }

            Spacer(modifier = modifier.width(MARGIN_DEFAULT))
            Column {
                Text(
                    text = attachment.name,
                    style = typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format(
                        stringResource(R.string.file_size_placeholder),
                        attachment.fileSize.toDouble() / (1024 * 1024)
                    ),
                    style = typography.bodySmall.copy(color = colorScheme.outlineVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

enum class AttachmentDownloadStatus {
    NotDownloaded,
    Downloaded,
    Downloading
}