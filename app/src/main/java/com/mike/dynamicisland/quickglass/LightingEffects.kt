package com.mike.dynamicisland.quickglass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MikeColors

/**
 * Effets Quick Glass sobres et maîtrisés pour 60 Hz sur PowerVR GE8320 (Android 10 sans RenderEffect).
 * Remplace les dégradés bleutés omniprésents par des couches neutres translucides haute précision.
 */
object LightingEffects {

    /**
     * Arrière-plan sobre et profond (#09090B -> #000000) garantissant un contraste net sans teinte néon.
     */
    fun ambientScreenBackdrop(): Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF0E0E11),
            0.28f to MikeColors.CanvasDark,
            1.0f to MikeColors.Background
        )
    )

    /**
     * Couche vitrée neutre stratifiée (#1A1A1E -> #121214) avec profondeur discrète.
     */
    fun glassLayerGradient(
        baseAlpha: Float,
        tintColor: Color
    ): Brush {
        val clamped = baseAlpha.coerceIn(0.62f, 0.96f)
        return Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B1B1F).copy(alpha = (clamped + 0.04f).coerceAtMost(0.98f)),
                MikeColors.CardElevated.copy(alpha = clamped)
            )
        )
    }

    /**
     * Filet de bordure subtil (hairline) : blanc très fin en haut qui s'estompe naturellement,
     * avec un souffle discret d'accent uniquement si demandé.
     */
    fun specularBorderBrush(
        accentColor: Color,
        intensity: Float
    ): Brush {
        val clamped = intensity.coerceIn(0.06f, 0.42f)
        return Brush.verticalGradient(
            colors = listOf(
                MikeColors.White.copy(alpha = (clamped * 0.65f).coerceAtMost(0.24f)),
                accentColor.copy(alpha = (clamped * 0.45f).coerceAtMost(0.20f)),
                MikeColors.White.copy(alpha = 0.04f)
            )
        )
    }

    fun pillGlassGradient(): Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF18181B),
            Color(0xFF121214)
        )
    )
}

/**
 * Halo ambiant ultra-discret réservé aux zones de prévisualisation optique (Quick Glass Studio).
 */
fun Modifier.mikeAmbientHalo(
    primaryColor: Color = MikeColors.Blue,
    secondaryColor: Color = MikeColors.White,
    intensity: Float = 0.16f
): Modifier = drawBehind {
    val clamped = intensity.coerceIn(0f, 0.36f)
    if (clamped <= 0.01f) return@drawBehind
    val radius = size.maxDimension * 0.65f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = clamped * 0.28f),
                secondaryColor.copy(alpha = clamped * 0.08f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.28f, size.height * 0.22f),
            radius = radius
        ),
        radius = radius,
        center = Offset(size.width * 0.28f, size.height * 0.22f)
    )
}
