package com.mike.dynamicisland.island

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import kotlin.math.PI
import kotlin.math.sin

/**
 * Glyphe vectoriel haute précision pour chaque activité de la Dynamic Island.
 * Privilégie nos VectorDrawables originaux (ic_mike_*) reproduisant les silhouettes de référence
 * plutôt que des icônes génériques.
 */
@Composable
fun ActivityVectorGlyph(
    activity: IslandActivity,
    tint: Color,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val customRes = customDrawableResFor(activity)
    if (customRes != null) {
        Icon(
            painter = painterResource(id = customRes),
            contentDescription = activity.kind.title,
            tint = tint,
            modifier = modifier.size(size)
        )
    } else {
        Icon(
            imageVector = v7IconForActivity(activity),
            contentDescription = activity.kind.title,
            tint = tint,
            modifier = modifier.size(size)
        )
    }
}

@DrawableRes
fun customDrawableResFor(activity: IslandActivity): Int? = when (activity) {
    is IslandActivity.FaceIdIndicator -> R.drawable.ic_mike_face_id
    is IslandActivity.AccessoryConnected -> R.drawable.ic_mike_earbuds
    is IslandActivity.SilentMode -> R.drawable.ic_mike_silent_bell
    is IslandActivity.DoNotDisturb -> R.drawable.ic_mike_dnd_moon
    is IslandActivity.VoiceMemo -> R.drawable.ic_mike_voice_memo
    is IslandActivity.Call -> if (activity.isVideoCall) null else R.drawable.ic_mike_call_accept
    is IslandActivity.Battery -> if (activity.isCharging) R.drawable.ic_mike_battery_bolt else null
    else -> null
}

/**
 * Onde audio minimaliste haute fidélité calibrée pour 60 Hz sur PowerVR GE8320 :
 * - Lorsque [isActive] == true : micro-oscillation fluide (une seule phase partagée).
 * - Lorsque [isActive] == false : aucune InfiniteTransition créée (0 % CPU/GPU au repos).
 */
@Composable
fun AnimatedWaveformBars(
    color: Color,
    isActive: Boolean,
    barCount: Int = 5,
    modifier: Modifier = Modifier
) {
    val baseHeights = remember(barCount) {
        listOf(0.38f, 0.92f, 0.58f, 0.84f, 0.44f, 0.74f).take(barCount.coerceIn(3, 6))
    }

    val phase: Float = if (isActive) {
        val transition = rememberInfiniteTransition(label = "mike_waveform_motion")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1050, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveform_phase"
        )
        animated
    } else {
        0f
    }

    Canvas(modifier = modifier.size(width = (baseHeights.size * 4 + 4).dp, height = 16.dp)) {
        val barWidth = 2.1.dp.toPx()
        val gap = (size.width - barWidth * baseHeights.size) / (baseHeights.size - 1).coerceAtLeast(1)
        baseHeights.forEachIndexed { idx, base ->
            val factor = if (isActive) {
                val wave = 0.5f + 0.5f * sin(phase + idx * 1.15f)
                (0.28f + wave * 0.68f).coerceIn(0.24f, 0.98f)
            } else {
                base * 0.68f
            }
            val barH = (size.height * factor).coerceAtLeast(3.dp.toPx())
            val x = idx * (barWidth + gap)
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Compatibilité pour les appels statiques (0 animation continue).
 */
@Composable
fun StaticWaveformBars(
    color: Color,
    barCount: Int = 5,
    modifier: Modifier = Modifier
) {
    AnimatedWaveformBars(
        color = color,
        isActive = false,
        barCount = barCount,
        modifier = modifier
    )
}

/**
 * Capsule batterie vectorielle horizontale épurée avec niveau réel.
 */
@Composable
fun MinimalBatteryCapsule(
    levelPercent: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fillRatio = (levelPercent / 100f).coerceIn(0.06f, 1f)
    Canvas(modifier = modifier.size(width = 24.dp, height = 12.dp)) {
        val stroke = 1.2.dp.toPx()
        val capW = 2.dp.toPx()
        val bodyW = size.width - capW - 1.5.dp.toPx()
        val bodyH = size.height - stroke
        val top = stroke / 2f
        val radius = 3.dp.toPx()

        drawRoundRect(
            color = MikeDesignTokens.TextSecondary.copy(alpha = 0.55f),
            topLeft = Offset(stroke / 2f, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )

        val inset = 2.dp.toPx()
        val innerW = (bodyW - inset * 2f).coerceAtLeast(2f) * fillRatio
        val innerH = (bodyH - inset * 2f).coerceAtLeast(2f)
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2f + inset, top + inset),
            size = Size(innerW, innerH),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )

        drawRoundRect(
            color = MikeDesignTokens.TextSecondary.copy(alpha = 0.65f),
            topLeft = Offset(bodyW + 2.dp.toPx(), size.height * 0.30f),
            size = Size(capW, size.height * 0.40f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
    }
}

@Composable
fun CompactActionPill(
    text: String,
    icon: ImageVector?,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.18f),
        modifier = modifier.heightIn(min = 38.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MikeDesignTokens.TextPrimary,
                maxLines = 1
            )
        }
    }
}

fun v7IconForActivity(activity: IslandActivity): ImageVector = when (activity) {
    is IslandActivity.Timer -> Icons.Filled.Timer
    is IslandActivity.Stopwatch -> Icons.Outlined.Timer
    is IslandActivity.Alarm -> Icons.Filled.Alarm
    is IslandActivity.Music -> Icons.Filled.MusicNote
    is IslandActivity.Call -> if (activity.isVideoCall) Icons.Filled.Videocam else Icons.Filled.Call
    is IslandActivity.Notification -> Icons.Filled.NotificationsActive
    is IslandActivity.Navigation -> Icons.Filled.TurnRight
    is IslandActivity.Download -> Icons.Filled.Download
    is IslandActivity.SilentMode -> Icons.Filled.NotificationsOff
    is IslandActivity.DoNotDisturb -> Icons.Filled.Bedtime
    is IslandActivity.AccessoryConnected -> Icons.Filled.Headphones
    is IslandActivity.FaceIdIndicator -> Icons.Filled.Face
    is IslandActivity.VoiceMemo -> Icons.Filled.Mic
    is IslandActivity.HomeEvent -> Icons.Filled.Home
    is IslandActivity.Battery -> when (activity.eventType) {
        BatteryEventType.CHARGER_CONNECTED -> Icons.Filled.BatteryChargingFull
        BatteryEventType.BATTERY_LOW -> Icons.Filled.BatteryAlert
        BatteryEventType.BATTERY_FULL -> Icons.Filled.BatteryFull
        BatteryEventType.CHARGER_DISCONNECTED -> Icons.Filled.BatteryStd
    }
}
