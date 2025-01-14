package com.mercata.pingworks.sign_in

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.animationDuration
import com.mercata.pingworks.common.Logo
import com.mercata.pingworks.theme.roboto
import kotlinx.coroutines.delay

@Composable
fun SignInScreen(
    navController: NavController,
    modifier: Modifier = Modifier, viewModel: SignInViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    val addressFocusRequester = remember { FocusRequester() }
    val encryptionFocusRequester = remember { FocusRequester() }
    val signingFocusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = state.isLoggedIn) {
        if (state.isLoggedIn) {
            navController.popBackStack(route = "SignInScreen", inclusive = true)
            navController.navigate(route = "HomeScreen")
        }
    }

    LaunchedEffect(key1 = state.keysInputOpen) {
        focusManager.clearFocus()
        if (state.keysInputOpen) {
            delay(animationDuration.toLong())
            viewModel.openInputKeys()
            encryptionFocusRequester.requestFocus()
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
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT / 2))
                Text(
                    stringResource(id = R.string.onboarding_text),
                    modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .align(AbsoluteAlignment.Left),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT * 2))
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
                        imeAction = ImeAction.Next,
                        showKeyboardOnFocus = true,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            viewModel.signInClicked()
                        }
                    )
                )
                AnimatedVisibility(
                    visible = !state.keysInputOpen,
                    enter = fadeIn(animationSpec = tween(durationMillis = animationDuration)) + expandVertically(
                        animationSpec = tween(durationMillis = animationDuration)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = animationDuration)) + shrinkVertically(
                        animationSpec = tween(durationMillis = animationDuration)
                    )
                ) {
                    Button(
                        modifier = modifier
                            .padding(horizontal = MARGIN_DEFAULT)
                            .padding(top = MARGIN_DEFAULT)
                            .fillMaxWidth(),
                        onClick = { viewModel.signInClicked() },
                        enabled = !state.loading && state.signInButtonActive
                    ) {
                        Text(
                            stringResource(id = R.string.sign_in_button),
                            fontFamily = roboto
                        )
                    }
                }
                AnimatedVisibility(
                    visible = state.keysInputOpen,
                    enter = fadeIn(animationSpec = tween(durationMillis = animationDuration)) + expandVertically(
                        animationSpec = tween(durationMillis = animationDuration)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = animationDuration)) + shrinkVertically(
                        animationSpec = tween(durationMillis = animationDuration)
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        OutlinedTextField(
                            value = state.privateEncryptionKeyInput,
                            onValueChange = { str -> viewModel.onPrivateEncryptionKeyInput(str) },
                            singleLine = true,
                            shape = CircleShape,
                            enabled = !state.loading,
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.key),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            },
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(horizontal = MARGIN_DEFAULT)
                                .focusRequester(encryptionFocusRequester),
                            label = {
                                Text(
                                    text = stringResource(id = R.string.private_encryption_key_hint)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Unspecified,
                                imeAction = ImeAction.Next,
                                showKeyboardOnFocus = true,
                                capitalization = KeyboardCapitalization.None
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    focusManager.clearFocus()
                                    signingFocusRequester.requestFocus()
                                }
                            ),
                        )
                        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                        OutlinedTextField(
                            value = state.privateSigningKeyInput,
                            onValueChange = { str -> viewModel.onPrivateSigningKeyInput(str) },
                            singleLine = true,
                            shape = CircleShape,
                            enabled = !state.loading,
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.key),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            },
                            modifier = modifier
                                .padding(horizontal = MARGIN_DEFAULT)
                                .fillMaxWidth()
                                .focusRequester(signingFocusRequester),
                            label = {
                                Text(
                                    text = stringResource(id = R.string.private_signing_key_hint)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Unspecified,
                                imeAction = ImeAction.Done,
                                showKeyboardOnFocus = true,
                                capitalization = KeyboardCapitalization.None
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.authenticateWithKeys()
                                }
                            ),
                        )
                        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                        Button(
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(horizontal = MARGIN_DEFAULT),
                            onClick = { viewModel.authenticateWithKeys() },
                            enabled = !state.loading && state.authenticateButtonEnabled
                        ) {
                            Text(
                                stringResource(id = R.string.sign_in_button),
                            )
                        }
                        Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    }
                }

                Box(contentAlignment = Alignment.Center, modifier = modifier.weight(1f)) {
                    if (state.loading)
                        CircularProgressIndicator()
                }
                Text(

                    String.format(
                        stringResource(id = R.string.no_account_question),
                        stringResource(id = R.string.app_name)
                    ),
                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier.height(MARGIN_DEFAULT))
                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARGIN_DEFAULT),
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.surfaceVariant
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
