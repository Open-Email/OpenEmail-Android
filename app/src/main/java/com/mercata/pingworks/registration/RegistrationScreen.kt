@file:OptIn(ExperimentalMaterial3Api::class)

package com.mercata.pingworks.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.Logo
import com.mercata.pingworks.sign_in.RequestErrorDialog

@Composable
fun RegistrationScreen(
    navController: NavController,
    modifier: Modifier = Modifier, viewModel: RegistrationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val focusManager = LocalFocusManager.current
    val usernameFocusRequester = remember { FocusRequester() }
    val fullNameFocusRequester = remember { FocusRequester() }

    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = state.isRegistered) {
        if (state.isRegistered) {
            navController.navigate(route = "SaveKeysSuggestionScreen")
        }
    }

    fun registerCall() {
        focusManager.clearFocus()
        viewModel.register()
    }

    Scaffold { padding ->
        Box {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()

            ) {
                Box(
                    modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .fillMaxWidth()
                        .weight(1.5f)
                        .defaultMinSize(minHeight = padding.calculateTopPadding() + MARGIN_DEFAULT),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding())
                    ) {
                        IconButton(modifier = modifier.align(Alignment.Start), content = {
                            Icon(
                                painterResource(R.drawable.back),
                                contentDescription = stringResource(R.string.back_button),
                            )
                        }, onClick = {
                            navController.popBackStack()
                        })
                        Logo(
                            modifier
                                .padding(top = padding.calculateTopPadding())
                        )
                    }
                }

                Text(
                    String.format(stringResource(R.string.registration_title), stringResource(R.string.app_name)),
                    modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .align(AbsoluteAlignment.Left),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT / 2))
                Text(
                    stringResource(id = R.string.registration_description),
                    modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .align(AbsoluteAlignment.Left),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT * 2))

                OutlinedTextField(
                    value = state.fullNameInput,
                    onValueChange = { str -> viewModel.onFullNameEdit(str) },
                    singleLine = true,
                    isError = state.fullNameError,
                    enabled = !state.isLoading,
                    shape = CircleShape,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARGIN_DEFAULT)
                        .focusRequester(fullNameFocusRequester),
                    label = {
                        Text(
                            text = stringResource(id = R.string.full_name_hint)
                        )
                    },
                    supportingText = {
                        if (state.fullNameError) {
                            Text(
                                text = stringResource(id = R.string.full_name_empty_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Unspecified,
                        imeAction = ImeAction.Done,
                        showKeyboardOnFocus = true,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            registerCall()
                        }
                    ),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.usernameInput,
                        onValueChange = { str -> viewModel.onUsernameChange(str) },
                        singleLine = true,
                        isError = state.userNameError,
                        enabled = !state.isLoading,
                        shape = CircleShape,
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(horizontal = MARGIN_DEFAULT)
                            .focusRequester(usernameFocusRequester),
                        supportingText = {
                            if (state.userNameError) {
                                Text(
                                    text = stringResource(id = R.string.user_name_existing_error),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        trailingIcon = {
                            Row {
                                Text(
                                    "@", modifier = modifier
                                        .padding(horizontal = MARGIN_DEFAULT / 2)
                                )
                                Box(
                                    modifier = modifier
                                        .padding(end = MARGIN_DEFAULT / 2)
                                        .clickable {
                                            dropdownExpanded = !dropdownExpanded
                                        }) {
                                    Row {
                                        Text(
                                            state.selectedHostName,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            tint = MaterialTheme.colorScheme.primary,
                                            contentDescription = null
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }) {
                                        state.hostnames.forEach {
                                            DropdownMenuItem(
                                                text = { Text(it) },
                                                onClick = {
                                                    viewModel.selectHostName(it)
                                                    dropdownExpanded = false
                                                })
                                        }
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(id = R.string.user_name_hint),
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            showKeyboardOnFocus = true,
                            capitalization = KeyboardCapitalization.None
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusManager.clearFocus()
                                fullNameFocusRequester.requestFocus()
                            }
                        )
                    )
                }

                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARGIN_DEFAULT),
                    onClick = { registerCall() },
                    enabled = !state.isLoading && state.usernameInput.isNotBlank() && state.fullNameInput.isNotBlank()
                ) {
                    Text(
                        stringResource(id = R.string.authenticate_button),
                    )
                }
                Box(contentAlignment = Alignment.Center, modifier = modifier.weight(1f)) {
                    if (state.isLoading)
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                }
                Text(
                    stringResource(id = R.string.terms_of_service_title),
                    modifier = modifier
                        .align(Alignment.Start)
                        .padding(horizontal = MARGIN_DEFAULT),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier.height(MARGIN_DEFAULT / 2))
                Text(
                    stringResource(id = R.string.terms_of_service_description),
                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT + padding.calculateBottomPadding()))
            }
            if (state.registrationError != null) {
                RequestErrorDialog(message = state.registrationError!!, onDismiss = {
                    viewModel.clearError()
                })
            }
        }
    }
}
