package com.mercata.pingworks.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.theme.bodyFontFamily
import com.mercata.pingworks.theme.displayFontFamily

@Composable
fun RegistrationScreen(
    navController: NavController,
    modifier: Modifier = Modifier, viewModel: RegistrationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val focusManager = LocalFocusManager.current
    val usernameFocusRequester = remember { FocusRequester() }
    val fullNameFocusRequester = remember { FocusRequester() }

    Scaffold { padding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MARGIN_DEFAULT)
                .verticalScroll(rememberScrollState())

        ) {
            Spacer(modifier = modifier.weight(1f))
            Icon(
                Icons.Default.AccountCircle,
                modifier = modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            Text(
                stringResource(id = R.string.registration_title),
                fontFamily = displayFontFamily,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            Text(
                stringResource(id = R.string.registration_description),
                fontFamily = bodyFontFamily,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = modifier.height(MARGIN_DEFAULT))


            OutlinedTextField(
                value = state.usernameInput,
                onValueChange = { str -> viewModel.onUsernameChange(str) },
                singleLine = true,
                isError = state.userNameError,
                enabled = !state.isLoading,
                modifier = modifier
                    .fillMaxWidth()
                    .focusRequester(usernameFocusRequester),
                supportingText = {
                    if (state.userNameError) {
                        Text(
                            text = stringResource(id = R.string.user_name_existing_error),
                            color = MaterialTheme.colorScheme.error
                        )
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
                ),

                )

            fun registerCall() {
                focusManager.clearFocus()
                viewModel.register()
            }
            OutlinedTextField(
                value = state.fullNameInput,
                onValueChange = { str -> viewModel.onFullNameEdit(str) },
                singleLine = true,
                isError = state.fullNameError,
                enabled = !state.isLoading,
                modifier = modifier
                    .fillMaxWidth()
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
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            Button(
                onClick = { registerCall() },
                enabled = !state.isLoading && state.usernameInput.isNotBlank() && state.fullNameInput.isNotBlank()
            ) {
                Text(
                    stringResource(id = R.string.authenticate_button),
                    fontFamily = bodyFontFamily
                )
            }
            Spacer(modifier = modifier.weight(1f))
            Text(
                stringResource(id = R.string.terms_of_service_title),
                textAlign = TextAlign.Center,
                fontFamily = bodyFontFamily,
            )
            Text(
                stringResource(id = R.string.terms_of_service_description),
                textAlign = TextAlign.Center,
                fontFamily = bodyFontFamily,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
        }
    }
}
