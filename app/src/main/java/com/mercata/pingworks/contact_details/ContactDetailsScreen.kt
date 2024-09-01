@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks.contact_details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R

@Composable
fun SharedTransitionScope.ContactDetailsScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()
    val imageSize = remember { 300.dp }

    Scaffold(
        modifier = modifier.sharedBounds(
            rememberSharedContentState(
                key = "contact_bounds/${state.address}"
            ),
            animatedVisibilityScope,
        ),
    ) { padding ->
        Column {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "contact_image/${state.address}"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .height(imageSize)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                if (state.contact?.imageUrl == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .height(imageSize)
                            .padding(horizontal = MARGIN_DEFAULT)
                            .fillMaxWidth()

                    ) {
                        Text(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.surface,
                            text = state.contact?.name ?: state.address,
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                } else {
                    AsyncImage(
                        model = state.contact!!.imageUrl,
                        contentDescription = stringResource(id = R.string.profile_image)
                    )
                }
                if (state.contact?.name != null) {
                    Text(text = state.contact?.name!!)
                }

            }

            Spacer(modifier = modifier.height(padding.calculateBottomPadding()))
        }
    }
}