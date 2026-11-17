package com.mike.dynamicisland.faceid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MikeColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Composant réutilisable de base tel que défini dans le Module 01-C.
 * Centre transparent pour ne jamais masquer la caméra frontale punch-hole.
 */
@Composable
fun FaceScanAnimation(
    progress: Float,
    success: Boolean,
    modifier: Modifier = Modifier
) {
    val ringColor = if (success) {
        Color(0xFF38E6B0)
    } else {
        Color(0xFF2990FF)
    }

    Canvas(
        modifier = modifier
            .size(76.dp)
            .semantics { contentDescription = "Animation Face ID" }
    ) {
        val stroke = 3.dp.toPx()

        drawArc(
            color = ringColor.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(stroke)
        )

        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * Moteur de rendu complet des 6 styles Face ID (Module 01-B).
 * Règle matérielle : aucune InfiniteTransition active lorsque visible == false ou screenOn == false.
 * Le centre reste creux/transparent pour laisser la caméra punch-hole totalement dégagée.
 */
@Composable
fun StyledFaceScanOverlay(
    visible: Boolean,
    progress: Float,
    success: Boolean,
    interrupted: Boolean,
    isScreenOn: Boolean,
    settings: FaceAnimationSettings,
    maxParticleCount: Int = 8,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && isScreenOn,
        enter = fadeIn(tween(170)) + scaleIn(initialScale = 0.78f, animationSpec = tween(220)),
        exit = fadeOut(tween(240)) + scaleOut(targetScale = 0.88f, animationSpec = tween(240)),
        modifier = modifier
    ) {
        StyledFaceScanCanvas(
            style = settings.style,
            progress = progress,
            success = success,
            interrupted = interrupted,
            runContinuousMotion = visible && isScreenOn && !success && !interrupted,
            accentColor = settings.accentColor,
            successColor = settings.successColor,
            sizeDp = settings.sizeDp.dp,
            strokeWidthDp = settings.strokeWidthDp.dp,
            speedMultiplier = settings.speedMultiplier,
            maxParticleCount = maxParticleCount
        )
    }
}

@Composable
fun StyledFaceScanCanvas(
    style: FaceScanStyle,
    progress: Float,
    success: Boolean,
    interrupted: Boolean = false,
    runContinuousMotion: Boolean = true,
    accentColor: Color = Color(0xFF2990FF),
    successColor: Color = Color(0xFF38E6B0),
    sizeDp: Dp = 76.dp,
    strokeWidthDp: Dp = 3.dp,
    speedMultiplier: Float = 1f,
    maxParticleCount: Int = 8,
    modifier: Modifier = Modifier
) {
    val speed = speedMultiplier.coerceIn(0.5f, 2.2f)
    val cycleMs = (1600 / speed).toInt().coerceIn(650, 2600)

    // Ne crée l'InfiniteTransition que lorsque l'animation tourne activement et que l'écran est allumé.
    val orbitAngle: Float
    val scanLinePhase: Float
    val haloPulse: Float

    if (runContinuousMotion) {
        val transition = rememberInfiniteTransition(label = "face_scan_motion")
        val angleState by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = cycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbit_angle"
        )
        val scanState by transition.animateFloat(
            initialValue = -0.78f,
            targetValue = 0.78f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (cycleMs * 0.72f).toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scan_beam"
        )
        val pulseState by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (cycleMs * 0.65f).toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "halo_pulse"
        )
        orbitAngle = angleState
        scanLinePhase = scanState
        haloPulse = pulseState
    } else {
        orbitAngle = 0f
        scanLinePhase = 0f
        haloPulse = 1f
    }

    val successFlash by animateFloatAsState(
        targetValue = if (success) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "success_flash"
    )

    val activeColor = when {
        interrupted -> MikeColors.Coral
        success -> successColor
        else -> accentColor
    }
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp)
    ) {
        Canvas(
            modifier = Modifier
                .size(sizeDp)
                .semantics { contentDescription = "Style Face ID ${style.title}" }
        ) {
            val strokePx = strokeWidthDp.toPx().coerceAtLeast(2f)
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) - strokePx * 2.2f
            if (baseRadius <= 4f) return@Canvas

            when (style) {
                FaceScanStyle.BLUE_FACE_SCAN -> drawBlueFaceScan(
                    center = center,
                    radius = baseRadius,
                    strokePx = strokePx,
                    color = activeColor,
                    progress = clampedProgress,
                    scanLinePhase = scanLinePhase,
                    haloPulse = haloPulse,
                    successFlash = successFlash
                )

                FaceScanStyle.QUICK_GLASS_SCAN -> drawQuickGlassScan(
                    center = center,
                    radius = baseRadius,
                    strokePx = strokePx,
                    color = activeColor,
                    progress = clampedProgress,
                    orbitAngle = orbitAngle,
                    successFlash = successFlash
                )

                FaceScanStyle.GALAXY_NEON -> drawGalaxyNeon(
                    center = center,
                    radius = baseRadius,
                    strokePx = strokePx,
                    color = activeColor,
                    progress = clampedProgress,
                    orbitAngle = orbitAngle,
                    particleCount = maxParticleCount.coerceIn(4, 14),
                    successFlash = successFlash
                )

                FaceScanStyle.SAMSUNG_INSPIRED -> drawSamsungInspired(
                    center = center,
                    radius = baseRadius,
                    strokePx = strokePx,
                    color = activeColor,
                    progress = clampedProgress,
                    haloPulse = haloPulse,
                    successFlash = successFlash
                )

                FaceScanStyle.FACE_ID_CLASSIC -> drawFaceIdClassic(
                    center = center,
                    radius = baseRadius,
                    strokePx = strokePx,
                    color = activeColor,
                    progress = clampedProgress,
                    orbitAngle = orbitAngle,
                    successFlash = successFlash,
                    interrupted = interrupted
                )

                FaceScanStyle.MINIMAL_LIGHT -> drawMinimalLight(
                    center = center,
                    radius = baseRadius,
                    strokePx = strokePx,
                    color = activeColor,
                    progress = clampedProgress,
                    successFlash = successFlash
                )
            }

            if (interrupted) {
                drawInterruptedBadge(center = center, radius = baseRadius, color = MikeColors.Coral)
            }
        }
    }
}

