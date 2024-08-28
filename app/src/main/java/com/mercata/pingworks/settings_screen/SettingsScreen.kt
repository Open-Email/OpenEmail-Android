@file:OptIn(ExperimentalMaterial3Api::class)

package com.mercata.pingworks.settings_screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.theme.bodyFontFamily

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Text(
                        stringResource(id = R.string.settings_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button)
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(MARGIN_DEFAULT)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.enable_autologin_feature),
                    fontFamily = bodyFontFamily,
                    softWrap = true
                )
                Spacer(modifier = modifier.weight(1f))
                Switch(
                    checked = state.autologinEnabled,
                    onCheckedChange = { viewModel.toggleAutologin(it) })
            }

            if (state.biometryAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(id = R.string.enable_biometric_feature),
                        fontFamily = bodyFontFamily,
                        softWrap = true
                    )
                    Spacer(modifier = modifier.weight(1f))
                    Switch(
                        checked = state.biometryEnabled,
                        onCheckedChange = { viewModel.toggleBiometry(it) })
                }
            }
            if (state.privateSigningKey != null && state.privateEncryptionKey != null) {
                Row(
                    modifier = modifier.padding(vertical = MARGIN_DEFAULT),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
            }
            Spacer(modifier = modifier.weight(1f))
            TextButton(modifier = modifier.fillMaxWidth(),
                onClick = {
                    viewModel.logout()
                    navController.popBackStack(route = "HomeScreen", inclusive = true)
                    navController.navigate(route = "SignInScreen")
                }) {
                Text(
                    text = stringResource(id = R.string.logout_button),
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = bodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    softWrap = true
                )
            }
        }
    }
}