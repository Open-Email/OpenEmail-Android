@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.mercata.pingworks.profile_screen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.common.SwitchViewHolder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.ProfileScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage
        }.distinctUntilChanged().collect {
            focusManager.clearFocus()
        }
    }

    Scaffold(
        modifier = modifier.sharedBounds(
            rememberSharedContentState(
                key = "profile_bounds"
            ),
            animatedVisibilityScope,
        ),
        topBar = {
            TopAppBar(title = {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = modifier.size(28.dp), strokeCap = StrokeCap.Round
                    )
                } else {
                    Text(stringResource(R.string.profile))
                }
            }, actions = {
                if (state.hasChanges()) {
                    Button(modifier = modifier.padding(horizontal = MARGIN_DEFAULT),
                        enabled = !state.loading,
                        onClick = {
                            viewModel.saveChanges()
                        }) {
                        Text(stringResource(R.string.save_button))
                    }
                }

            }, navigationIcon = {
                IconButton(content = {
                    Icon(
                        painterResource(R.drawable.back),
                        contentDescription = stringResource(R.string.back_button),
                        //tint = colorScheme.onSurface
                    )
                }, onClick = {
                    navController.popBackStack()
                })
            })
        },

        ) { padding ->
        Column(
            modifier.padding(top = padding.calculateTopPadding())
            //.verticalScroll(rememberScrollState())
        ) {
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage,
                divider = {
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
            }, tabs = {
                state.tabs.forEachIndexed { index, tabData ->
                    Tab(selected = pagerState.currentPage == index, onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }) {
                        Text(
                            stringResource(tabData.titleResId),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color =
                                if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = modifier.padding(vertical = MARGIN_DEFAULT)
                        )
                    }
                }
            })

            HorizontalPager(state = pagerState) { index ->
                Column(
                    modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    state.tabs[index].listItems.forEach { tabData ->
                        when (tabData) {
                            is ProfileViewModel.InputListItem -> {
                                OutlinedTextField(
                                    modifier = modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = MARGIN_DEFAULT,
                                            vertical = MARGIN_DEFAULT / 2
                                        ),
                                    shape = CircleShape,
                                    value = tabData.getValue(state),
                                    label = {
                                        Text(stringResource(id = tabData.hintResId))
                                    },
                                    supportingText = tabData.supportingStringResId?.let {
                                        {
                                            Text(
                                                text = stringResource(id = it),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done,
                                        showKeyboardOnFocus = true,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    onValueChange = { str -> tabData.onChanged(viewModel, str) })
                            }

                            is ProfileViewModel.SwitchListItem -> {
                                SwitchViewHolder(
                                    isChecked = tabData.getValue(state),
                                    title = tabData.titleResId,
                                    onChange = { isChecked ->
                                        tabData.onChanged(
                                            viewModel,
                                            isChecked
                                        )
                                    })
                            }
                        }
                    }
                }
            }


            /* ProfileImage(
                 modifier
                     .height(imageSize)
                 //Elevation bug under the navigation drawer
                 *//*.sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = "message_image/${state.address}"
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                )*//*,
                state.address.getProfilePictureUrl() ?: "",
                onError = {
                    Icon(
                        painterResource(R.drawable.contacts),
                        modifier = Modifier.size(100.dp),
                        contentDescription = null,
                        tint = colorScheme.outline
                    )
                })*/


        }
    }
}