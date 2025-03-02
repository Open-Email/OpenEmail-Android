package com.mercata.pingworks.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
fun Logo(modifier: Modifier = Modifier, size: LogoSize = LogoSize.Medium, mode: LogoMode = LogoMode.Full, lightFont: Boolean = false) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mode != LogoMode.TitleOnly) {
            Image(
                modifier = Modifier.size(size.iconSize.dp),
                painter = painterResource(R.drawable.monochrome_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        }

        if (mode == LogoMode.Full) {
            Spacer(Modifier.width(8.dp))
        }

        if (mode != LogoMode.LogoOnly) {
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
}

enum class LogoSize(
    val fontSize: Float,
    val iconSize: Float
) {
    Large(28.0f, 45.5f),
    Medium(24.0f, 39.0f),
    Small(18.0f, 28.0f)
}

enum class LogoMode {
    Full,
    LogoOnly,
    TitleOnly
}
