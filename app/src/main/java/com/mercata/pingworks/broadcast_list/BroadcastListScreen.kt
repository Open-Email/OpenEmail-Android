@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks.broadcast_list

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.R
import com.mercata.pingworks.common.ListScreen
import com.mercata.pingworks.models.BroadcastMessage

@Composable
fun SharedTransitionScope.BroadcastListScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: BroadcastListViewModel = viewModel(),
) {

    val state by viewModel.state.collectAsState()
    ListScreen(
        navController = navController,
        animatedVisibilityScope = animatedVisibilityScope,
        state = state,
        titleResId = R.string.broadcast_title,
        primaryColor = MaterialTheme.colorScheme.secondary,
        primaryContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        onDeleteAction = { message -> viewModel.removeItem(message as BroadcastMessage) }
    )
}

