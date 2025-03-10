@file:OptIn(
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.mercata.openemail.composing_screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.openemail.ATTACHMENT_LIST_ITEM_HEIGHT
import com.mercata.openemail.CHIP_HEIGHT
import com.mercata.openemail.CHIP_ICON_SIZE
import com.mercata.openemail.CLEAR_ICON_SIZE
import com.mercata.openemail.CONTACT_LIST_ITEM_IMAGE_SIZE
import com.mercata.openemail.DEFAULT_CORNER_RADIUS
import com.mercata.openemail.DEFAULT_DATE_TIME_FORMAT
import com.mercata.openemail.MARGIN_DEFAULT
import com.mercata.openemail.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.openemail.R
import com.mercata.openemail.common.ProfileImage
import com.mercata.openemail.contact_details.ContactType
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.utils.getMimeType
import com.mercata.openemail.utils.getNameFromURI
import com.mercata.openemail.utils.getProfilePictureUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SharedTransitionScope.ComposingScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ComposingViewModel = viewModel()
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    //val toFocusRequester = remember { FocusRequester() }
    val subjectFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    val documentChooserLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            viewModel.addAttachments(it)
        }

    val photoSnapLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            viewModel.addInstantPhotoAsAttachment(it)
        }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.snackbarErrorResId) {
        state.snackbarErrorResId?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(it),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    fun backAction() {
        when (state.mode) {
            ComposingScreenMode.Default -> viewModel.confirmExit()
            ComposingScreenMode.ContactSuggestion -> viewModel.toggleMode(false)
        }
    }

    BackHandler(enabled = true) {
        backAction()
    }

    LaunchedEffect(state.sent) {
        if (state.sent) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(state.replyMessage) {
        if (state.replyMessage != null) {

            val time: String? = state.replyMessage?.timestamp?.let {
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(it), ZoneId.systemDefault()
                ).format(DEFAULT_DATE_TIME_FORMAT)
            }

            val reply = String.format(
                context.getString(R.string.reply_header),
                time,
                state.replyMessage?.getAuthorPublicData(),
                state.replyMessage?.textBody,
                state.replyMessage?.subject
            )

            viewModel.updateSubject(state.replyMessage?.subject ?: "")
            viewModel.updateBody(reply)
            viewModel.consumeReplyData()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier.sharedBounds(
            rememberSharedContentState(
                key = "composing_bounds"
            ),
            animatedVisibilityScope,
        ),
        topBar = {
            TopAppBar(
                modifier = modifier.shadow(1.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface,
                ),
                title = {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = modifier.size(28.dp),
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        Column {
                            Text(
                                text = stringResource(id = R.string.create_message),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.currentUser?.address ?: "",
                                maxLines = 1,
                                style = typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        backAction()
                    }) {
                        when(state.mode) {
                            ComposingScreenMode.Default -> Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.back_button),
                                tint = colorScheme.onSurface
                            )
                            ComposingScreenMode.ContactSuggestion -> Icon(
                                painter = painterResource(R.drawable.back),
                                contentDescription = stringResource(id = R.string.back_button),
                                tint = colorScheme.onSurface
                            )
                        }

                    }
                },
                actions = {
                    AnimatedVisibility(visible = state.mode == ComposingScreenMode.Default) {
                        Button(modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                            enabled = !state.loading,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.send()
                            }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painterResource(R.drawable.send),
                                    contentDescription = stringResource(id = R.string.send),
                                    tint = colorScheme.onPrimary
                                )
                                Spacer(modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.send),
                                    style = typography.labelLarge.copy(color = colorScheme.onPrimary)
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.mode == ComposingScreenMode.Default) {
                FloatingActionButton(
                    modifier = modifier.imePadding(),
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    onClick = {
                        viewModel.toggleAttachmentBottomSheet(true)
                    }) {
                    Icon(
                        painter = painterResource(id = R.drawable.attach),
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(
                    start = padding.calculateLeftPadding(LocalLayoutDirection.current),
                    end = padding.calculateRightPadding(LocalLayoutDirection.current),
                    top = padding.calculateTopPadding()
                )
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(visible = state.mode == ComposingScreenMode.Default) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleBroadcast() }
                            .padding(horizontal = MARGIN_DEFAULT, vertical = MARGIN_DEFAULT / 2)

                    ) {
                        Switch(
                            checked = state.broadcast,
                            onCheckedChange = { viewModel.toggleBroadcast() })
                        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
                        Text(
                            stringResource(id = R.string.broadcast_title),
                            style = typography.labelLarge,
                            softWrap = true
                        )
                    }
                    HorizontalDivider(
                        modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                        thickness = 1.dp,
                        color = colorScheme.outline
                    )
                }
            }

            AnimatedVisibility(visible = !state.broadcast) {
                Column {
                    FlowRow(
                        modifier = modifier
                            .clickable {
                                viewModel.toggleMode(true)
                            }
                            .fillMaxWidth()
                            .padding(MARGIN_DEFAULT),
                        verticalArrangement = Arrangement.spacedBy(MARGIN_DEFAULT / 2),
                        horizontalArrangement = Arrangement.spacedBy(MARGIN_DEFAULT / 2),
                    ) {
                        Row(
                            modifier = modifier
                                .height(CHIP_HEIGHT), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.contacts),
                                modifier = Modifier.size(24.dp),
                                contentDescription = null,
                                tint = colorScheme.outlineVariant
                            )
                            Spacer(modifier = modifier.width(MARGIN_DEFAULT / 4))
                            Text(
                                stringResource(R.string.readers),
                                style = typography.titleSmall.copy(color = colorScheme.outlineVariant)
                            )
                        }
                        state.recipients.map { user ->
                            AddressChip(
                                modifier = modifier,
                                user = user,
                                onClick = { clickedUser ->
                                    navController.navigate(
                                        "ContactDetailsScreen/${clickedUser.address}/${ContactType.DetailsOnly.id}"
                                    )
                                },
                                onDismiss = { clickedUser ->
                                    viewModel.removeRecipient(clickedUser)
                                })
                        }
                    }
                    AnimatedVisibility(state.mode == ComposingScreenMode.ContactSuggestion) {
                        OutlinedTextField(
                            value = state.addressFieldText,
                            shape = CircleShape,
                            suffix = {
                                val localModifier = modifier.size(22.dp).clickable {
                                    viewModel.clearAddressField()
                                    focusManager.clearFocus()
                                    viewModel.toggleMode(false)
                                }
                                if (state.loading) {
                                    CircularProgressIndicator(modifier = localModifier, strokeCap = StrokeCap.Round)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        modifier = localModifier,
                                        contentDescription = stringResource(
                                            id = R.string.add_recipient
                                        ),
                                        tint = colorScheme.primary
                                    )
                                }

                            },
                            onValueChange = { str -> viewModel.updateTo(str) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                                showKeyboardOnFocus = true,
                            ),
                            isError = state.addressErrorResId != null,
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            label = {
                                Text(stringResource(id = R.string.search_contacts))
                            },
                            supportingText = {
                                state.addressErrorResId?.let {
                                    Text(
                                        text = stringResource(id = it),
                                        color = colorScheme.error
                                    )
                                }
                            },
                            modifier = modifier
                                .padding(horizontal = MARGIN_DEFAULT)
                                //.focusRequester(toFocusRequester)
                                .fillMaxWidth()
                            /*.onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    viewModel.attemptToAddAddress()
                                }
                                viewModel.toggleMode(focusState.isFocused)
                            }*/
                        )
                    }

                }
            }
            AnimatedVisibility(visible = state.mode == ComposingScreenMode.Default) {
                OutlinedTextField(
                    shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
                    value = state.subject,
                    onValueChange = { str -> viewModel.updateSubject(str) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        showKeyboardOnFocus = true,
                    ),
                    isError = state.subjectErrorResId != null,
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    ),
                    label = {
                        Text(stringResource(id = R.string.subject_placeholder))
                    },
                    supportingText = {
                        state.subjectErrorResId?.let {
                            Text(
                                text = stringResource(id = it),
                                color = colorScheme.error
                            )
                        }
                    },
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .focusRequester(subjectFocusRequester)
                        .fillMaxWidth()
                )
            }
            AnimatedVisibility(visible = state.mode == ComposingScreenMode.Default) {
                OutlinedTextField(
                    shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.None,
                        showKeyboardOnFocus = true,
                    ),
                    placeholder = {
                        Text(text = stringResource(id = R.string.body_placeholder))
                    },
                    isError = state.bodyErrorResId != null,
                    supportingText = {
                        state.bodyErrorResId?.let {
                            Text(
                                text = stringResource(id = it),
                                color = colorScheme.error
                            )
                        }
                    },
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .defaultMinSize(minHeight = 200.dp)
                        .fillMaxWidth()
                        .focusRequester(bodyFocusRequester),
                    value = state.body,
                    onValueChange = { str ->
                        viewModel.updateBody(str)
                    })
            }
            AnimatedVisibility(visible = state.mode == ComposingScreenMode.Default) {
                Column {
                    state.attachments.map { attachmentUri ->
                        AttachmentViewHolder(
                            modifier = modifier,
                            attachment = attachmentUri,
                            viewModel = viewModel
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state.mode == ComposingScreenMode.ContactSuggestion) {
                Column {
                    (state.contacts + state.externalContact).filter {
                        it != null && (it.fullName.contains(
                            state.addressFieldText,
                            true
                        ) || it.address.contains(state.addressFieldText, true))
                    }.forEach { contact ->
                        NewContactViewHolder (
                            modifier = modifier,
                            item = contact!!,
                            onMessageClicked = { person ->
                                viewModel.addContactSuggestion(person)
                                viewModel.clearAddressField()
                                focusManager.clearFocus()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = modifier.height(MARGIN_DEFAULT + 56.dp + padding.calculateBottomPadding()))
        }

        if (state.confirmExitDialogShown) {
            AlertDialog(
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
                title = {
                    Text(
                        text = stringResource(id = R.string.warning), textAlign = TextAlign.Start,
                        style = typography.titleMedium
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = stringResource(id = R.string.exit_composing_confirmation),
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                        Button(modifier = modifier.fillMaxWidth(), onClick = {
                            viewModel.closeExitConfirmation()
                            navController.popBackStack()
                        }) {
                            Text(stringResource(id = R.string.save_as_draft))
                        }
                        OutlinedButton(modifier = modifier.fillMaxWidth(), onClick = {
                            coroutineScope.launch {
                                viewModel.deleteDraft()
                                withContext(Dispatchers.Main) {
                                    viewModel.closeExitConfirmation()
                                    navController.popBackStack()
                                }
                            }
                        }) {
                            Text(stringResource(id = R.string.close_without_saving))
                        }
                    }
                },

                onDismissRequest = {
                    viewModel.closeExitConfirmation()
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.closeExitConfirmation()
                        }
                    ) {
                        Text(stringResource(id = R.string.cancel_button))
                    }
                },
            )
        }
        if (state.attachmentBottomSheetShown) {
            ModalBottomSheet(
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
                onDismissRequest = {
                    viewModel.toggleAttachmentBottomSheet(false)
                },
                sheetState = sheetState
            ) {
                // Sheet content
                OutlinedButton(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARGIN_DEFAULT),
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                viewModel.toggleAttachmentBottomSheet(false)
                            }
                        }
                        documentChooserLauncher.launch(arrayOf("*/*"))
                    }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.file),
                            contentDescription = stringResource(R.string.attach_file),
                            tint = colorScheme.primary
                        )
                        Spacer(modifier.width(MARGIN_DEFAULT))
                        Text(stringResource(R.string.attach_file))
                    }
                }
                Spacer(modifier.height(MARGIN_DEFAULT / 2))
                OutlinedButton(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARGIN_DEFAULT),
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                viewModel.toggleAttachmentBottomSheet(false)
                            }
                        }
                        photoSnapLauncher.launch(viewModel.getNewFileUri())
                    }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.camera),
                            contentDescription = stringResource(R.string.add_instant_photo),
                            tint = colorScheme.primary
                        )
                        Spacer(modifier.width(MARGIN_DEFAULT))
                        Text(stringResource(R.string.add_instant_photo))
                    }
                }
                Spacer(modifier.height(MARGIN_DEFAULT))

            }
        }
    }
}

