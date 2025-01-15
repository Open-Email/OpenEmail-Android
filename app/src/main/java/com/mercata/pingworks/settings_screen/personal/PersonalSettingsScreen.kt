package com.mercata.pingworks.settings_screen.personal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.R
import com.mercata.pingworks.SETTING_LIST_ITEM_SIZE
import com.mercata.pingworks.common.InputViewHolder
import com.mercata.pingworks.common.ProfileImage
import com.mercata.pingworks.common.SwitchViewHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalSettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PersonalSettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    val documentChooserLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                viewModel.setUserImage(it)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Text(
                        stringResource(id = R.string.personal_settings),
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
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveData()
                        }, enabled = state.data != null && state.data != state.tmpData
                    ) {
                        Text(stringResource(R.string.save_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .imePadding()
                .padding(vertical = MARGIN_DEFAULT)
        ) {
            val imageModifier = modifier
                .size(100.dp)
                .clickable {
                    documentChooserLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                .clip(CircleShape)
                .align(alignment = Alignment.CenterHorizontally)
            ProfileImage(
                modifier = imageModifier,
                imageUrl = state.avatarUrl ?: "",
                onError = {
                    Icon(
                        modifier = imageModifier.padding(MARGIN_DEFAULT),
                        painter = painterResource(R.drawable.frame_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onLoading = { CircularProgressIndicator(imageModifier, strokeCap = StrokeCap.Round) })
            state.localData?.let {
                Text(
                    it.name,
                    modifier = modifier.align(alignment = Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    it.address,
                    modifier = modifier.align(alignment = Alignment.CenterHorizontally)
                )
            }
            state.tmpData?.let { localData ->
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Category(modifier, R.string.current) { modifier ->
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.status ?: "",
                        R.string.status,
                        state.loading
                    ) { str ->
                        viewModel.onStatusChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.about ?: "",
                        R.string.about,
                        state.loading
                    ) { str ->
                        viewModel.onAboutChange(str)

                    }
                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Category(modifier, R.string.work) { modifier ->
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.work ?: "",
                        R.string.work,
                        state.loading
                    ) { str ->
                        viewModel.onWorkChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.organization ?: "",
                        R.string.organization,
                        state.loading
                    ) { str ->
                        viewModel.onOrganizationChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.department ?: "",
                        R.string.department,
                        state.loading
                    ) { str ->
                        viewModel.onDepartmentChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.jobTitle ?: "",
                        R.string.jobTitle,
                        state.loading
                    ) { str ->
                        viewModel.onJobTitleChange(str)
                    }
                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Category(modifier, R.string.personal) { modifier ->
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.gender ?: "",
                        R.string.gender,
                        state.loading
                    ) { str ->
                        viewModel.onGenderChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.relationshipStatus ?: "",
                        R.string.relationshipStatus,
                        state.loading
                    ) { str ->
                        viewModel.onRelationshipStatusChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))

                    //TODO birthday date selector

                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.education ?: "",
                        R.string.education,
                        state.loading
                    ) { str ->
                        viewModel.onEducationChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.language ?: "",
                        R.string.language,
                        state.loading
                    ) { str ->
                        viewModel.onLanguageChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.placesLived ?: "",
                        R.string.placesLived,
                        state.loading
                    ) { str ->
                        viewModel.onPacesLivedChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.notes ?: "",
                        R.string.notes,
                        state.loading
                    ) { str ->
                        viewModel.onNotesChange(str)
                    }

                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Category(modifier, R.string.interests) { modifier ->
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.interests ?: "",
                        R.string.interests,
                        state.loading
                    ) { str ->
                        viewModel.onInterestsChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))

                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.books ?: "",
                        R.string.books,
                        state.loading
                    ) { str ->
                        viewModel.onBooksChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.movies ?: "",
                        R.string.movies,
                        state.loading
                    ) { str ->
                        viewModel.onMoviesChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.music ?: "",
                        R.string.music,
                        state.loading
                    ) { str ->
                        viewModel.onMusicChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.sports ?: "",
                        R.string.sports,
                        state.loading
                    ) { str ->
                        viewModel.onSportsChange(str)
                    }
                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Category(modifier, R.string.contacts) { modifier ->
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.website ?: "",
                        R.string.website,
                        state.loading
                    ) { str ->
                        viewModel.onWebsiteChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.location ?: "",
                        R.string.location,
                        state.loading
                    ) { str ->
                        viewModel.onLocationChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.mailingAddress ?: "",
                        R.string.mailingAddress,
                        state.loading
                    ) { str ->
                        viewModel.onMailingAddressChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.phone ?: "",
                        R.string.phone,
                        state.loading
                    ) { str ->
                        viewModel.onPhoneChange(str)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))

                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.streams ?: "",
                        R.string.streams,
                        state.loading
                    ) { str ->
                        viewModel.onStreamsChange(str)
                    }

                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Category(modifier, R.string.absence) { modifier ->
                    SwitchViewHolder(
                        modifier,
                        state.tmpData?.away == true,
                        R.string.away
                    ) {
                        viewModel.toggleAway(it)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    InputViewHolder(
                        modifier,
                        focusManager,
                        localData.awayWarning ?: "",
                        R.string.away_warning,
                        state.loading
                    ) { str ->
                        viewModel.onAwayWarningChange(str)
                    }
                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                Category(modifier, R.string.configuration) { modifier ->
                    SwitchViewHolder(
                        modifier,
                        state.tmpData?.lastSeenPublic == true,
                        R.string.last_seen_public
                    ) {
                        viewModel.toggleLastSeenPublic(it)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))
                    SwitchViewHolder(
                        modifier,
                        state.tmpData?.publicAccess == true,
                        R.string.public_access
                    ) {
                        viewModel.togglePublicAccess(it)
                    }
                    Spacer(modifier = modifier.height(MARGIN_DEFAULT))

                }
                Spacer(modifier = modifier.height(MARGIN_DEFAULT))
            }
        }
    }
}

@Composable
fun Category(
    modifier: Modifier = Modifier,
    title: Int,
    content: @Composable (modifier: Modifier) -> Unit
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    var isVisible by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = rotation, label = "Chevron animation")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row(
            modifier
                .padding(horizontal = MARGIN_DEFAULT)
                .shadow(4.dp, shape = RoundedCornerShape(4.dp))
                .zIndex(1f)
                .fillMaxWidth()
                .height(SETTING_LIST_ITEM_SIZE)
                .clickable {
                    rotation = if (rotation == 0f) 90f else 0f
                    isVisible = !isVisible
                }

                .background(MaterialTheme.colorScheme.surface),

            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier.width(MARGIN_DEFAULT))
            Text(stringResource(title), fontWeight = FontWeight.Bold)
            Spacer(modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = modifier.rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier.width(MARGIN_DEFAULT))
        }


        AnimatedVisibility(isVisible) {
            Column {
                Spacer(modifier.height(MARGIN_DEFAULT))
                content(modifier.padding(horizontal = MARGIN_DEFAULT))
            }
        }
    }
}
