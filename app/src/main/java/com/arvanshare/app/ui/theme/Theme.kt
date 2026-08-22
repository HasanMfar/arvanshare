package com.arvanshare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand palette ────────────────────────────────────────────────────────────
// Primary: deep indigo-violet   Secondary: vibrant teal
val Indigo400  = Color(0xFF5C6BC0)
val Indigo600  = Color(0xFF3949AB)
val Indigo900  = Color(0xFF1A237E)
val Teal300    = Color(0xFF4DD0E1)
val Teal500    = Color(0xFF00BCD4)
val ErrorRed   = Color(0xFFEF5350)
val SurfaceDark = Color(0xFF12131A)
val SurfaceVarDark = Color(0xFF1E2030)
val OnSurfaceDark  = Color(0xFFE8EAF6)
val SurfaceLight   = Color(0xFFF5F5FB)
val SurfaceVarLight = Color(0xFFE8EAF6)

private val DarkColors = darkColorScheme(
    primary          = Indigo400,
    onPrimary        = Color.White,
    primaryContainer = Indigo900,
    onPrimaryContainer = Color(0xFFC5CAE9),
    secondary        = Teal300,
    onSecondary      = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9EEAF3),
    background       = SurfaceDark,
    onBackground     = OnSurfaceDark,
    surface          = SurfaceDark,
    onSurface        = OnSurfaceDark,
    surfaceVariant   = SurfaceVarDark,
    onSurfaceVariant = Color(0xFFB9C1EA),
    outline          = Color(0xFF8A90C8),
    error            = ErrorRed,
    onError          = Color.White,
)

private val LightColors = lightColorScheme(
    primary          = Indigo600,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = Indigo900,
    secondary        = Teal500,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363D),
    background       = SurfaceLight,
    onBackground     = Color(0xFF1A1B2E),
    surface          = SurfaceLight,
    onSurface        = Color(0xFF1A1B2E),
    surfaceVariant   = SurfaceVarLight,
    onSurfaceVariant = Color(0xFF383A52),
    outline          = Color(0xFF767A9E),
    error            = ErrorRed,
    onError          = Color.White,
)

// ── Typography (system default with weight customisation) ────────────────────
private val AppTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 57.sp, lineHeight = 64.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun ArvanShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = AppTypography,
        content     = content,
    )
}
