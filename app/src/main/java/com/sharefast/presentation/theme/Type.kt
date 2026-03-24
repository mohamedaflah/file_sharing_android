package com.sharefast.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.sharefast.R

private val gmsFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun interGoogleFont() = GoogleFont("Inter")

private fun poppinsGoogleFont() = GoogleFont("Poppins")

/** Inter via Google Play services font provider (offline cached after first fetch). */
val InterFontFamily: FontFamily = FontFamily(
    Font(googleFont = interGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.Normal),
    Font(googleFont = interGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.Medium),
    Font(googleFont = interGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.Bold),
)

/** Display / titles — Poppins for a premium headline feel. */
val PoppinsFontFamily: FontFamily = FontFamily(
    Font(googleFont = poppinsGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.Normal),
    Font(googleFont = poppinsGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.Medium),
    Font(googleFont = poppinsGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = poppinsGoogleFont(), fontProvider = gmsFontProvider, weight = FontWeight.Bold),
)

val ShareFastTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

/** Used if Google Fonts provider fails (no Play services, bad certs, etc.). */
val ShareFastTypographySans = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)
