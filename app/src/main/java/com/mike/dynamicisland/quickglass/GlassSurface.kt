package com.mike.dynamicisland.quickglass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MikeColors

/**
 * Surface Quick Glass minimaliste et sobre (Android 10 compatible sans RenderEffect) :
 * - Fond élevé #121214 légèrement translucide
 * - Contour fin 0.75 dp sans néon excessif
 * - Reflet spéculaire supérieur 0.5 dp discret
 */
@Composable
fun QuickGlassSurface(
    modifier: Modifier = Modifier,
    config: QuickGlassConfig = QuickGlassConfig(),
    accentColor: Color = MikeColors.White,
    cornerRadius: Dp = config.cornerRadiusDp.dp.coerceIn(18.dp, 28.dp),
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isNeutralAccent = accentColor == MikeColors.White || accentColor == MikeColors.OutlineSubtle
    val borderIntensity = if (isNeutralAccent) {
        (config.borderGlowIntensity * 0.55f).coerceIn(0.08f, 0.20f)
    } else {
        (config.borderGlowIntensity * 0.85f).coerceIn(0.14f, 0.34f)
    }

    val baseModifier = modifier
        .clip(shape)
        .background(
            brush = LightingEffects.glassLayerGradient(
                baseAlpha = config.glassAlpha.coerceAtLeast(0.82f),
                tintColor = accentColor
            )
        )
        .border(
            width = 0.75.dp,
            brush = LightingEffects.specularBorderBrush(
                accentColor = accentColor,
                intensity = borderIntensity
            ),
            shape = shape
        )
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Box(modifier = baseModifier) {
        if (config.specularHighlightEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MikeColors.White.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}
