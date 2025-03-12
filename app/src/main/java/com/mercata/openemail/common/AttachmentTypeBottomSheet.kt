@file:OptIn(ExperimentalMaterial3Api::class)

package com.mercata.openemail.common

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mercata.openemail.DEFAULT_CORNER_RADIUS
import com.mercata.openemail.MARGIN_DEFAULT
import com.mercata.openemail.R
import kotlinx.coroutines.launch

@Composable
fun AttachmentTypeBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onSelectFromStorageClick: () -> Unit,
    onPhotoAttachClick: () -> Unit,
    @DrawableRes selectFileIconRes: Int = R.drawable.file,
    @StringRes selectFileTitleRes: Int = R.string.attach_file
) {

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismissRequest()
                }
            }
            onPhotoAttachClick()
        }
    }

    ModalBottomSheet(
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(DEFAULT_CORNER_RADIUS),
        onDismissRequest = {
            onDismissRequest()
        },
        sheetState = sheetState
    ) {
        // Sheet content
        OutlinedButton(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = MARGIN_DEFAULT),
            onClick = {
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
                onSelectFromStorageClick()

            }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(selectFileIconRes),
                    contentDescription = stringResource(selectFileTitleRes),
                    tint = colorScheme.primary
                )
                Spacer(modifier.width(MARGIN_DEFAULT))
                Text(stringResource(selectFileTitleRes))
            }
        }
        Spacer(modifier.height(MARGIN_DEFAULT / 2))
        OutlinedButton(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = MARGIN_DEFAULT),
            onClick = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.camera),
                    contentDescription = stringResource(R.string.add_instant_photo),
                    tint = colorScheme.primary
                )
                Spacer(modifier.width(MARGIN_DEFAULT))
                Text(stringResource(R.string.add_instant_photo))
            }
        }
        Spacer(modifier.height(MARGIN_DEFAULT))

    }
}