private fun DrawScope.drawBlueFaceScan(
    center: Offset,
    radius: Float,
    strokePx: Float,
    color: Color,
    progress: Float,
    scanLinePhase: Float,
    haloPulse: Float,
    successFlash: Float
) {
    // Halo extérieur lumineux sans masquer le centre (caméra punch-hole)
    val haloRadius = radius * (1.14f * haloPulse)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.55f to Color.Transparent,
                0.82f to color.copy(alpha = 0.24f + successFlash * 0.20f),
                1.0f to Color.Transparent
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )

    // Piste de base
    drawCircle(
        color = color.copy(alpha = 0.18f),
        radius = radius,
        center = center,
        style = Stroke(width = strokePx)
    )

    // Arc de progression circulaire
    val arcSize = Size(radius * 2f, radius * 2f)
    val arcTopLeft = Offset(center.x - radius, center.y - radius)
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(color, MikeColors.Cyan, color),
            center = center
        ),
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokePx * 1.15f, cap = StrokeCap.Round)
    )

    // Ligne de balayage verticale traversant la périphérie (transparente au cœur)
    if (successFlash < 0.85f) {
        val beamY = center.y + scanLinePhase * radius * 0.88f
        val halfSpan = kotlin.math.sqrt((radius * radius - (beamY - center.y) * (beamY - center.y)).coerceAtLeast(0f)) * 0.86f
        if (halfSpan > 4f) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MikeColors.Cyan.copy(alpha = 0.85f * (1f - successFlash)),
                        color.copy(alpha = 0.22f * (1f - successFlash)),
                        MikeColors.Cyan.copy(alpha = 0.85f * (1f - successFlash)),
                        Color.Transparent
                    ),
                    startX = center.x - halfSpan,
                    endX = center.x + halfSpan
                ),
                start = Offset(center.x - halfSpan, beamY),
                end = Offset(center.x + halfSpan, beamY),
                strokeWidth = strokePx * 0.75f,
                cap = StrokeCap.Round
            )
        }
    }

    if (successFlash > 0.05f) {
        drawValidationBadge(center, radius, color, successFlash)
    }
}

