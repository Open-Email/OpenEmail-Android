package com.mercata.openemail.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.mercata.openemail.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val roboto = FontFamily(
    Font(
        googleFont = GoogleFont("Roboto"),
        fontProvider = provider,
    )
)

val lexend = FontFamily(
    Font(
        googleFont = GoogleFont("Lexend"),
        fontProvider = provider,
    )
)

val robotoRegular = FontFamily(
    Font(
        googleFont = GoogleFont("Roboto Regular"),
        fontProvider = provider,
    )
)

val robotoMedium = FontFamily(
    Font(
        googleFont = GoogleFont("Roboto Medium"),
        fontProvider = provider,
    )
)


// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(
        fontFamily = roboto,
        fontSize = 57.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 64.0.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = baseline.displayMedium.copy(
        fontFamily = roboto, fontSize = 45.0.sp, fontWeight = FontWeight.W400, lineHeight = 52.0.sp
    ),
    displaySmall = baseline.displaySmall.copy(
        fontFamily = roboto, fontSize = 36.0.sp, fontWeight = FontWeight.W400, lineHeight = 44.0.sp
    ),
    headlineLarge = baseline.headlineLarge.copy(
        fontFamily = roboto,
        fontSize = 32.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 40.0.sp,
    ),
    headlineMedium = baseline.headlineMedium.copy(
        fontFamily = roboto,
        fontSize = 28.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 36.0.sp,
    ),
    headlineSmall = baseline.headlineSmall.copy(
        fontFamily = roboto,
        fontSize = 24.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 32.0.sp,
    ),
    titleLarge = baseline.titleLarge.copy(
        fontFamily = robotoRegular,
        fontSize = 22.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 28.0.sp,
    ),
    titleMedium = baseline.titleMedium.copy(
        fontFamily = robotoMedium,
        fontSize = 16.0.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 24.0.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = baseline.titleSmall.copy(
        fontFamily = robotoMedium,
        fontSize = 14.0.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 20.0.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = baseline.labelLarge.copy(
        fontFamily = robotoMedium,
        fontSize = 14.0.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 20.0.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = baseline.labelMedium.copy(
        fontFamily = robotoMedium,
        fontSize = 12.0.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 16.0.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = baseline.labelSmall.copy(
        fontFamily = robotoMedium,
        fontSize = 11.0.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 16.0.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = baseline.bodyLarge.copy(
        fontFamily = roboto,
        fontSize = 16.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 24.0.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = baseline.bodyMedium.copy(
        fontFamily = roboto,
        fontSize = 14.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 20.0.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = baseline.bodySmall.copy(
        fontFamily = roboto,
        fontSize = 12.0.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 16.0.sp,
        letterSpacing = 0.4.sp
    )
)

