package com.mercata.pingworks.sign_in

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.SETTING_LIST_ITEM_SIZE
import com.mercata.pingworks.common.Logo
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.theme.roboto
import com.mercata.pingworks.utils.getProfilePictureUrl

@Composable
fun SignInScreen(
    navController: NavController,
    modifier: Modifier = Modifier, viewModel: SignInViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val addressFocusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = state.isLoggedIn) {
        if (state.isLoggedIn) {
            navController.popBackStack(route = "SignInScreen", inclusive = true)
            navController.navigate(route = "HomeScreen")
        }
    }

    BiometryEffect(
        isShown = state.biometryShown,
        onPassed = { viewModel.biometryPassed() },
        onCancelled = { viewModel.biometryCanceled() },
        onError = { viewModel.biometryCanceled() })

    Scaffold { padding ->
        Box {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())

            ) {
                Box(
                    modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.3f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .fillMaxWidth()
                        .weight(1.5f)
                        .defaultMinSize(minHeight = padding.calculateTopPadding() + MARGIN_DEFAULT),
                    contentAlignment = Alignment.Center
                ) {
                    Logo(
                        modifier
                            .padding(top = padding.calculateTopPadding())
                    )
                }

                Text(
                    stringResource(id = R.string.login_title),
                    modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .align(AbsoluteAlignment.Left),
                    style = typography.headlineSmall,
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT / 2))
                Text(
                    stringResource(id = R.string.onboarding_text),
                    modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .align(AbsoluteAlignment.Left),
                    style = typography.bodyMedium
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT * 2))
                if (state.currentUser == null) {
                    OutlinedTextField(
                        value = state.emailInput,
                        onValueChange = { str -> viewModel.onEmailChange(str) },
                        singleLine = true,
                        isError = state.emailErrorResId != null,
                        enabled = !state.loading,
                        shape = CircleShape,
                        modifier = modifier
                            .padding(horizontal = MARGIN_DEFAULT)
                            .fillMaxWidth()
                            .focusRequester(addressFocusRequester),
                        supportingText = {
                            state.emailErrorResId?.run {
                                Text(
                                    text = stringResource(id = this),
                                    color = colorScheme.error
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
                            imeAction = ImeAction.Next,
                            showKeyboardOnFocus = true,
                            capitalization = KeyboardCapitalization.None
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                viewModel.signInClicked(onNewUser = {
                                    navController.navigate(
                                        "EnterKeysScreen/${state.emailInput}"
                                    )
                                })
                            }
                        )
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(id = R.string.your_account),
                            modifier.padding(horizontal = MARGIN_DEFAULT),
                            style = typography.bodyMedium
                        )
                        Spacer(modifier.weight(1f))
                        TextButton(onClick = {
                            viewModel.openManualEmailInput()
                        }) {
                            Text(
                                stringResource(id = R.string.not_your_account_button),
                                modifier.padding(horizontal = MARGIN_DEFAULT),
                                style = typography.bodyMedium
                            )
                        }
                    }
                    Row(
                        modifier.padding(bottom = MARGIN_DEFAULT),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier.width(MARGIN_DEFAULT))
                        ProfileImage(
                            modifier
                                .size(SETTING_LIST_ITEM_SIZE)
                                .clip(CircleShape),
                            state.currentUser!!.address.getProfilePictureUrl(),
                            onError = {
                                Box(
                                    modifier
                                        .size(SETTING_LIST_ITEM_SIZE)
                                        .background(color = colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (state.currentUser!!.name.takeIf { it.isNotBlank() }
                                            ?: state.currentUser!!.address).substring(0, 2),
                                        style = typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                }
                            })
                        Spacer(modifier.width(MARGIN_DEFAULT))
                        Column(modifier = modifier.weight(1f)) {
                            Text(state.currentUser!!.name, style = typography.titleMedium)
                            Text(state.currentUser!!.address, style = typography.bodyMedium)
                        }
                    }
                }

                Button(
                    modifier = modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .padding(top = MARGIN_DEFAULT)
                        .fillMaxWidth(),
                    onClick = {
                        viewModel.signInClicked(onNewUser = {
                            navController.navigate(
                                "EnterKeysScreen/${state.emailInput}"
                            )
                        })
                    },
                    enabled = !state.loading && state.signInButtonActive
                ) {
                    Text(
                        stringResource(id = R.string.sign_in_button),
                        fontFamily = roboto
                    )
                }

                Box(contentAlignment = Alignment.Center, modifier = modifier.weight(1f)) {
                    if (state.loading)
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                }
                Text(

                    String.format(
                        stringResource(id = R.string.no_account_question),
                        stringResource(id = R.string.app_name)
                    ),
                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                    style = typography.labelLarge,
                    color = colorScheme.onSurface,
                )
                Spacer(modifier.height(MARGIN_DEFAULT))
                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARGIN_DEFAULT),
                    colors = ButtonColors(
                        containerColor = colorScheme.surfaceVariant,
                        contentColor = colorScheme.primary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = colorScheme.surfaceVariant
                    ), onClick = {
                        navController.navigate("RegistrationScreen")
                    }, enabled = !state.loading
                ) {
                    Text(
                        stringResource(id = R.string.registration_button),
                    )
                }
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

@Composable
fun RequestErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.something_went_wrong_title))
        },
        text = {
            Text(text = "Error: $message")
        },
        onDismissRequest = {
            onDismiss()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.cancel_button))
            }
        },
    )
}

@Composable
fun BiometryEffect(
    isShown: Boolean,
    onPassed: () -> Unit,
    onCancelled: () -> Unit,
    onError: ((errorCode: Int) -> Unit)? = null
) {
    val context = LocalContext.current as FragmentActivity
    LaunchedEffect(key1 = isShown) {
        if (!isShown) return@LaunchedEffect

        val authCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onPassed()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == 10) {
                    onCancelled()
                } else {
                    onError?.invoke(errorCode)
                }
            }

            override fun onAuthenticationFailed() {
                //ignore
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.app_name))
            .setSubtitle(context.getString(R.string.enable_biometric_feature))
            .setDescription(context.getString(R.string.authenticate_to_enable_biometric))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .setConfirmationRequired(true)
            .build()

        BiometricPrompt(context, ContextCompat.getMainExecutor(context), authCallback).authenticate(
            promptInfo
        )
    }
}