private fun DrawScope.drawQuickGlassScan(
    center: Offset,
    radius: Float,
    strokePx: Float,
    color: Color,
    progress: Float,
    orbitAngle: Float,
    successFlash: Float
) {
    // Anneau vitré translucide (bordure épaisse semi-transparente, centre libre pour caméra)
    val glassRingThickness = strokePx * 2.8f
    drawCircle(
        color = MikeColors.Glass.copy(alpha = 0.58f),
        radius = radius,
        center = center,
        style = Stroke(width = glassRingThickness)
    )

    // Bordure externe de profondeur
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                MikeColors.White.copy(alpha = 0.45f),
                MikeColors.Cyan.copy(alpha = 0.35f),
                color.copy(alpha = 0.15f)
            )
        ),
        radius = radius + glassRingThickness / 2f,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )

    // Reflet cyan en rotation autour du verre
    rotate(degrees = orbitAngle, pivot = center) {
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    MikeColors.Cyan.copy(alpha = 0.88f),
                    MikeColors.White.copy(alpha = 0.72f),
                    Color.Transparent
                ),
                center = center
            ),
            startAngle = 0f,
            sweepAngle = 120f + 240f * progress,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokePx * 1.25f, cap = StrokeCap.Round)
        )
    }

    if (successFlash > 0.05f) {
        drawValidationBadge(center, radius, color, successFlash)
    }
}

private fun DrawScope.drawGalaxyNeon(
    center: Offset,
    radius: Float,
    strokePx: Float,
    color: Color,
    progress: Float,
    orbitAngle: Float,
    particleCount: Int,
    successFlash: Float
) {
    val outerRadius = radius
    val innerRadius = radius * 0.80f

    // Anneau externe rotatif
    rotate(degrees = orbitAngle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(color, MikeColors.Cyan, MikeColors.Purple, color),
                center = center
            ),
            startAngle = -90f,
            sweepAngle = (270f * progress).coerceAtLeast(90f) + successFlash * 90f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2f, outerRadius * 2f),
            style = Stroke(width = strokePx * 1.1f, cap = StrokeCap.Round)
        )
    }

    // Deuxième anneau contre-rotatif contrôlé
    rotate(degrees = -orbitAngle * 0.75f, pivot = center) {
        drawArc(
            color = MikeColors.Cyan.copy(alpha = 0.75f),
            startAngle = 40f,
            sweepAngle = 150f * progress.coerceAtLeast(0.35f),
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = Size(innerRadius * 2f, innerRadius * 2f),
            style = Stroke(width = strokePx * 0.75f, cap = StrokeCap.Round)
        )
    }

    // Petites particules orbitales légères (nombre plafonné selon DeviceProfile)
    val baseOrbit = orbitAngle * (PI.toFloat() / 180f)
    val particleOrbitRadius = outerRadius + strokePx * 1.65f
    for (i in 0 until particleCount) {
        val theta = baseOrbit + (i.toFloat() / particleCount.toFloat()) * (2f * PI.toFloat())
        val px = center.x + cos(theta) * particleOrbitRadius
        val py = center.y + sin(theta) * particleOrbitRadius
        val dotRadius = if (i % 2 == 0) strokePx * 0.55f else strokePx * 0.35f
        val dotColor = if (i % 2 == 0) MikeColors.Cyan else color
        drawCircle(
            color = dotColor.copy(alpha = 0.65f + successFlash * 0.35f),
            radius = dotRadius,
            center = Offset(px, py)
        )
    }

    // Éclair lumineux de validation
    if (successFlash > 0.05f) {
        drawCircle(
            color = color.copy(alpha = 0.36f * successFlash),
            radius = outerRadius * (1.06f + successFlash * 0.14f),
            center = center,
            style = Stroke(width = strokePx * 2.2f)
        )
        drawValidationBadge(center, outerRadius, color, successFlash)
    }
}

