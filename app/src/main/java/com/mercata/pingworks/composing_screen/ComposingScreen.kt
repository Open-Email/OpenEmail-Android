@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.mercata.pingworks.composing_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.theme.bodyFontFamily

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
                    IconButton(onClick = {
                        //TODO send message
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleBroadcast() }
                    .padding(MARGIN_DEFAULT)
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
                    value = state.to,
                    onValueChange = { str -> viewModel.updateTo(str) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        showKeyboardOnFocus = true,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    ),
                    prefix = {
                        Text(stringResource(id = R.string.to_placeholder))
                    },
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .focusRequester(toFocusRequester)
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            //TODO check address presented

                            println("To has focus: ${focusState.hasFocus}")
                        })
            }
            AnimatedVisibility(visible = !state.broadcast) {
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            }
            OutlinedTextField(
                value = state.subject,
                onValueChange = { str -> viewModel.updateSubject(str) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    showKeyboardOnFocus = true,
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                prefix = {
                    Text(stringResource(id = R.string.subject_placeholder))
                },
                modifier = modifier
                    .padding(horizontal = MARGIN_DEFAULT)
                    .focusRequester(subjectFocusRequester)
                    .fillMaxWidth()
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            Box(contentAlignment = Alignment.TopStart) {
                if (state.body.isEmpty()) {
                    Text(modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = stringResource(id = R.string.body_placeholder))
                }
                BasicTextField(
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        showKeyboardOnFocus = true,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .fillMaxSize()
                        //.clip(RoundedCornerShape(DEFAULT_CORNER_RADIUS))
                        //.background(MaterialTheme.colorScheme.surface)
                        .focusRequester(bodyFocusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    value = state.body,
                    onValueChange = { str ->
                        viewModel.updateBody(str)
                    })
            }
        }
    }
}