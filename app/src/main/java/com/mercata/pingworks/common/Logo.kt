package com.mercata.pingworks.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercata.pingworks.R
import com.mercata.pingworks.theme.lexend

@Composable
fun Logo(modifier: Modifier = Modifier, size: LogoSize = LogoSize.Medium, lightFont: Boolean = false) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(size.iconSize.dp),
            painter = painterResource(R.drawable.monochrome_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.app_name),
            style = TextStyle(
                fontFamily = lexend,
                fontSize = size.fontSize.sp,
                fontWeight = FontWeight.W400,
                color = if (lightFont) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

enum class LogoSize(
    val fontSize: Float,
    val iconSize: Float
) {
    Medium(24.0f, 39.0f),
    Small(18.0f, 28.0f)
}