private fun DrawScope.drawSamsungInspired(
    center: Offset,
    radius: Float,
    strokePx: Float,
    color: Color,
    progress: Float,
    haloPulse: Float,
    successFlash: Float
) {
    // Halo bleu compact façon One UI
    val compactRadius = radius * 0.90f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.68f to Color.Transparent,
                0.88f to color.copy(alpha = 0.28f * haloPulse),
                1.0f to Color.Transparent
            ),
            center = center,
            radius = compactRadius * 1.18f
        ),
        radius = compactRadius * 1.18f,
        center = center
    )

    // Fin indicateur circulaire
    drawCircle(
        color = MikeColors.White.copy(alpha = 0.12f),
        radius = compactRadius,
        center = center,
        style = Stroke(width = strokePx * 0.85f)
    )
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - compactRadius, center.y - compactRadius),
        size = Size(compactRadius * 2f, compactRadius * 2f),
        style = Stroke(width = strokePx * 0.95f, cap = StrokeCap.Round)
    )

    if (successFlash > 0.05f) {
        drawValidationBadge(center, compactRadius, color, successFlash)
    }
}

private fun DrawScope.drawFaceIdClassic(
    center: Offset,
    radius: Float,
    strokePx: Float,
    color: Color,
    progress: Float,
    orbitAngle: Float,
    successFlash: Float,
    interrupted: Boolean
) {
    // 4 coins biométriques ouverts arrondis (géométrie Face ID fidèle) sans obstruer le centre caméra
    val half = radius * 0.98f
    val arcR = radius * 0.34f
    val arm = radius * 0.16f
    val left = center.x - half
    val top = center.y - half
    val right = center.x + half
    val bottom = center.y + half
    val bracketStroke = Stroke(width = strokePx * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val bracketColor = color.copy(alpha = if (interrupted) 0.92f else 0.42f + successFlash * 0.48f)

    // Coin haut-gauche
    drawPath(
        path = Path().apply {
            moveTo(left, top + arcR + arm)
            lineTo(left, top + arcR)
            quadraticTo(left, top, left + arcR, top)
            lineTo(left + arcR + arm, top)
        },
        color = bracketColor,
        style = bracketStroke
    )
    // Coin haut-droit
    drawPath(
        path = Path().apply {
            moveTo(right - arcR - arm, top)
            lineTo(right - arcR, top)
            quadraticTo(right, top, right, top + arcR)
            lineTo(right, top + arcR + arm)
        },
        color = bracketColor,
        style = bracketStroke
    )
    // Coin bas-droit
    drawPath(
        path = Path().apply {
            moveTo(right, bottom - arcR - arm)
            lineTo(right, bottom - arcR)
            quadraticTo(right, bottom, right - arcR, bottom)
            lineTo(right - arcR - arm, bottom)
        },
        color = bracketColor,
        style = bracketStroke
    )
    // Coin bas-gauche
    drawPath(
        path = Path().apply {
            moveTo(left + arcR + arm, bottom)
            lineTo(left + arcR, bottom)
            quadraticTo(left, bottom, left, bottom - arcR)
            lineTo(left, bottom - arcR - arm)
        },
        color = bracketColor,
        style = bracketStroke
    )

    // Anneau scanner circulaire sobre
    rotate(degrees = orbitAngle * 0.55f, pivot = center) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.80f, center.y - radius * 0.80f),
            size = Size(radius * 1.60f, radius * 1.60f),
            style = Stroke(width = strokePx * 1.0f, cap = StrokeCap.Round)
        )
    }

    if (successFlash > 0.05f && !interrupted) {
        drawValidationBadge(center, radius, color, successFlash)
    }
}

