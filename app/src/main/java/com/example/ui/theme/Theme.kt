package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val MikeDarkColorScheme = darkColorScheme(
    primary = MikeColors.Blue,
    onPrimary = MikeColors.White,
    primaryContainer = Color(0xFF142338),
    onPrimaryContainer = MikeColors.White,
    secondary = MikeColors.Green,
    onSecondary = MikeColors.Background,
    secondaryContainer = Color(0xFF15291D),
    onSecondaryContainer = MikeColors.Green,
    tertiary = MikeColors.Purple,
    onTertiary = MikeColors.White,
    tertiaryContainer = Color(0xFF241830),
    onTertiaryContainer = MikeColors.Purple,
    background = MikeColors.CanvasDark,
    onBackground = MikeColors.White,
    surface = MikeColors.Surface,
    onSurface = MikeColors.White,
    surfaceVariant = MikeColors.SurfaceElevated,
    onSurfaceVariant = MikeColors.Secondary,
    outline = MikeColors.OutlineSubtle,
    outlineVariant = MikeColors.DividerSubtle,
    error = MikeColors.Coral,
    onError = MikeColors.White
)

private val MikeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MikeDynamicIslandTheme(
    useDynamicColor: Boolean = false,
    accentColorOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        dynamicDarkColorScheme(context).copy(
            background = MikeColors.CanvasDark,
            surface = MikeColors.Surface
        )
    } else {
        MikeDarkColorScheme
    }

    val colorScheme = if (accentColorOverride != null) {
        baseScheme.copy(primary = accentColorOverride)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MikeTypography,
        shapes = MikeShapes,
        content = content
    )
}
