package com.mercata.openemail.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mercata.openemail.MARGIN_DEFAULT

@Composable
fun SwitchViewHolder(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    title: Int,
    hint: Int? = null,
    onChange: (isChecked: Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onChange(!isChecked)
            }
            .padding(horizontal = MARGIN_DEFAULT, vertical = MARGIN_DEFAULT / 2)
    ) {
        Column(
            modifier = modifier
                .weight(1f)
                .padding(end = MARGIN_DEFAULT),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(id = title),
                softWrap = true
            )
            hint?.let {
                Text(
                    stringResource(id = it),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    softWrap = true
                )
            }

        }
        Switch(
            checked = isChecked,
            onCheckedChange = { onChange(it) })
    }
}