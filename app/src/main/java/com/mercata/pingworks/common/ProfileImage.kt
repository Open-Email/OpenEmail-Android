package com.mercata.pingworks.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.mercata.pingworks.R

@Composable
fun ProfileImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    onError: @Composable () -> Unit,
    onLoading: (@Composable () -> Unit)? = null
) {
    var imageState by remember { mutableStateOf(ImageLoadingState.Success) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (imageState == ImageLoadingState.Loading) {
            onLoading?.invoke()
        }

        if (imageState == ImageLoadingState.Error) {
            onError()
        }

        AsyncImage(
            modifier = modifier.fillMaxSize(),
            onLoading = {
                imageState = ImageLoadingState.Loading
            },
            onSuccess = {
                imageState = ImageLoadingState.Success
            },
            onError = {
                imageState = ImageLoadingState.Error
            },
            contentScale = ContentScale.Crop,
            model = imageUrl,
            contentDescription = stringResource(id = R.string.profile_image)
        )
    }
}

enum class ImageLoadingState {
    Loading, Error, Success
}