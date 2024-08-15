package com.mercata.pingworks.message_details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mercata.pingworks.DEFAULT_DATE_FORMAT
import com.mercata.pingworks.HEADER_LARGE_TEXT_SIZE
import com.mercata.pingworks.HEADER_TEXT_SIZE
import com.mercata.pingworks.MARGIN_DEFAULT
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailsScreen(
    navController: NavController,
    messageDetailsViewModel: MessageDetailsViewModel = viewModel()
) {

    val state by messageDetailsViewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(elevation = if (scrollState.value == 0) 0.dp else 16.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (scrollState.value == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    AnimatedVisibility(visible = scrollState.value != 0,
                        enter = fadeIn() + slideInVertically { 100.dp.value.roundToInt() },
                        exit = fadeOut() + slideOutVertically { 100.dp.value.roundToInt() }
                    ) {
                        Text(
                            state.message.subject,
                            //state.message.subject,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = HEADER_TEXT_SIZE
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(MARGIN_DEFAULT)
        ) {
            Text(text = state.message.subject, fontSize = HEADER_LARGE_TEXT_SIZE)
            Spacer(modifier = Modifier.height(MARGIN_DEFAULT))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.0.dp))
                        .size(width = 72.0.dp, height = 72.0.dp),
                    model = state.message.person.imageUrl,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(MARGIN_DEFAULT))
                Text(
                    text = state.message.person.name,
                    maxLines = 2,
                    fontSize = HEADER_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Text(state.message.date.format(DEFAULT_DATE_FORMAT))
            }
            Spacer(modifier = Modifier.height(MARGIN_DEFAULT))
            Text(text = state.message.body)
        }
    }
}