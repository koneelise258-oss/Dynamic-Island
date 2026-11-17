package com.mike.dynamicisland.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.Gravity
import com.mike.dynamicisland.faceid.FacePositionMode
import com.mike.dynamicisland.quickglass.IslandBlueprintMetrics
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.settings.IslandAlignment

enum class CutoutHorizontalZone(val label: String) {
    LEFT("Haut gauche (Punch-Hole)"),
    CENTER("Haut centré"),
    RIGHT("Haut droit"),
    UNKNOWN("Non détecté (Calibration manuelle)")
}

data class OverlayWindowBounds(
    val gravity: Int,
    val xPx: Int,
    val yPx: Int,
    val maxWidthPx: Int,
    val maxHeightPx: Int
)

/**
 * Gestionnaire dédié DisplayCutout & WindowInsets (PARTIE 3 & PARTIE 4).
 * Ne suppose jamais que la caméra frontale est centrée (TECNO Camon 15 Air : punch-hole à gauche),
 * respecte la barre d'état et garantit que la fenêtre flottante reste strictement compacte en haut.
 */
object DisplayCutoutManager {

    fun detectZone(profile: DeviceProfile): CutoutHorizontalZone {
        val cutout = profile.displayCutoutBounds
            ?: return if (profile.isTecnoOrHiOsDevice) CutoutHorizontalZone.LEFT else CutoutHorizontalZone.UNKNOWN
        val third = profile.screenWidth / 3
        return when {
            cutout.centerXPx < third -> CutoutHorizontalZone.LEFT
            cutout.centerXPx > third * 2 -> CutoutHorizontalZone.RIGHT
            else -> CutoutHorizontalZone.CENTER
        }
    }

    fun extractTopCutout(cutout: DisplayCutout?): CutoutBounds? {
        if (cutout == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val rects: List<Rect> = cutout.boundingRects
        val topRect = rects.firstOrNull { it.top <= 140 && it.width() > 0 && it.height() > 0 }
            ?: return null
        return CutoutBounds(
            leftPx = topRect.left,
            topPx = topRect.top,
            rightPx = topRect.right,
            bottomPx = topRect.bottom,
            isDetectedFromSystem = true
        )
    }

    fun getStatusBarHeightPx(context: Context): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val fromRes = if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        val density = context.resources.displayMetrics.density
        return fromRes.takeIf { it > 0 } ?: (28f * density).toInt()
    }

    fun updateProfileFromActivity(activity: Activity, current: DeviceProfile): DeviceProfile {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return current
        val insets = activity.window?.decorView?.rootWindowInsets ?: return current
        val detected = extractTopCutout(insets.displayCutout) ?: return current
        return current.copy(
            hasDisplayCutout = true,
            displayCutoutBounds = detected
        )
    }

    /**
     * Calcule la géométrie strictement bornée pour l'anneau Face ID autour du punch-hole.
     * Ne dépasse jamais MikeDesignTokens.MaxFaceWindowSizeDp.
     */
    fun computeFaceOverlayBounds(
        profile: DeviceProfile,
        density: Float,
        ringSizeDp: Float,
        positionMode: FacePositionMode,
        manualOffsetXDp: Float,
        manualOffsetYDp: Float
    ): OverlayWindowBounds {
        val clampedRingDp = ringSizeDp.coerceIn(48f, MikeDesignTokens.MaxFaceWindowSizeDp.value - 8f)
        val windowPx = ((clampedRingDp + 8f) * density).toInt()
            .coerceAtMost((MikeDesignTokens.MaxFaceWindowSizeDp.value * density).toInt())
        val cutout = profile.recommendedPunchHoleBoundsPx()
        val maxX = (profile.screenWidth - windowPx).coerceAtLeast(0)
        val maxY = (profile.screenHeight / 4).coerceAtLeast(windowPx)

        val (gravity, rawX, rawY) = when (positionMode) {
            FacePositionMode.AUTO_CUTOUT, FacePositionMode.MANUAL -> Triple(
                Gravity.TOP or Gravity.START,
                cutout.centerXPx - windowPx / 2 + (manualOffsetXDp * density).toInt(),
                cutout.centerYPx - windowPx / 2 + (manualOffsetYDp * density).toInt()
            )
            FacePositionMode.TOP_LEFT_PUNCH_HOLE -> Triple(
                Gravity.TOP or Gravity.START,
                ((14f + manualOffsetXDp) * density).toInt(),
                ((6f + manualOffsetYDp) * density).toInt()
            )
            FacePositionMode.TOP_CENTER -> Triple(
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                (manualOffsetXDp * density).toInt(),
                ((8f + manualOffsetYDp) * density).toInt()
            )
        }

        val safeX = if (gravity and Gravity.CENTER_HORIZONTAL == Gravity.CENTER_HORIZONTAL) {
            rawX.coerceIn(-maxX / 2, maxX / 2)
        } else {
            rawX.coerceIn(0, maxX)
        }
        val safeY = rawY.coerceIn(0, maxY)

        return OverlayWindowBounds(
            gravity = gravity,
            xPx = safeX,
            yPx = safeY,
            maxWidthPx = windowPx,
            maxHeightPx = windowPx
        )
    }

