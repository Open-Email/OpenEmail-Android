@file:OptIn(
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.mercata.pingworks.composing_screen

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
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.ui.focus.onFocusChanged
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
import com.mercata.pingworks.ATTACHMENT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.CHIP_HEIGHT
import com.mercata.pingworks.CHIP_ICON_SIZE
import com.mercata.pingworks.CLEAR_ICON_SIZE
import com.mercata.pingworks.CONTACT_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.DEFAULT_DATE_TIME_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.home_screen.MessageViewHolder
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.utils.getMimeType
import com.mercata.pingworks.utils.getNameFromURI
import com.mercata.pingworks.utils.getProfilePictureUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
    val toFocusRequester = remember { FocusRequester() }
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

    BackHandler(enabled = true) {
        viewModel.confirmExit()
    }

    LaunchedEffect(state.sent) {
        if (state.sent) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(state.replyMessage) {
        if (state.replyMessage != null) {

            val time: String? = state.replyMessage?.message?.timestamp?.let {
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(it), ZoneId.systemDefault()
                ).format(DEFAULT_DATE_TIME_FORMAT)
            }

            val reply = String.format(
                context.getString(R.string.reply_header),
                time,
                state.replyMessage?.author?.name,
                state.replyMessage?.message?.textBody,
                state.replyMessage?.message?.subject
            )

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
                                text = stringResource(id = R.string.create_new_message),
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
                        viewModel.confirmExit()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.back_button),
                            tint = colorScheme.onSurface
                        )
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
                            .padding(vertical = MARGIN_DEFAULT)

                    ) {
                        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
                        Switch(
                            checked = state.broadcast,
                            onCheckedChange = { viewModel.toggleBroadcast() })
                        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
                        Text(
                            stringResource(id = R.string.broadcast_title),
                            style = typography.labelLarge,
                            softWrap = true
                        )
                        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
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
                        modifier = modifier.padding(horizontal = MARGIN_DEFAULT * 3 / 4),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        state.recipients.map { user ->
                            AddressChip(
                                modifier = modifier,
                                user = user,
                                onClick = { clickedUser ->
                                    viewModel.openUserDetails(clickedUser)
                                },
                                onDismiss = { clickedUser ->
                                    viewModel.removeRecipient(clickedUser)
                                })
                        }
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    OutlinedTextField(
                        value = state.addressFieldText,
                        prefix = if (state.mode == ComposingScreenMode.Default) {
                            null
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    modifier = modifier.clickable {
                                        viewModel.clearAddressField()
                                        focusManager.clearFocus()
                                    },
                                    contentDescription = stringResource(
                                        id = R.string.add_recipient
                                    ),
                                    tint = colorScheme.primary
                                )
                            }
                        },
                        suffix = {
                            if (state.addressFieldText.isNotBlank()) {
                                if (state.addressLoading) {
                                    CircularProgressIndicator(
                                        strokeCap = StrokeCap.Round,
                                        modifier = modifier.size(
                                            CHIP_ICON_SIZE
                                        )
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        modifier = modifier.clickable {
                                            viewModel.attemptToAddAddress()
                                        },
                                        contentDescription = stringResource(
                                            id = R.string.add_recipient
                                        ),
                                        tint = colorScheme.primary
                                    )
                                }
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
                            Text(stringResource(id = R.string.to_placeholder))
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
                            .focusRequester(toFocusRequester)
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    viewModel.attemptToAddAddress()
                                }
                                viewModel.toggleMode(focusState.isFocused)
                            })
                }
            }
            AnimatedVisibility(visible = !state.broadcast) {
                Spacer(modifier = modifier.height(MARGIN_DEFAULT / 2))
            }
            AnimatedVisibility(visible = state.mode == ComposingScreenMode.Default) {
                OutlinedTextField(
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
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            }
            AnimatedVisibility(visible = state.mode == ComposingScreenMode.Default) {
                OutlinedTextField(
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
                    state.contacts.filter {
                        it.name?.contains(
                            state.addressFieldText,
                            true
                        ) == true || it.address.contains(state.addressFieldText, true)
                    }.forEach { contact ->
                        MessageViewHolder(
                            modifier = modifier,
                            animatedVisibilityScope = animatedVisibilityScope,
                            item = contact,
                            isSelected = false,
                            onMessageClicked = { person ->
                                viewModel.addContactSuggestion(person as DBContact)
                                viewModel.clearAddressField()
                                focusManager.clearFocus()
                            },
                            onMessageSelected = null,
                            currentUser = state.currentUser!!
                        )
                    }
                }
            }

            Spacer(modifier = modifier.height(MARGIN_DEFAULT + 56.dp + padding.calculateBottomPadding()))
        }

        if (state.openedAddressDetails != null) {
            AlertDialog(
                onDismissRequest = { viewModel.closeUserDetails() },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.closeUserDetails()
                    }) {
                        Text(text = stringResource(id = R.string.cancel_button))
                    }
                },
                icon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .clip(CircleShape)
                            .size(MESSAGE_LIST_ITEM_IMAGE_SIZE)
                    ) {
                        ProfileImage(
                            modifier = modifier,
                            imageUrl = state.openedAddressDetails?.address?.getProfilePictureUrl()
                                ?: "",
                            onError = {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = stringResource(id = R.string.profile_image)
                                )
                            })
                    }
                },
                title = {
                    Text(text = state.openedAddressDetails!!.fullName)
                },
                text = {
                    Text(text = state.openedAddressDetails!!.address)
                }
            )
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

    Box(modifier.padding(MARGIN_DEFAULT / 4)) {
        Box(
            modifier = modifier
                .height(CHIP_HEIGHT)
                .clip(shape = CircleShape)
                .clickable { onClick(user) }
                .border(1.dp, color = colorScheme.onSurface, shape = CircleShape),
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
                Text(text = user.fullName, style = typography.bodySmall)
                Spacer(modifier = modifier.width(MARGIN_DEFAULT / 4))
                if (onDismiss != null) {
                    Icon(
                        modifier = modifier.clickable {
                            onDismiss(user)
                        },
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(id = R.string.dismiss_address)
                    )
                }
                Spacer(modifier = modifier.width(MARGIN_DEFAULT / 2))
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