@Composable
fun AddressChip(
    modifier: Modifier = Modifier,
    user: PublicUserData,
    onClick: (address: PublicUserData) -> Unit,
    onDismiss: ((address: PublicUserData) -> Unit)? = null
) {
    Box {
        Box(
            modifier = modifier
                .height(CHIP_HEIGHT)
                .clip(shape = CircleShape)
                .clickable { onClick(user) }
                .border(1.dp, color = colorScheme.outline, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = modifier.padding(start = MARGIN_DEFAULT / 4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .clip(CircleShape)
                        .size(CHIP_ICON_SIZE)
                ) {
                    ProfileImage(
                        modifier,
                        user.address.getProfilePictureUrl(),
                        onError = {
                            Icon(
                                Icons.Default.AccountCircle,
                                modifier = modifier,
                                contentDescription = stringResource(id = R.string.profile_image)
                            )
                        })
                }
                Spacer(modifier = modifier.width(MARGIN_DEFAULT / 4))
                Text(
                    text = user.fullName,
                    style = typography.bodySmall.copy(color = colorScheme.onSurface)
                )
                Spacer(modifier = modifier.width(MARGIN_DEFAULT / 4))
                if (onDismiss != null) {
                    Icon(
                        modifier = modifier
                            .size(18.dp)
                            .clickable {
                                onDismiss(user)
                            },
                        imageVector = Icons.Default.Clear,
                        tint = colorScheme.onSurface,
                        contentDescription = stringResource(id = R.string.dismiss_address)
                    )
                }
                Spacer(modifier = modifier.width(MARGIN_DEFAULT / 2))
            }
        }
        Box(
            modifier = modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (user.away == true) colorScheme.error else colorScheme.secondary)

        )
    }
}

