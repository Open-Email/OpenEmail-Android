@file:OptIn(ExperimentalMaterial3Api::class)

package com.mercata.pingworks.settings_screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lightspark.composeqr.DotShape
import com.lightspark.composeqr.QrCodeColors
import com.lightspark.composeqr.QrCodeView
import com.mercata.pingworks.DEFAULT_CORNER_RADIUS
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.SETTING_LIST_ITEM_SIZE
import com.mercata.pingworks.common.SwitchViewHolder
import com.mercata.pingworks.theme.roboto
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

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
                    IconButton(content = {
                        Icon(
                            painterResource(R.drawable.back),
                            contentDescription = stringResource(R.string.back_button),
                        )
                    }, onClick = {
                        navController.popBackStack()
                    })
                },
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {

            TabRow(selectedTabIndex = pagerState.currentPage,
                divider = {
                    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
                }, tabs = {
                    Tab(selected = pagerState.currentPage == 0, onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    }) {
                        Text(
                            stringResource(R.string.general),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color =
                                if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = modifier.padding(MARGIN_DEFAULT)
                        )
                    }
                    Tab(selected = pagerState.currentPage == 1, onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }) {
                        Text(
                            stringResource(R.string.keys),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color =
                                if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = modifier.padding(MARGIN_DEFAULT)
                        )
                    }
                })

            HorizontalPager(state = pagerState) { index ->
                when (index) {
                    0 -> {
                        Column(
                            modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT + padding.calculateBottomPadding()
                                )
                        ) {
                            SwitchViewHolder(
                                modifier,
                                state.autologinEnabled,
                                R.string.enable_autologin_feature
                            ) {
                                viewModel.toggleAutologin(it)
                            }

                            if (state.biometryAvailable) {
                                SwitchViewHolder(
                                    modifier,
                                    state.biometryEnabled,
                                    R.string.enable_biometric_feature
                                ) {
                                    viewModel.toggleBiometry(it)
                                }
                            }
                            Spacer(modifier = modifier.weight(1f))
                            TextButton(modifier = modifier
                                .fillMaxWidth()
                                .padding(horizontal = MARGIN_DEFAULT),
                                onClick = {
                                    viewModel.logout {
                                        navController.popBackStack(
                                            route = "HomeScreen",
                                            inclusive = true
                                        )
                                        navController.navigate(route = "SignInScreen")
                                    }

                                }) {
                                Text(
                                    text = stringResource(id = R.string.logout_button),
                                    color = MaterialTheme.colorScheme.error,
                                    fontFamily = roboto,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = true
                                )
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    top = MARGIN_DEFAULT,
                                    bottom = MARGIN_DEFAULT + padding.calculateBottomPadding()
                                )
                        ) {
                            if (state.privateSigningKey != null && state.privateEncryptionKey != null) {
                                Box(
                                    modifier = modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .fillMaxWidth()
                                        .height(300.dp + MARGIN_DEFAULT * 2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = modifier
                                            .clip(
                                                RoundedCornerShape(DEFAULT_CORNER_RADIUS)
                                            )
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        QrCodeView(
                                            data = "${state.privateEncryptionKey}:${state.privateSigningKey}",
                                            modifier = modifier
                                                .size(300.dp)
                                                .padding(MARGIN_DEFAULT),
                                            colors = QrCodeColors(
                                                background = MaterialTheme.colorScheme.surface,
                                                foreground = MaterialTheme.colorScheme.onSurface
                                            ),
                                            dotShape = DotShape.Circle
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .rotate(45f)
                                                    .background(
                                                        brush = Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFF005BEC),
                                                                Color(0xFF1E76F2),
                                                                Color(0xFF00CFFF),

                                                                )
                                                        )
                                                    )
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.monochrome_logo),
                                                    modifier = modifier
                                                        .rotate(-45f)
                                                        .fillMaxSize()
                                                        .padding(7.dp),
                                                    tint = Color.White,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier.height(MARGIN_DEFAULT * 1.5f))
                                Text(
                                    stringResource(R.string.encryption),
                                    modifier.padding(horizontal = MARGIN_DEFAULT),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier.height(MARGIN_DEFAULT))
                                Row(
                                    modifier.padding(horizontal = MARGIN_DEFAULT),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.private_key),
                                        modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    IconButton(onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, state.privateEncryptionKey)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(
                                            sendIntent,
                                            context.getString(R.string.private_key)
                                        )
                                        context.startActivity(shareIntent)
                                    }) {
                                        Icon(
                                            Icons.Rounded.Share,
                                            stringResource(R.string.share),
                                            tint = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                                Spacer(modifier.height(MARGIN_DEFAULT))
                                Text(
                                    state.privateEncryptionKey ?: "",
                                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                HorizontalDivider(
                                    thickness = 1.dp,
                                    modifier = modifier.padding(vertical = MARGIN_DEFAULT),
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Row(
                                    modifier.padding(horizontal = MARGIN_DEFAULT),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.public_key),
                                        modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    IconButton(onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, state.publicEncryptionKey)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(
                                            sendIntent,
                                            context.getString(R.string.public_key)
                                        )
                                        context.startActivity(shareIntent)
                                    }) {
                                        Icon(
                                            Icons.Rounded.Share,
                                            stringResource(R.string.share),
                                            tint = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                                Spacer(modifier.height(MARGIN_DEFAULT))
                                Text(
                                    state.publicEncryptionKey ?: "",
                                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    modifier = modifier.padding(vertical = MARGIN_DEFAULT),
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    stringResource(R.string.signing),
                                    modifier.padding(horizontal = MARGIN_DEFAULT),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier.height(MARGIN_DEFAULT))

                                Row(
                                    modifier.padding(horizontal = MARGIN_DEFAULT),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.private_key),
                                        modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    IconButton(onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, state.privateSigningKey)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(
                                            sendIntent,
                                            context.getString(R.string.private_key)
                                        )
                                        context.startActivity(shareIntent)
                                    }) {
                                        Icon(
                                            Icons.Rounded.Share,
                                            stringResource(R.string.share),
                                            tint = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                                Spacer(modifier.height(MARGIN_DEFAULT))
                                Text(
                                    state.privateSigningKey ?: "",
                                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier.height(MARGIN_DEFAULT))
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Row(
                                    modifier.padding(horizontal = MARGIN_DEFAULT),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.public_key),
                                        modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    IconButton(onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, state.publicSigningKey)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(
                                            sendIntent,
                                            context.getString(R.string.public_key)
                                        )
                                        context.startActivity(shareIntent)
                                    }) {
                                        Icon(
                                            Icons.Rounded.Share,
                                            stringResource(R.string.share),
                                            tint = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                                Spacer(modifier.height(MARGIN_DEFAULT))
                                Text(
                                    state.publicSigningKey ?: "",
                                    modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier.height(MARGIN_DEFAULT))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingViewHolder(modifier: Modifier = Modifier, title: String, onClick: () -> Unit) {
    Row(modifier = modifier
        .height(SETTING_LIST_ITEM_SIZE)
        .clickable { onClick() }
        .padding(horizontal = MARGIN_DEFAULT), verticalAlignment = Alignment.CenterVertically) {
        Text(title)
        Spacer(modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}