    /**
     * Calcule la géométrie strictement bornée de la Dynamic Island (Niveau 1 Mini / Niveau 2 Smart).
     * Garantit que la fenêtre flottante reste compacte en haut de l'écran et respecte l'heure/icônes.
     */
    fun computeIslandOverlayBounds(
        profile: DeviceProfile,
        density: Float,
        isExpanded: Boolean,
        alignment: IslandAlignment,
        verticalOffsetDp: Float,
        horizontalOffsetDp: Float
    ): OverlayWindowBounds {
        val screenWidthDp = (profile.screenWidth / density).coerceAtLeast(320f)
        val blueprint = IslandBlueprintMetrics.forScreenWidthDp(screenWidthDp)
        val marginPx = (blueprint.safeMarginHorizontal.value * density).toInt()
        val maxAllowedW = (profile.screenWidth - marginPx * 2).coerceAtLeast((160f * density).toInt())

        val targetMaxWidthPx = if (isExpanded) {
            (blueprint.expandedMaxWidth.value * density).toInt().coerceAtMost(maxAllowedW)
        } else {
            (blueprint.compactMaxWidth.value * density).toInt().coerceAtMost(maxAllowedW)
        }

        // Plafond absolu : jamais plein écran (<= 216dp développé, <= 54dp compact)
        val targetMaxHeightPx = if (isExpanded) {
            (MikeDesignTokens.MaxFloatingWindowHeightDp.value * density).toInt()
        } else {
            (54f * density).toInt()
        }

        val cutoutZone = detectZone(profile)
        val cutout = profile.recommendedPunchHoleBoundsPx()

        val (gravity, xPx) = when {
            isExpanded -> {
                // En mode Smart Island développé (Niveau 2), toujours centré en haut avec marges sûres 12dp
                Gravity.TOP or Gravity.CENTER_HORIZONTAL to (horizontalOffsetDp * density).toInt()
            }
            alignment == IslandAlignment.TOP_LEFT -> {
                val safeLeft = (cutout.leftPx + (horizontalOffsetDp * density).toInt())
                    .coerceIn(marginPx, (profile.screenWidth - targetMaxWidthPx).coerceAtLeast(marginPx))
                Gravity.TOP or Gravity.START to safeLeft
            }
            alignment == IslandAlignment.AUTO_CUTOUT && cutoutZone == CutoutHorizontalZone.LEFT -> {
                // Sur TECNO Camon 15 Air (caméra gauche) : décale proprement à droite de l'objectif ou centre pour ne pas masquer l'heure/icônes
                val afterCutout = (cutout.rightPx + (6f * density).toInt() + (horizontalOffsetDp * density).toInt())
                    .coerceIn(marginPx, (profile.screenWidth - targetMaxWidthPx).coerceAtLeast(marginPx))
                Gravity.TOP or Gravity.START to afterCutout
            }
            else -> {
                Gravity.TOP or Gravity.CENTER_HORIZONTAL to (horizontalOffsetDp * density).toInt()
            }
        }

        val safeY = (verticalOffsetDp.coerceIn(4f, 44f) * density).toInt()

        return OverlayWindowBounds(
            gravity = gravity,
            xPx = xPx,
            yPx = safeY,
            maxWidthPx = targetMaxWidthPx,
            maxHeightPx = targetMaxHeightPx
        )
    }
}
