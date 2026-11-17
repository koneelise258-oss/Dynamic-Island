package com.mike.dynamicisland.quickglass

import android.os.Build
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.utils.DeviceProfile

enum class QuickGlassPreset(val label: String, val description: String) {
    QUICK_GLASS_SIGNATURE(
        label = "Verre Fumé Minimal",
        description = "Translucidité sobre, filet 0.75 dp discret et lisibilité optimale"
    ),
    OBSIDIAN_STEALTH(
        label = "Obsidienne Mate",
        description = "Surface quasi opaque sans halo pour un confort nocturne absolu"
    ),
    CYBER_PRISM(
        label = "Reflet Cristal",
        description = "Transparence lumineuse légèrement teintée avec liseré supérieur fin"
    ),
    ONE_UI_FROST(
        label = "One UI Satin",
        description = "Sobriété Samsung One UI, contraste calme et coût GPU minimal"
    )
}

data class QuickGlassConfig(
    val preset: QuickGlassPreset = QuickGlassPreset.QUICK_GLASS_SIGNATURE,
    val glassAlpha: Float = 0.86f,
    val borderGlowIntensity: Float = 0.20f,
    val haloIntensity: Float = 0.16f,
    val specularHighlightEnabled: Boolean = true,
    val optionalHardwareBlurEnabled: Boolean = false,
    val accentTintArgb: Long = 0xFF0A84FFL,
    val cornerRadiusDp: Int = 22
) {
    val accentTint: Color
        get() = Color(accentTintArgb.toInt())
}

/**
 * Moteur Quick Glass compatible Android 10 (API 29) sans dépendance à RenderEffect (API 31+).
 * Adapte automatiquement les couches lumineuses selon le profil matériel détecté.
 */
object QuickGlassEngine {

    fun isHardwareBlurSupported(profile: DeviceProfile): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            profile.androidVersion >= Build.VERSION_CODES.S &&
            !profile.isLowRamProfile
    }

    fun resolveEffectiveConfig(
        raw: QuickGlassConfig,
        profile: DeviceProfile
    ): QuickGlassConfig {
        val canBlur = isHardwareBlurSupported(profile) && raw.optionalHardwareBlurEnabled
        val clampedHalo = if (profile.isLowRamProfile) {
            raw.haloIntensity.coerceAtMost(0.45f)
        } else {
            raw.haloIntensity.coerceIn(0f, 1f)
        }
        return raw.copy(
            glassAlpha = raw.glassAlpha.coerceIn(0.45f, 0.96f),
            borderGlowIntensity = raw.borderGlowIntensity.coerceIn(0.08f, 0.75f),
            haloIntensity = clampedHalo,
            optionalHardwareBlurEnabled = canBlur
        )
    }

    fun presetDefaults(preset: QuickGlassPreset): QuickGlassConfig = when (preset) {
        QuickGlassPreset.QUICK_GLASS_SIGNATURE -> QuickGlassConfig(
            preset = preset,
            glassAlpha = 0.86f,
            borderGlowIntensity = 0.20f,
            haloIntensity = 0.16f,
            specularHighlightEnabled = true,
            accentTintArgb = 0xFF0A84FFL,
            cornerRadiusDp = 22
        )
        QuickGlassPreset.OBSIDIAN_STEALTH -> QuickGlassConfig(
            preset = preset,
            glassAlpha = 0.94f,
            borderGlowIntensity = 0.12f,
            haloIntensity = 0.08f,
            specularHighlightEnabled = false,
            accentTintArgb = 0xFF8E8E93L,
            cornerRadiusDp = 20
        )
        QuickGlassPreset.CYBER_PRISM -> QuickGlassConfig(
            preset = preset,
            glassAlpha = 0.74f,
            borderGlowIntensity = 0.32f,
            haloIntensity = 0.26f,
            specularHighlightEnabled = true,
            accentTintArgb = 0xFF0A84FFL,
            cornerRadiusDp = 24
        )
        QuickGlassPreset.ONE_UI_FROST -> QuickGlassConfig(
            preset = preset,
            glassAlpha = 0.88f,
            borderGlowIntensity = 0.16f,
            haloIntensity = 0.12f,
            specularHighlightEnabled = true,
            accentTintArgb = 0xFFBF5AF2L,
            cornerRadiusDp = 22
        )
    }

    fun baseGlassColor(alpha: Float): Color =
        MikeColors.Glass.copy(alpha = alpha.coerceIn(0.4f, 0.96f))
}
