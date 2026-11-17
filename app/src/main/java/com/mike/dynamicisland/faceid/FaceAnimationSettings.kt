package com.mike.dynamicisland.faceid

import androidx.compose.ui.graphics.Color

/**
 * Les 6 styles d'animation Face ID définis dans le Module 01.
 */
enum class FaceScanStyle(
    val title: String,
    val subtitle: String,
    val gpuCostLabel: String
) {
    BLUE_FACE_SCAN(
        title = "Blue Face Scan",
        subtitle = "Cercle bleu lumineux, ligne de balayage verticale et halo extérieur",
        gpuCostLabel = "Léger"
    ),
    QUICK_GLASS_SCAN(
        title = "Quick Glass Scan",
        subtitle = "Anneau vitré translucide, reflets cyan et profondeur lumineuse",
        gpuCostLabel = "Léger"
    ),
    GALAXY_NEON(
        title = "Galaxy Neon",
        subtitle = "Double anneau en rotation contrôlée, micro-particules et éclair de validation",
        gpuCostLabel = "Modéré"
    ),
    SAMSUNG_INSPIRED(
        title = "Samsung Inspired",
        subtitle = "Halo bleu compact façon One UI, progression fine et validation discrète",
        gpuCostLabel = "Ultra-léger"
    ),
    FACE_ID_CLASSIC(
        title = "Face ID Classic",
        subtitle = "Anneau biométrique lumineux, scanner circulaire et symbole de validation",
        gpuCostLabel = "Léger"
    ),
    MINIMAL_LIGHT(
        title = "Minimal Light",
        subtitle = "Petit cercle épuré, animation courte et consommation minimale",
        gpuCostLabel = "Minimal"
    )
}

enum class FacePositionMode(val label: String) {
    AUTO_CUTOUT("Détection caméra auto"),
    TOP_LEFT_PUNCH_HOLE("Punch-Hole gauche (Camon 15 Air)"),
    TOP_CENTER("Haut centré"),
    MANUAL("Calibration manuelle")
}

data class FaceAccentPreset(
    val name: String,
    val argb: Long
) {
    val color: Color get() = Color(argb.toInt())
}

object FaceAccentPresets {
    val all = listOf(
        FaceAccentPreset("Bleu Mike", 0xFF2990FFL),
        FaceAccentPreset("Cyan Glass", 0xFF39DDF2L),
        FaceAccentPreset("Violet Galaxy", 0xFF9567FFL),
        FaceAccentPreset("Émeraude", 0xFF38E6B0L),
        FaceAccentPreset("Glace Blanche", 0xFFEAF4FFL)
    )
}

data class FaceAnimationSettings(
    val enabled: Boolean = true,
    val style: FaceScanStyle = FaceScanStyle.BLUE_FACE_SCAN,
    val accentColorArgb: Long = 0xFF2990FFL,
    val successColorArgb: Long = 0xFF38E6B0L,
    val sizeDp: Float = 76f,
    val strokeWidthDp: Float = 3f,
    val speedMultiplier: Float = 1.0f,
    val durationMs: Int = 1100,
    val holdAfterSuccessMs: Int = 650,
    val positionMode: FacePositionMode = FacePositionMode.AUTO_CUTOUT,
    val manualOffsetXDp: Float = 0f,
    val manualOffsetYDp: Float = 0f,
    val vibrateOnSuccess: Boolean = true,
    val triggerOnDegradedKeyguardUnlock: Boolean = true
) {
    val accentColor: Color get() = Color(accentColorArgb.toInt())
    val successColor: Color get() = Color(successColorArgb.toInt())
}
