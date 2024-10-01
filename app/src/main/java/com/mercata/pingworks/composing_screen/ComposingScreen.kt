@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.mercata.pingworks.composing_screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.ATTACHMENT_LIST_ITEM_HEIGHT
import com.mercata.pingworks.CONTACT_LIST_ITEM_IMAGE_SIZE
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.theme.bodyFontFamily
import com.mercata.pingworks.utils.getNameFromURI

@Composable
fun SharedTransitionScope.ComposingScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ComposingViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val toFocusRequester = remember { FocusRequester() }
    val subjectFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            viewModel.addAttachments(it)
        }

    val state by viewModel.state.collectAsState()

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
                            text = state.currentUser!!.address,
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
                    IconButton(enabled = !state.loading,
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
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
                .padding(padding)
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
                OutlinedTextField(
                    value = state.addressFieldText,
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
                            if (!focusState.isFocused && state.addressFieldText.isNotBlank()) {
                                viewModel.checkAddressExist()
                            }
                        })
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