package com.thefadghost.neverforget.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.thefadghost.neverforget.model.ThemePreference

private val EmberLight = lightColorScheme(
    primary = Color(0xFFB34E3B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD2),
    onPrimaryContainer = Color(0xFF3D0600),
    secondary = Color(0xFF775650),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
    surfaceVariant = Color(0xFFF4DED9),
    onSurface = Color(0xFF241A18),
    outline = Color(0xFF8B716B),
)

private val EmberDark = darkColorScheme(
    primary = Color(0xFFFFB4A5),
    onPrimary = Color(0xFF671A0E),
    primaryContainer = Color(0xFF8F3324),
    onPrimaryContainer = Color(0xFFFFDAD2),
    background = Color(0xFF1B1210),
    surface = Color(0xFF1B1210),
    surfaceVariant = Color(0xFF53433F),
    onSurface = Color(0xFFF1DFDB),
)

private fun lightWithAccent(accent: Color) = EmberLight.copy(
    primary = accent,
    primaryContainer = accent.copy(alpha = 0.16f).compositeOver(Color.White),
)

private fun darkWithAccent(accent: Color, oled: Boolean = false) = EmberDark.copy(
    primary = accent,
    background = if (oled) Color(0xFF050505) else EmberDark.background,
    surface = if (oled) Color(0xFF050505) else EmberDark.surface,
)

private val NeverForgetTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun NeverForgetTheme(
    preference: ThemePreference = ThemePreference.EMBER,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val accent = when (preference) {
        ThemePreference.EMBER, ThemePreference.SYSTEM, ThemePreference.OLED -> Color(0xFFB34E3B)
        ThemePreference.SAGE -> Color(0xFF52755B)
        ThemePreference.COBALT -> Color(0xFF315F9E)
        ThemePreference.MONOCHROME -> Color(0xFF5F5E62)
    }
    val useDark = when (preference) {
        ThemePreference.OLED -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        else -> darkTheme
    }
    val colors = if (useDark) {
        darkWithAccent(accent, oled = preference == ThemePreference.OLED)
    } else {
        lightWithAccent(accent)
    }
    MaterialTheme(
        colorScheme = colors,
        typography = NeverForgetTypography,
        content = content,
    )
}

private fun Color.compositeOver(background: Color): Color {
    val alpha = alpha + background.alpha * (1f - alpha)
    return Color(
        red = (red * this.alpha + background.red * background.alpha * (1f - this.alpha)) / alpha,
        green = (green * this.alpha + background.green * background.alpha * (1f - this.alpha)) / alpha,
        blue = (blue * this.alpha + background.blue * background.alpha * (1f - this.alpha)) / alpha,
        alpha = alpha,
    )
}

