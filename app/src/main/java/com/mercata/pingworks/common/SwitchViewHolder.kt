package com.mercata.pingworks.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mercata.pingworks.MARGIN_DEFAULT
import com.mercata.pingworks.theme.roboto

@Composable
fun SwitchViewHolder(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    title: Int,
    onChange: (isChecked: Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onChange(!isChecked)
            }
            .padding(horizontal = MARGIN_DEFAULT, vertical = MARGIN_DEFAULT/2)
    ) {
        Text(
            stringResource(id = title),
            fontFamily = roboto,
            softWrap = true
        )
        Spacer(modifier = modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = { onChange(it) })
    }
}