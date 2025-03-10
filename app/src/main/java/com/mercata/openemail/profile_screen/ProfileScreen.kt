@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.mercata.openemail.profile_screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.openemail.DEFAULT_CORNER_RADIUS
import com.mercata.openemail.MARGIN_DEFAULT
import com.mercata.openemail.PROFILE_IMAGE_HEIGHT
import com.mercata.openemail.R
import com.mercata.openemail.common.ProfileImage
import com.mercata.openemail.common.SwitchViewHolder
import com.mercata.openemail.utils.getProfilePictureUrl
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

    val documentChooserLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                viewModel.setUserImage(it)
            }
        }

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
                    )
                }, onClick = {
                    navController.popBackStack()
                })
            })
        },

        ) { padding ->
        Column(
            modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                ProfileImage(
                    modifier
                        .height(PROFILE_IMAGE_HEIGHT),
                    imageUrl = state.selectedNewImage?.toString()
                        ?: state.current?.address?.getProfilePictureUrl() ?: "",
                    onError = {
                        Icon(
                            painter = painterResource(R.drawable.contacts),
                            modifier = Modifier.size(100.dp),
                            contentDescription = null,
                            tint = colorScheme.outline
                        )
                    })
                Row(
                    modifier = modifier.fillMaxWidth().padding(bottom = MARGIN_DEFAULT),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ElevatedButton(onClick = {
                        documentChooserLauncher.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = stringResource(R.string.edit)
                            )
                            Spacer(modifier.width(MARGIN_DEFAULT / 2))
                            Text(stringResource(R.string.edit))
                        }
                    }
                    ElevatedButton(onClick = {
                        viewModel.deleteUserpic()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.delete)
                        )
                        Spacer(modifier.width(MARGIN_DEFAULT / 2))
                        Text(stringResource(R.string.delete))
                    }
                }
            }
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage,
                divider = {
                    HorizontalDivider(thickness = 2.dp, color = colorScheme.outline)
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
                                    if (pagerState.currentPage == index) colorScheme.primary
                                    else colorScheme.onSurface
                                ),
                                modifier = modifier.padding(MARGIN_DEFAULT)
                            )
                        }
                    }
                })

            HorizontalPager(modifier = modifier.fillMaxSize(), state = pagerState) { index ->
                Column(
                    modifier
                        .fillMaxSize()
                        .padding(
                            top = MARGIN_DEFAULT,
                            bottom = MARGIN_DEFAULT + padding.calculateBottomPadding()
                        )

                ) {
                    val tab = state.tabs[index]
                    tab.listItems.forEach { tabData ->
                        when (tabData) {
                            is ProfileViewModel.UserPicListItem -> {
                                val imageModifier = modifier
                                    .size(80.dp)
                                    .clickable {
                                        documentChooserLauncher.launch(
                                            PickVisualMediaRequest(
                                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                    .clip(CircleShape)
                                    .align(alignment = Alignment.CenterHorizontally)
                                ProfileImage(
                                    modifier = imageModifier,
                                    imageUrl = state.selectedNewImage?.toString()
                                        ?: state.current?.address?.getProfilePictureUrl() ?: "",
                                    onError = {
                                        Icon(
                                            modifier = imageModifier.padding(MARGIN_DEFAULT),
                                            painter = painterResource(R.drawable.frame_person),
                                            contentDescription = null,
                                            tint = colorScheme.primary
                                        )
                                    },
                                    onLoading = {
                                        CircularProgressIndicator(
                                            imageModifier,
                                            strokeCap = StrokeCap.Round
                                        )
                                    })
                            }

                            is ProfileViewModel.InputWithSwitchListItem -> {
                                Column {
                                    HorizontalDivider(
                                        modifier = modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = MARGIN_DEFAULT,
                                                end = MARGIN_DEFAULT,
                                                top = MARGIN_DEFAULT
                                            ),
                                        color = colorScheme.outline,
                                        thickness = 1.dp
                                    )
                                    SwitchViewHolder(
                                        isChecked = tabData.getSwitchValue(state),
                                        title = tabData.switchTitle,
                                        hint = tabData.switchHint,
                                        onChange = { isChecked ->
                                            tabData.onSwitchChanged(
                                                viewModel,
                                                isChecked
                                            )
                                        })
                                    AnimatedVisibility(visible = tabData.getSwitchValue(state)) {
                                        OutlinedTextField(
                                            modifier = modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = MARGIN_DEFAULT,
                                                    end = MARGIN_DEFAULT,
                                                    bottom = MARGIN_DEFAULT
                                                ),
                                            shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
                                            value = tabData.getValue(state),
                                            label = {
                                                Text(stringResource(id = tabData.hintResId))
                                            },
                                            supportingText = tabData.supportingStringResId?.let {
                                                {
                                                    Text(
                                                        text = stringResource(id = it),
                                                        color = colorScheme.error
                                                    )
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                imeAction = ImeAction.Unspecified,
                                                showKeyboardOnFocus = true,
                                            ),

                                            onValueChange = { str ->
                                                tabData.onChanged(
                                                    viewModel,
                                                    str
                                                )
                                            })
                                    }
                                    HorizontalDivider(
                                        modifier = modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = MARGIN_DEFAULT,
                                                end = MARGIN_DEFAULT,
                                            ),
                                        color = colorScheme.outline,
                                        thickness = 1.dp
                                    )
                                }
                            }

                            is ProfileViewModel.InputListItem -> {
                                OutlinedTextField(
                                    modifier = modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = MARGIN_DEFAULT,
                                            vertical = MARGIN_DEFAULT / 2
                                        ),
                                    shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
                                    value = tabData.getValue(state),
                                    label = {
                                        Text(stringResource(id = tabData.hintResId))
                                    },
                                    supportingText = tabData.supportingStringResId?.let {
                                        {
                                            Text(
                                                text = stringResource(id = it),
                                                color = colorScheme.error
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Unspecified,
                                        showKeyboardOnFocus = true,
                                    ),

                                    onValueChange = { str ->
                                        tabData.onChanged(
                                            viewModel,
                                            str
                                        )
                                    })
                            }

                            is ProfileViewModel.SwitchListItem -> {
                                SwitchViewHolder(
                                    isChecked = tabData.getValue(state),
                                    title = tabData.titleResId,
                                    hint = tabData.getHintResId(state),
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