@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks.outbox_list

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
fun SharedTransitionScope.OutboxListScreen(
    navController: NavController,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: OutboxListViewModel = viewModel(),
) {

    val state by viewModel.state.collectAsState()
    ListScreen(
        navController = navController,
        animatedVisibilityScope = animatedVisibilityScope,
        state = state,
        titleResId = R.string.outbox_title,
        primaryColor = MaterialTheme.colorScheme.tertiary,
        primaryContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        onDeleteAction = { message -> viewModel.removeItem(message as BroadcastMessage) }
    )
}

