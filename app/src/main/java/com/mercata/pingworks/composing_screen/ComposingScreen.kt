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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mercata.pingworks.ATTACHMENT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.CHIP_HEIGHT
import com.mercata.pingworks.CHIP_ICON_SIZE
import com.mercata.pingworks.CONTACT_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.DEFAULT_DATE_TIME_FORMAT
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.MESSAGE_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.R
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.theme.bodyFontFamily
import com.mercata.pingworks.utils.getNameFromURI
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val toFocusRequester = remember { FocusRequester() }
    val subjectFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            viewModel.addAttachments(it)
        }

    val state by viewModel.state.collectAsState()

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

    Box {
        Scaffold(
            modifier = modifier.sharedBounds(
                rememberSharedContentState(
                    key = "composing_bounds"
                ),
                animatedVisibilityScope,
            ),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.create_new_message),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.currentUser?.address ?: "",
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.back_button),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(enabled = (!state.loading && state.recipients.isNotEmpty()) || state.broadcast,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.send()
                            }) {
                            if (state.loading) {
                                CircularProgressIndicator()
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(id = R.string.send),
                                    tint = if (state.recipients.isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    modifier = modifier.imePadding(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        launcher.launch(arrayOf("*/*"))
                    }) {
                    Icon(
                        painter = painterResource(id = R.drawable.attach),
                        stringResource(id = R.string.add_new_contact)
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .padding(
                        start = padding.calculateLeftPadding(LayoutDirection.Ltr),
                        end = padding.calculateRightPadding(LayoutDirection.Ltr),
                        top = padding.calculateTopPadding()
                    )
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
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
                            id = R.string.broadcast_title
                        ),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = modifier.width(MARGIN_DEFAULT))
                    Text(
                        stringResource(id = R.string.broadcast_title),
                        fontFamily = bodyFontFamily,
                        softWrap = true
                    )
                    Spacer(modifier = modifier.weight(1f))
                    Switch(
                        checked = state.broadcast,
                        onCheckedChange = { viewModel.toggleBroadcast() })
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
                            suffix = {
                                if (state.addressFieldText.isNotBlank()) {
                                    if (state.addressLoading) {
                                        CircularProgressIndicator(
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
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onValueChange = { str -> viewModel.updateTo(str) },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                showKeyboardOnFocus = true,
                            ),
                            isError = state.addressErrorResId != null,
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            ),
                            label = {
                                Text(stringResource(id = R.string.to_placeholder))
                            },
                            supportingText = {
                                state.addressErrorResId?.let {
                                    Text(
                                        text = stringResource(id = it),
                                        color = MaterialTheme.colorScheme.error
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
                                })
                    }
                }
                AnimatedVisibility(visible = !state.broadcast) {
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT / 2))
                }
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
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .focusRequester(subjectFocusRequester)
                        .fillMaxWidth()
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
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
                                color = MaterialTheme.colorScheme.error
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
                state.attachments.map { attachmentUri ->
                    AttachmentViewHolder(
                        modifier = modifier,
                        attachment = attachmentUri,
                        viewModel = viewModel
                    )
                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT + 56.dp + padding.calculateBottomPadding()))
            }
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
                        if (state.openedAddressDetails?.imageUrl == null) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = stringResource(id = R.string.profile_image)
                            )
                        } else {
                            AsyncImage(
                                contentScale = ContentScale.Crop,
                                model = state.openedAddressDetails!!.imageUrl,
                                contentDescription = stringResource(id = R.string.profile_image)
                            )
                        }
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
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(id = R.string.warning))
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(id = R.string.exit_composing_confirmation),  textAlign = TextAlign.Center)
                        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                        ElevatedButton(modifier = modifier.fillMaxWidth(), onClick = {
                            viewModel.closeExitConfirmation()
                            navController.popBackStack(route = "HomeScreen", inclusive = false)
                        }) {
                            Text(stringResource(id = R.string.save_as_draft))
                        }
                        OutlinedButton(modifier = modifier.fillMaxWidth(), onClick = {
                            coroutineScope.launch {
                                viewModel.closeExitConfirmation()
                                viewModel.deleteDraft()
                                navController.popBackStack(route = "HomeScreen", inclusive = false)
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
                        //modifier = modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.closeExitConfirmation()
                        }
                    ) {
                        Text(stringResource(id = R.string.cancel_button))
                    }
                },
            )
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
                .border(1.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape),
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
                    if (user.imageUrl == null) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = stringResource(id = R.string.profile_image)
                        )
                    } else {
                        AsyncImage(
                            contentScale = ContentScale.Crop,
                            model = user.imageUrl,
                            contentDescription = stringResource(id = R.string.profile_image)
                        )
                    }
                }
                Spacer(modifier = modifier.width(MARGIN_DEFAULT / 4))
                Text(text = user.fullName, style = MaterialTheme.typography.bodySmall)
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
    val type = context.contentResolver.getType(attachment)?.lowercase() ?: ""
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = MARGIN_DEFAULT)
    ) {
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

        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
        Text(
            text = attachment.getNameFromURI(context),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = modifier.weight(1f))
        IconButton(onClick = { viewModel.removeAttachment(attachment) }) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(id = R.string.clear_button)
            )
        }
    }
}