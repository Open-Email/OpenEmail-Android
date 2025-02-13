package com.mercata.pingworks.sign_in.enter_keys_screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.QR_SCANNER_RESULT
import com.mercata.pingworks.R
import com.mercata.pingworks.SETTING_LIST_ITEM_SIZE
import com.mercata.pingworks.common.Logo
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.sign_in.RequestErrorDialog
import com.mercata.pingworks.theme.roboto
import com.mercata.pingworks.utils.getProfilePictureUrl

@Composable
fun EnterKeysScreen(
    navController: NavController,
    modifier: Modifier = Modifier, viewModel: EnterKeysViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val encryptionFocusRequester = remember { FocusRequester() }
    val signingFocusRequester = remember { FocusRequester() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(route = "QRCodeScannerScreen")
        }
    }

    val qrResult =
        navController.currentBackStackEntry?.savedStateHandle?.get<String>(QR_SCANNER_RESULT)

    if (qrResult != null) {
        viewModel.parseScannedKeys(qrResult)
        navController.currentBackStackEntry?.savedStateHandle?.set(QR_SCANNER_RESULT, null)
    }

    LaunchedEffect(key1 = state.isLoggedIn) {
        if (state.isLoggedIn) {
            navController.popBackStack(route = "SignInScreen", inclusive = true)
            navController.navigate(route = "HomeScreen")
        }
    }

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
                    stringResource(id = R.string.enter_your_private_keys),
                    modifier
                        .padding(horizontal = MARGIN_DEFAULT)
                        .align(AbsoluteAlignment.Left),
                    style = typography.headlineSmall,
                )
                Spacer(modifier = modifier.height(MARGIN_DEFAULT / 2))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(id = R.string.your_account),
                        modifier.padding(horizontal = MARGIN_DEFAULT),
                        style = typography.bodyMedium
                    )
                    Spacer(modifier.weight(1f))
                    TextButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Text(
                            stringResource(id = R.string.not_your_account_button),
                            modifier.padding(horizontal = MARGIN_DEFAULT),
                            style = typography.bodyMedium
                        )
                    }
                }
                if (state.publicUserData != null) {
                    Row(
                        modifier.padding(bottom = MARGIN_DEFAULT),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier.width(MARGIN_DEFAULT))
                        ProfileImage(
                            modifier
                                .size(SETTING_LIST_ITEM_SIZE)
                                .clip(CircleShape),
                            state.address.getProfilePictureUrl(),
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
                                        text = (state.publicUserData?.fullName
                                            ?: state.address).substring(0, 2),
                                        style = typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                }
                            })
                        Spacer(modifier.width(MARGIN_DEFAULT))
                        Column(modifier = modifier.weight(1f)) {
                            Text(state.publicUserData!!.fullName, style = typography.titleMedium)
                            Text(state.address, style = typography.bodyMedium)
                        }
                    }
                }

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
                            tint = colorScheme.onSurface.copy(alpha = 0.8f)
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
                            tint = colorScheme.onSurface.copy(alpha = 0.8f)
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
                        .padding(horizontal = MARGIN_DEFAULT)
                        .padding(top = MARGIN_DEFAULT)
                        .fillMaxWidth(),
                    onClick = { viewModel.authenticateWithKeys() },
                    enabled = !state.loading && state.authenticateButtonEnabled
                ) {
                    Text(
                        stringResource(id = R.string.authenticate_button),
                        fontFamily = roboto
                    )
                }

                Box(contentAlignment = Alignment.Center, modifier = modifier.weight(1f)) {
                    if (state.loading)
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                }

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
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }, enabled = !state.loading
                ) {
                    Text(
                        stringResource(id = R.string.scan_qr_code),
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

