package com.mercata.openemail.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mercata.openemail.DEFAULT_CORNER_RADIUS
import com.mercata.openemail.MARGIN_DEFAULT
import com.mercata.openemail.SETTING_LIST_ITEM_SIZE
import com.mercata.openemail.utils.Address
import com.mercata.openemail.utils.getProfilePictureUrl

@Composable
fun ProfileView(modifier: Modifier = Modifier, name: String, address: Address) {
    Row(
        modifier
            .clip(
                RoundedCornerShape(
                    DEFAULT_CORNER_RADIUS
                )
            )
            .border(
                width = 1.dp, shape = RoundedCornerShape(
                    DEFAULT_CORNER_RADIUS
                ), color = colorScheme.outline
            ).padding(vertical = MARGIN_DEFAULT/2, horizontal = MARGIN_DEFAULT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        ProfileImage(
            Modifier
                .size(SETTING_LIST_ITEM_SIZE)
                .clip(CircleShape),
            address.getProfilePictureUrl(),
            onError = {
                Box(
                    Modifier
                        .size(SETTING_LIST_ITEM_SIZE)
                        .background(color = colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (name.takeIf { it.isNotBlank() }
                            ?: address).takeIf { it.isNotBlank() }?.substring(0, 2) ?: "",
                        style = typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                }
            })
        Spacer(Modifier.width(MARGIN_DEFAULT))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = MARGIN_DEFAULT)
        ) {
            Text(name, style = typography.titleMedium)
            Text(address, style = typography.bodyMedium)
        }
    }
}