@Composable
fun NewContactViewHolder(
    item: PublicUserData,
    modifier: Modifier = Modifier,
    onMessageClicked: (message: PublicUserData) -> Unit,
) {

    Box {
        Row(verticalAlignment = Alignment.Top, modifier = modifier
            .background(color = colorScheme.surface)
            //.height(MESSAGE_LIST_ITEM_HEIGHT)
            .fillMaxWidth()
            .clickable {
                onMessageClicked(item)
            }
            .padding(MARGIN_DEFAULT)

        ) {
            Box(contentAlignment = Alignment.TopStart) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .border(width = 1.dp, color = colorScheme.outline, shape = CircleShape)
                        .clip(CircleShape)
                        .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                        .background(colorScheme.surface)
                ) {
                    ProfileImage(
                        modifier
                            .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                            .clip(CircleShape),
                        item.address.getProfilePictureUrl(),
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
                                    text = "${
                                        item.fullName.firstOrNull() ?: ""
                                    }${
                                        item.fullName.getOrNull(1) ?: ""
                                    }",
                                    style = typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                            }
                        })
                }
            }
            Spacer(modifier = modifier.width(MARGIN_DEFAULT))
            Column(modifier = modifier.weight(1f)) {
                Row {
                    if (item.fullName.isNotEmpty()) {
                        Text(
                            text = item.fullName,
                            modifier = modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = typography.titleMedium,
                        )
                    } else {
                        Spacer(modifier = modifier.weight(1f))
                    }
                    item.lastSeen?.let { instant ->
                        val current = LocalDateTime.now()
                        val localDateTime = LocalDateTime.ofInstant(
                            instant,
                            ZoneId.systemDefault()
                        )
                        val today =
                            current.year == localDateTime.year && current.dayOfYear == localDateTime.dayOfYear
                        val formatter =
                            if (today) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofPattern(
                                "dd.MM.yyyy"
                            )
                        Text(
                            String.format(
                                stringResource(R.string.last_seen_placeholder),
                                formatter.format(localDateTime)
                            ),
                            modifier = modifier.padding(start = MARGIN_DEFAULT),
                            style = typography.titleSmall.copy(color = colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier.height(MARGIN_DEFAULT / 2))
                Text(
                    text = item.address,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = typography.titleSmall,
                )
            }
        }
    }
}

@Composable
fun AttachmentViewHolder(
    modifier: Modifier = Modifier,
    attachment: Uri,
    viewModel: ComposingViewModel
) {
    val context = LocalContext.current
    val type = attachment.getMimeType(context) ?: ""
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .height(ATTACHMENT_LIST_ITEM_HEIGHT)
            .background(colorScheme.surface)
            .padding(horizontal = MARGIN_DEFAULT)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(CircleShape)
                .size(CONTACT_LIST_ITEM_IMAGE_SIZE)
                .background(colorScheme.primary)
        ) {
            Icon(
                painter = painterResource(id = imageResource),
                contentDescription = null,
                tint = colorScheme.onPrimary
            )
        }
        Text(
            modifier = modifier
                .weight(1f)
                .padding(horizontal = MARGIN_DEFAULT),
            text = attachment.getNameFromURI(context) ?: "",
            style = typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = { viewModel.removeAttachment(attachment) }) {
            Icon(
                modifier = Modifier.size(CLEAR_ICON_SIZE),
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(id = R.string.clear_button)
            )
        }
    }
}