private fun DrawScope.drawMinimalLight(
    center: Offset,
    radius: Float,
    strokePx: Float,
    color: Color,
    progress: Float,
    successFlash: Float
) {
    val microRadius = radius * 0.84f
    val thinStroke = (strokePx * 0.65f).coerceAtLeast(1.8f)

    drawCircle(
        color = color.copy(alpha = 0.12f),
        radius = microRadius,
        center = center,
        style = Stroke(width = thinStroke)
    )
    drawArc(
        color = color.copy(alpha = 0.92f),
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - microRadius, center.y - microRadius),
        size = Size(microRadius * 2f, microRadius * 2f),
        style = Stroke(width = thinStroke, cap = StrokeCap.Round)
    )

    if (successFlash > 0.1f) {
        // Confirmation subtile sur le bord bas sans remplir la caméra
        drawCircle(
            color = color.copy(alpha = 0.85f * successFlash),
            radius = thinStroke * 1.8f,
            center = Offset(center.x, center.y + microRadius)
        )
    }
}

/**
 * Badge de confirmation placé sur le bord inférieur droit de l'anneau afin de
 * garder le centre strictement libre autour de l'objectif punch-hole.
 */
private fun DrawScope.drawValidationBadge(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float
) {
    val badgeCenter = Offset(
        x = center.x + radius * 0.72f,
        y = center.y + radius * 0.72f
    )
    val badgeRadius = (radius * 0.28f).coerceIn(7.dp.toPx(), 13.dp.toPx())
    drawCircle(
        color = MikeColors.Background.copy(alpha = 0.92f * alpha),
        radius = badgeRadius * 1.15f,
        center = badgeCenter
    )
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = badgeRadius,
        center = badgeCenter
    )

    val checkPath = Path().apply {
        moveTo(badgeCenter.x - badgeRadius * 0.44f, badgeCenter.y + badgeRadius * 0.02f)
        lineTo(badgeCenter.x - badgeRadius * 0.12f, badgeCenter.y + badgeRadius * 0.34f)
        lineTo(badgeCenter.x + badgeRadius * 0.46f, badgeCenter.y - badgeRadius * 0.30f)
    }
    drawPath(
        path = checkPath,
        color = MikeColors.Background.copy(alpha = alpha),
        style = Stroke(
            width = (badgeRadius * 0.24f).coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Badge d'interruption / échec discret sur le bord inférieur droit (#FF453A).
 */
private fun DrawScope.drawInterruptedBadge(
    center: Offset,
    radius: Float,
    color: Color
) {
    val badgeCenter = Offset(
        x = center.x + radius * 0.72f,
        y = center.y + radius * 0.72f
    )
    val badgeRadius = (radius * 0.26f).coerceIn(7.dp.toPx(), 12.dp.toPx())
    drawCircle(color = MikeColors.Background.copy(alpha = 0.94f), radius = badgeRadius * 1.15f, center = badgeCenter)
    drawCircle(color = color, radius = badgeRadius, center = badgeCenter)

    val cross = badgeRadius * 0.36f
    val stroke = (badgeRadius * 0.22f).coerceAtLeast(1.6f)
    drawLine(
        color = MikeColors.White,
        start = Offset(badgeCenter.x - cross, badgeCenter.y - cross),
        end = Offset(badgeCenter.x + cross, badgeCenter.y + cross),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = MikeColors.White,
        start = Offset(badgeCenter.x + cross, badgeCenter.y - cross),
        end = Offset(badgeCenter.x - cross, badgeCenter.y + cross),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}
