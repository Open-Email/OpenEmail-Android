package com.mercata.pingworks.sign_in

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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun SignInScreen(
    navController: NavController,
    modifier: Modifier = Modifier, viewModel: SignInViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

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
            Spacer(modifier = modifier.weight(0.3f))
            Icon(
                Icons.Default.Email,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier.size(100.dp),
                contentDescription = null
            )
            Text(
                stringResource(id = R.string.app_name),
                fontFamily = displayFontFamily,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            Text(
                stringResource(id = R.string.onboarding_title),
                fontFamily = bodyFontFamily,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(id = R.string.onboarding_text),
                textAlign = TextAlign.Center,
                fontFamily = bodyFontFamily,
            )
            Spacer(modifier = modifier.weight(0.3f))
            OutlinedTextField(
                value = state.emailInput,
                onValueChange = { str -> viewModel.onEmailChange(str) },
                singleLine = true,
                isError = !state.emailValid,
                enabled = !state.loading,
                modifier = modifier.fillMaxWidth(),
                supportingText = {
                    if (!state.emailValid) {
                        Text(
                            text = stringResource(id = R.string.invalid_email),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                label = {
                    Text(
                        text = String.format(
                            stringResource(id = R.string.address_input_hint),
                            stringResource(id = R.string.app_name)
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Go,
                    showKeyboardOnFocus = true,
                    capitalization = KeyboardCapitalization.None
                ),
                keyboardActions = KeyboardActions(
                    onGo = { focusManager.clearFocus() }
                ),
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            Button(
                onClick = { viewModel.signIn() },
                enabled = !state.loading && state.signInButtonActive
            ) {
                Text(
                    stringResource(id = R.string.sign_in_button),
                    fontFamily = bodyFontFamily
                )
            }
            Spacer(modifier = modifier.weight(1f))
            Text(
                String.format(
                    stringResource(id = R.string.no_account_question),
                    stringResource(id = R.string.app_name)
                ),
                textAlign = TextAlign.Center,
                fontFamily = bodyFontFamily,
            )
            TextButton(onClick = { /*TODO*/ }, enabled = !state.loading) {
                Text(stringResource(id = R.string.registration_button), fontFamily = bodyFontFamily)
            }
        }
    }
}