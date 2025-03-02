package com.mercata.pingworks.save_keys_suggestion

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.animationDuration
import com.mercata.pingworks.sign_in.BiometryEffect
import com.mercata.pingworks.theme.roboto

@Composable
fun SaveKeysSuggestionScreen(
    navController: NavController,
    modifier: Modifier = Modifier, viewModel: SaveKeysSuggestionViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold { padding ->

        LaunchedEffect(key1 = state.navigate) {
            if (state.navigate) {
                navController.popBackStack(route = "SignInScreen", inclusive = true)
                navController.navigate(route = "HomeScreen")
            }
        }

        BiometryEffect(
            isShown = state.biometryPrompt,
            onPassed = { viewModel.biometryPassed() },
            onCancelled = { viewModel.biometryCanceled() })

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = MARGIN_DEFAULT)
                .imePadding()
                .verticalScroll(rememberScrollState())

        ) {

            Spacer(modifier = modifier.weight(1f))

            Icon(
                Icons.Default.AccountCircle,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier.size(100.dp),
                contentDescription = null
            )

            Text(
                stringResource(id = R.string.registered_title),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = modifier.weight(1f))

            Text(
                stringResource(id = R.string.review_login_settings),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                softWrap = true
            )
            Spacer(modifier = modifier.height(MARGIN_DEFAULT))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.enable_autologin_feature),
                    softWrap = true
                )
                Spacer(modifier = modifier.weight(1f))
                Switch(
                    checked = state.autologinEnabled,
                    onCheckedChange = { viewModel.autologinToggle(it) })
            }

            if (state.biometryAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(id = R.string.enable_biometric_feature),
                        softWrap = true
                    )
                    Spacer(modifier = modifier.weight(1f))
                    Switch(
                        checked = state.biometryEnabled,
                        onCheckedChange = { viewModel.biometryToggle(it) })
                }
            }

            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            AnimatedVisibility(
                visible = (state.biometryAvailable && !state.biometryEnabled && !state.autologinEnabled)
                        || (!state.biometryAvailable && !state.autologinEnabled),
                enter = fadeIn(animationSpec = tween(durationMillis = animationDuration)) + expandVertically(
                    animationSpec = tween(durationMillis = animationDuration)
                ),
                exit = fadeOut(animationSpec = tween(durationMillis = animationDuration)) + shrinkVertically(
                    animationSpec = tween(durationMillis = animationDuration)
                )
            ) {
                Column(modifier = modifier.fillMaxWidth()) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {

                            val keysString =
                                "${context.getString(R.string.private_encryption_key_hint)}: ${state.privateEncryptionKey}\n\n${
                                    context.getString(R.string.private_signing_key_hint)
                                }: ${state.privateSigningKey}"

                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, keysString)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)

                            context.startActivity(shareIntent)
                        }) {
                            Icon(
                                Icons.Default.Share,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = modifier.width(MARGIN_DEFAULT))
                        Column(
                            modifier = modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                "${stringResource(id = R.string.private_encryption_key_hint)}: ${state.privateEncryptionKey}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                            Text(
                                "${stringResource(id = R.string.private_signing_key_hint)}: ${state.privateSigningKey}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    Text(
                        stringResource(id = R.string.consider_saving_warning),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        softWrap = true
                    )

                }
            }

            Spacer(modifier = modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveSettings()
                },
            ) {
                Text(
                    stringResource(id = R.string.save_button),
                )
            }

            Spacer(modifier = modifier.height(MARGIN_DEFAULT + padding.calculateBottomPadding()))
        }
    }
}