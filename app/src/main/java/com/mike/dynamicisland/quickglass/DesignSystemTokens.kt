package com.mike.dynamicisland.quickglass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Jetons officiels issus du Design System JSON V7
 * (OLED Dark Mode / Apple Dynamic Island UI Kit × Samsung One UI × Quick Glass).
 *
 * Note matérielle TECNO Camon 15 Air :
 * L'écran 720×1600 est une dalle LCD IPS. Le noir pur #000000 est conservé pour
 * le contraste visuel et l'intégration discrète près de la caméra punch-hole,
 * sans prétendre à une extinction matérielle des pixels OLED.
 */
object MikeDesignTokens {
    // Surfaces JSON
    val CanvasBackgroundLight = Color(0xFFF4F5FA)
    val CanvasBackgroundNeutral = Color(0xFFC4C7CE)
    val IslandBackground = Color(0xFF000000)
    val CardElevatedSurface = Color(0xFF121214)
    val PillButtonSurface = Color(0xFF1C1C1E)
    val ActiveSurfaceLight = Color(0xFFFFFFFF)

    // Couleurs JSON
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val AccentGreen = Color(0xFF30D158)
    val AccentBlue = Color(0xFF0A84FF)
    val AccentOrange = Color(0xFFFF9F0A)
    val AccentRed = Color(0xFFFF453A)
    val AccentPurple = Color(0xFFBF5AF2)
    val AccentYellow = Color(0xFFFFD60A)

    // Géométrie de référence (base JSON : 390pt)
    const val REFERENCE_SCREEN_WIDTH_PT = 390f
    val SafeAreaMarginHorizontal: Dp = 12.dp
    val TopOffset: Dp = 12.dp
    val BorderRadiusPill: Dp = 999.dp
    val BorderRadiusCardSm: Dp = 24.dp
    val BorderRadiusCardMd: Dp = 28.dp
    val BorderRadiusCardLg: Dp = 36.dp

    // Plafonds stricts de fenêtre flottante (PARTIE 1 — jamais plein écran)
    val MaxFloatingWindowHeightDp: Dp = 216.dp
    val MaxFaceWindowSizeDp: Dp = 124.dp
}

/**
 * Mesures Blueprint (Source JSON #3) converties dynamiquement en dp Android
 * selon la largeur réelle de l'écran (ex. 360dp sur TECNO Camon 15 Air 720×1600).
 */
data class IslandBlueprintMetrics(
    val scaleFactor: Float,
    val safeMarginHorizontal: Dp,
    val topMargin: Dp,
    val statusBarHeight: Dp,
    val idlePillWidth: Dp,
    val compactHeight: Dp,
    val compactMaxWidth: Dp,
    val compactLeadingWidth: Dp,
    val compactTrailingWidth: Dp,
    val compactInnerHeight: Dp,
    val expandedMaxWidth: Dp,
    val expandedMediaHeight: Dp,
    val expandedCallHeight: Dp,
    val albumArtSize: Dp,
    val waveformIconSize: Dp,
    val scrubberMaxWidth: Dp,
    val scrubberHeight: Dp,
    val contactAvatarSize: Dp,
    val callActionButtonSize: Dp,
    val callButtonGap: Dp
) {
    companion object {
        fun forScreenWidthDp(screenWidthDp: Float): IslandBlueprintMetrics {
            val safeWidth = screenWidthDp.coerceIn(320f, 440f)
            val scale = (safeWidth / MikeDesignTokens.REFERENCE_SCREEN_WIDTH_PT).coerceIn(0.84f, 1.06f)
            val marginH = 12.dp
            val maxExpandedW = (safeWidth - 24f).coerceAtMost(366f).dp

            return IslandBlueprintMetrics(
                scaleFactor = scale,
                safeMarginHorizontal = marginH,
                topMargin = 10.dp,
                statusBarHeight = 36.dp,
                idlePillWidth = (122f * scale).dp,
                compactHeight = 36.dp,
                compactMaxWidth = (314f * scale).coerceAtMost(safeWidth - 24f).dp,
                compactLeadingWidth = (65f * scale).dp,
                compactTrailingWidth = (73f * scale).dp,
                compactInnerHeight = 22.dp,
                expandedMaxWidth = maxExpandedW,
                expandedMediaHeight = (200f * scale).coerceIn(176f, 206f).dp,
                expandedCallHeight = (74f * scale).coerceIn(68f, 80f).dp,
                albumArtSize = (64f * scale).coerceIn(52f, 64.dp.value).dp,
                waveformIconSize = 24.dp,
                scrubberMaxWidth = (318f * scale).coerceAtMost(safeWidth - 48f).dp,
                scrubberHeight = 22.dp,
                contactAvatarSize = (50f * scale).coerceIn(44f, 52f).dp,
                callActionButtonSize = 44.dp,
                callButtonGap = 12.dp
            )
        }
    }
}
