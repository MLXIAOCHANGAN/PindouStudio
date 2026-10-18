package com.pindou.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Teal = Color(0xFF2E7D6F)
val TealDark = Color(0xFF1E5A50)
val Amber = Color(0xFFE9A13B)
val Cream = Color(0xFFFBF7F2)
val Ink = Color(0xFF26221E)
val InkSoft = Color(0xFF6B635B)
val Line = Color(0xFFE5DED5)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EBE5),
    onPrimaryContainer = TealDark,
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCEBD2),
    onSecondaryContainer = Color(0xFF6B4A12),
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2EDE6),
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD0BE),
    onPrimary = Color(0xFF00382F),
    secondary = Color(0xFFF3C177),
    background = Color(0xFF17181A),
    onBackground = Color(0xFFEDE7E0),
    surface = Color(0xFF212326),
    onSurface = Color(0xFFEDE7E0),
    surfaceVariant = Color(0xFF2C2F33),
    onSurfaceVariant = Color(0xFFC3BDB6),
    outline = Color(0xFF3B3F44),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp),
)

@Composable
fun PindouTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
