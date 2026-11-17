package com.mike.dynamicisland.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mike.dynamicisland.faceid.FaceAnimationSettings
import com.mike.dynamicisland.faceid.FacePositionMode
import com.mike.dynamicisland.faceid.FaceScanStyle
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.quickglass.QuickGlassConfig
import com.mike.dynamicisland.quickglass.QuickGlassPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mikeDataStore: DataStore<Preferences> by preferencesDataStore(name = "mike_dynamic_island_prefs")

enum class IslandAlignment(val label: String) {
    AUTO_CUTOUT("Aligné sur la caméra"),
    TOP_CENTER("Haut centré"),
    TOP_LEFT("Haut gauche")
}

data class IslandSettings(
    val overlayServiceEnabled: Boolean = false,
    val autoStartOnBoot: Boolean = false,
    val alignment: IslandAlignment = IslandAlignment.AUTO_CUTOUT,
    val compactWidthDp: Float = 168f,
    val compactHeightDp: Float = 36f,
    val verticalOffsetDp: Float = 8f,
    val horizontalOffsetDp: Float = 0f,
    val tapToExpandEnabled: Boolean = true,
    val autoCollapseSeconds: Int = 8,
    val allowMultipleActivities: Boolean = true,
    val enabledActivities: Set<ActivityKind> = ActivityKind.entries.toSet(),
    val priorityOrder: List<ActivityKind> = listOf(
        ActivityKind.CALL,
        ActivityKind.ALARM,
        ActivityKind.FACE_ID,
        ActivityKind.TIMER,
        ActivityKind.NAVIGATION,
        ActivityKind.MUSIC,
        ActivityKind.STOPWATCH,
        ActivityKind.SILENT_MODE,
        ActivityKind.DO_NOT_DISTURB,
        ActivityKind.ACCESSORY,
        ActivityKind.NOTIFICATION,
        ActivityKind.VOICE_MEMO,
        ActivityKind.DOWNLOAD,
        ActivityKind.HOME_EVENT,
        ActivityKind.BATTERY
    ),
    val allowedNotificationPackages: Set<String> = DEFAULT_PRIORITY_PACKAGES
) {
    companion object {
        val DEFAULT_PRIORITY_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.google.android.gm",
            "org.telegram.messenger",
            "com.google.android.dialer",
            "com.android.dialer"
        )
    }
}

data class PrivacySettings(
    val hideNotificationContentWhenLocked: Boolean = true,
    val privacyModeMaskPreview: Boolean = false,
    val localOnlyStrictMode: Boolean = true
)

data class AppearanceSettings(
    val useDynamicMaterialColors: Boolean = false,
    val compactOneUiHeader: Boolean = true,
    val quickStarCompactClockBadge: Boolean = true,
    val quickStarHideIdleIndicators: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true
)

data class MikeAppSettings(
    val faceId: FaceAnimationSettings = FaceAnimationSettings(),
    val island: IslandSettings = IslandSettings(),
    val quickGlass: QuickGlassConfig = QuickGlassConfig(),
    val privacy: PrivacySettings = PrivacySettings(),
    val appearance: AppearanceSettings = AppearanceSettings()
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        // Face ID
        val FACE_ENABLED = booleanPreferencesKey("face_enabled")
        val FACE_STYLE = stringPreferencesKey("face_style")
        val FACE_ACCENT_ARGB = longPreferencesKey("face_accent_argb")
        val FACE_SUCCESS_ARGB = longPreferencesKey("face_success_argb")
        val FACE_SIZE_DP = floatPreferencesKey("face_size_dp")
        val FACE_STROKE_DP = floatPreferencesKey("face_stroke_dp")
        val FACE_SPEED = floatPreferencesKey("face_speed")
        val FACE_DURATION_MS = intPreferencesKey("face_duration_ms")
        val FACE_POS_MODE = stringPreferencesKey("face_pos_mode")
        val FACE_OFFSET_X = floatPreferencesKey("face_offset_x")
        val FACE_OFFSET_Y = floatPreferencesKey("face_offset_y")
        val FACE_VIBRATE = booleanPreferencesKey("face_vibrate")
        val FACE_DEGRADED_TRIGGER = booleanPreferencesKey("face_degraded_trigger")

        // Dynamic Island
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        val ISLAND_ALIGNMENT = stringPreferencesKey("island_alignment")
        val ISLAND_WIDTH_DP = floatPreferencesKey("island_width_dp")
        val ISLAND_HEIGHT_DP = floatPreferencesKey("island_height_dp")
        val ISLAND_OFFSET_Y = floatPreferencesKey("island_offset_y")
        val ISLAND_OFFSET_X = floatPreferencesKey("island_offset_x")
        val ISLAND_TAP_EXPAND = booleanPreferencesKey("island_tap_expand")
        val ISLAND_AUTO_COLLAPSE = intPreferencesKey("island_auto_collapse")
        val ISLAND_MULTI_ACTIVITY = booleanPreferencesKey("island_multi_activity")
        val ISLAND_ENABLED_ACTIVITIES = stringSetPreferencesKey("island_enabled_activities")
        val ISLAND_PRIORITY_ORDER = stringPreferencesKey("island_priority_order")
        val ALLOWED_PACKAGES = stringSetPreferencesKey("allowed_packages")

        // Quick Glass
        val GLASS_PRESET = stringPreferencesKey("glass_preset")
        val GLASS_ALPHA = floatPreferencesKey("glass_alpha")
        val GLASS_BORDER_GLOW = floatPreferencesKey("glass_border_glow")
        val GLASS_HALO = floatPreferencesKey("glass_halo")
        val GLASS_SPECULAR = booleanPreferencesKey("glass_specular")
        val GLASS_OPTIONAL_BLUR = booleanPreferencesKey("glass_optional_blur")
        val GLASS_TINT_ARGB = longPreferencesKey("glass_tint_argb")
        val GLASS_CORNER_DP = intPreferencesKey("glass_corner_dp")

        // Privacy & Appearance
        val PRIVACY_HIDE_LOCKED = booleanPreferencesKey("privacy_hide_locked")
        val PRIVACY_MASK_PREVIEW = booleanPreferencesKey("privacy_mask_preview")
        val APPEARANCE_DYNAMIC_COLOR = booleanPreferencesKey("appearance_dynamic_color")
        val APPEARANCE_ONE_UI_COMPACT = booleanPreferencesKey("appearance_one_ui_compact")
        val APPEARANCE_QUICKSTAR_CLOCK = booleanPreferencesKey("appearance_quickstar_clock")
        val APPEARANCE_QUICKSTAR_HIDE_IDLE = booleanPreferencesKey("appearance_quickstar_hide_idle")
        val APPEARANCE_HAPTIC = booleanPreferencesKey("appearance_haptic")
    }

    val settingsFlow: Flow<MikeAppSettings> = context.mikeDataStore.data.map { prefs ->
        val faceStyle = prefs[Keys.FACE_STYLE]
            ?.let { runCatching { FaceScanStyle.valueOf(it) }.getOrNull() }
            ?: FaceScanStyle.BLUE_FACE_SCAN
        val facePos = prefs[Keys.FACE_POS_MODE]
            ?.let { runCatching { FacePositionMode.valueOf(it) }.getOrNull() }
            ?: FacePositionMode.AUTO_CUTOUT
        val islandAlign = prefs[Keys.ISLAND_ALIGNMENT]
            ?.let { runCatching { IslandAlignment.valueOf(it) }.getOrNull() }
            ?: IslandAlignment.AUTO_CUTOUT
        val glassPreset = prefs[Keys.GLASS_PRESET]
            ?.let { runCatching { QuickGlassPreset.valueOf(it) }.getOrNull() }
            ?: QuickGlassPreset.QUICK_GLASS_SIGNATURE

        val enabledActivities = prefs[Keys.ISLAND_ENABLED_ACTIVITIES]
            ?.mapNotNull { runCatching { ActivityKind.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: ActivityKind.entries.toSet()

        val defaultOrder = IslandSettings().priorityOrder
        val savedOrder = prefs[Keys.ISLAND_PRIORITY_ORDER]
            ?.split(",")
            ?.mapNotNull { runCatching { ActivityKind.valueOf(it) }.getOrNull() }
            ?.distinct()
            ?.let { parsed -> parsed + defaultOrder.filterNot { it in parsed } }
            ?: defaultOrder

        MikeAppSettings(
            faceId = FaceAnimationSettings(
                enabled = prefs[Keys.FACE_ENABLED] ?: true,
                style = faceStyle,
                accentColorArgb = prefs[Keys.FACE_ACCENT_ARGB] ?: 0xFF2990FFL,
                successColorArgb = prefs[Keys.FACE_SUCCESS_ARGB] ?: 0xFF38E6B0L,
                sizeDp = prefs[Keys.FACE_SIZE_DP] ?: 76f,
                strokeWidthDp = prefs[Keys.FACE_STROKE_DP] ?: 3f,
                speedMultiplier = prefs[Keys.FACE_SPEED] ?: 1.0f,
                durationMs = prefs[Keys.FACE_DURATION_MS] ?: 1100,
                positionMode = facePos,
                manualOffsetXDp = prefs[Keys.FACE_OFFSET_X] ?: 0f,
                manualOffsetYDp = prefs[Keys.FACE_OFFSET_Y] ?: 0f,
                vibrateOnSuccess = prefs[Keys.FACE_VIBRATE] ?: true,
                triggerOnDegradedKeyguardUnlock = prefs[Keys.FACE_DEGRADED_TRIGGER] ?: true
            ),
            island = IslandSettings(
                overlayServiceEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
                autoStartOnBoot = prefs[Keys.AUTO_START_BOOT] ?: false,
                alignment = islandAlign,
                compactWidthDp = prefs[Keys.ISLAND_WIDTH_DP] ?: 168f,
                compactHeightDp = prefs[Keys.ISLAND_HEIGHT_DP] ?: 36f,
                verticalOffsetDp = prefs[Keys.ISLAND_OFFSET_Y] ?: 8f,
                horizontalOffsetDp = prefs[Keys.ISLAND_OFFSET_X] ?: 0f,
                tapToExpandEnabled = prefs[Keys.ISLAND_TAP_EXPAND] ?: true,
                autoCollapseSeconds = prefs[Keys.ISLAND_AUTO_COLLAPSE] ?: 8,
                allowMultipleActivities = prefs[Keys.ISLAND_MULTI_ACTIVITY] ?: true,
                enabledActivities = enabledActivities,
                priorityOrder = savedOrder,
                allowedNotificationPackages = prefs[Keys.ALLOWED_PACKAGES]
                    ?: IslandSettings.DEFAULT_PRIORITY_PACKAGES
            ),
            quickGlass = QuickGlassConfig(
                preset = glassPreset,
                glassAlpha = prefs[Keys.GLASS_ALPHA] ?: 0.72f,
                borderGlowIntensity = prefs[Keys.GLASS_BORDER_GLOW] ?: 0.28f,
                haloIntensity = prefs[Keys.GLASS_HALO] ?: 0.35f,
                specularHighlightEnabled = prefs[Keys.GLASS_SPECULAR] ?: true,
                optionalHardwareBlurEnabled = prefs[Keys.GLASS_OPTIONAL_BLUR] ?: false,
                accentTintArgb = prefs[Keys.GLASS_TINT_ARGB] ?: 0xFF39DDF2L,
                cornerRadiusDp = prefs[Keys.GLASS_CORNER_DP] ?: 24
            ),
            privacy = PrivacySettings(
                hideNotificationContentWhenLocked = prefs[Keys.PRIVACY_HIDE_LOCKED] ?: true,
                privacyModeMaskPreview = prefs[Keys.PRIVACY_MASK_PREVIEW] ?: false,
                localOnlyStrictMode = true
            ),
            appearance = AppearanceSettings(
                useDynamicMaterialColors = prefs[Keys.APPEARANCE_DYNAMIC_COLOR] ?: false,
                compactOneUiHeader = prefs[Keys.APPEARANCE_ONE_UI_COMPACT] ?: true,
                quickStarCompactClockBadge = prefs[Keys.APPEARANCE_QUICKSTAR_CLOCK] ?: true,
                quickStarHideIdleIndicators = prefs[Keys.APPEARANCE_QUICKSTAR_HIDE_IDLE] ?: true,
                hapticFeedbackEnabled = prefs[Keys.APPEARANCE_HAPTIC] ?: true
            )
        )
    }

    suspend fun updateFaceSettings(transform: (FaceAnimationSettings) -> FaceAnimationSettings) {
        context.mikeDataStore.edit { prefs ->
            val current = FaceAnimationSettings(
                enabled = prefs[Keys.FACE_ENABLED] ?: true,
                style = prefs[Keys.FACE_STYLE]?.let { runCatching { FaceScanStyle.valueOf(it) }.getOrNull() }
                    ?: FaceScanStyle.BLUE_FACE_SCAN,
                accentColorArgb = prefs[Keys.FACE_ACCENT_ARGB] ?: 0xFF2990FFL,
                successColorArgb = prefs[Keys.FACE_SUCCESS_ARGB] ?: 0xFF38E6B0L,
                sizeDp = prefs[Keys.FACE_SIZE_DP] ?: 76f,
                strokeWidthDp = prefs[Keys.FACE_STROKE_DP] ?: 3f,
                speedMultiplier = prefs[Keys.FACE_SPEED] ?: 1.0f,
                durationMs = prefs[Keys.FACE_DURATION_MS] ?: 1100,
                positionMode = prefs[Keys.FACE_POS_MODE]?.let { runCatching { FacePositionMode.valueOf(it) }.getOrNull() }
                    ?: FacePositionMode.AUTO_CUTOUT,
                manualOffsetXDp = prefs[Keys.FACE_OFFSET_X] ?: 0f,
                manualOffsetYDp = prefs[Keys.FACE_OFFSET_Y] ?: 0f,
                vibrateOnSuccess = prefs[Keys.FACE_VIBRATE] ?: true,
                triggerOnDegradedKeyguardUnlock = prefs[Keys.FACE_DEGRADED_TRIGGER] ?: true
            )
            val updated = transform(current)
            prefs[Keys.FACE_ENABLED] = updated.enabled
            prefs[Keys.FACE_STYLE] = updated.style.name
            prefs[Keys.FACE_ACCENT_ARGB] = updated.accentColorArgb
            prefs[Keys.FACE_SUCCESS_ARGB] = updated.successColorArgb
            prefs[Keys.FACE_SIZE_DP] = updated.sizeDp
            prefs[Keys.FACE_STROKE_DP] = updated.strokeWidthDp
            prefs[Keys.FACE_SPEED] = updated.speedMultiplier
            prefs[Keys.FACE_DURATION_MS] = updated.durationMs
            prefs[Keys.FACE_POS_MODE] = updated.positionMode.name
            prefs[Keys.FACE_OFFSET_X] = updated.manualOffsetXDp
            prefs[Keys.FACE_OFFSET_Y] = updated.manualOffsetYDp
            prefs[Keys.FACE_VIBRATE] = updated.vibrateOnSuccess
            prefs[Keys.FACE_DEGRADED_TRIGGER] = updated.triggerOnDegradedKeyguardUnlock
        }
    }

    suspend fun updateIslandSettings(transform: (IslandSettings) -> IslandSettings) {
        context.mikeDataStore.edit { prefs ->
            val currentEnabled = prefs[Keys.ISLAND_ENABLED_ACTIVITIES]
                ?.mapNotNull { runCatching { ActivityKind.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: ActivityKind.entries.toSet()
            val defaultOrder = IslandSettings().priorityOrder
            val currentOrder = prefs[Keys.ISLAND_PRIORITY_ORDER]
                ?.split(",")
                ?.mapNotNull { runCatching { ActivityKind.valueOf(it) }.getOrNull() }
                ?.distinct()
                ?.let { parsed -> parsed + defaultOrder.filterNot { it in parsed } }
                ?: defaultOrder

            val current = IslandSettings(
                overlayServiceEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
                autoStartOnBoot = prefs[Keys.AUTO_START_BOOT] ?: false,
                alignment = prefs[Keys.ISLAND_ALIGNMENT]?.let { runCatching { IslandAlignment.valueOf(it) }.getOrNull() }
                    ?: IslandAlignment.AUTO_CUTOUT,
                compactWidthDp = prefs[Keys.ISLAND_WIDTH_DP] ?: 168f,
                compactHeightDp = prefs[Keys.ISLAND_HEIGHT_DP] ?: 36f,
                verticalOffsetDp = prefs[Keys.ISLAND_OFFSET_Y] ?: 8f,
                horizontalOffsetDp = prefs[Keys.ISLAND_OFFSET_X] ?: 0f,
                tapToExpandEnabled = prefs[Keys.ISLAND_TAP_EXPAND] ?: true,
                autoCollapseSeconds = prefs[Keys.ISLAND_AUTO_COLLAPSE] ?: 8,
                allowMultipleActivities = prefs[Keys.ISLAND_MULTI_ACTIVITY] ?: true,
                enabledActivities = currentEnabled,
                priorityOrder = currentOrder,
                allowedNotificationPackages = prefs[Keys.ALLOWED_PACKAGES]
                    ?: IslandSettings.DEFAULT_PRIORITY_PACKAGES
            )
            val updated = transform(current)
            prefs[Keys.OVERLAY_ENABLED] = updated.overlayServiceEnabled
            prefs[Keys.AUTO_START_BOOT] = updated.autoStartOnBoot
            prefs[Keys.ISLAND_ALIGNMENT] = updated.alignment.name
            prefs[Keys.ISLAND_WIDTH_DP] = updated.compactWidthDp
            prefs[Keys.ISLAND_HEIGHT_DP] = updated.compactHeightDp
            prefs[Keys.ISLAND_OFFSET_Y] = updated.verticalOffsetDp
            prefs[Keys.ISLAND_OFFSET_X] = updated.horizontalOffsetDp
            prefs[Keys.ISLAND_TAP_EXPAND] = updated.tapToExpandEnabled
            prefs[Keys.ISLAND_AUTO_COLLAPSE] = updated.autoCollapseSeconds
            prefs[Keys.ISLAND_MULTI_ACTIVITY] = updated.allowMultipleActivities
            prefs[Keys.ISLAND_ENABLED_ACTIVITIES] = updated.enabledActivities.map { it.name }.toSet()
            prefs[Keys.ISLAND_PRIORITY_ORDER] = updated.priorityOrder.joinToString(",") { it.name }
            prefs[Keys.ALLOWED_PACKAGES] = updated.allowedNotificationPackages
        }
    }

    suspend fun updateQuickGlass(transform: (QuickGlassConfig) -> QuickGlassConfig) {
        context.mikeDataStore.edit { prefs ->
            val current = QuickGlassConfig(
                preset = prefs[Keys.GLASS_PRESET]?.let { runCatching { QuickGlassPreset.valueOf(it) }.getOrNull() }
                    ?: QuickGlassPreset.QUICK_GLASS_SIGNATURE,
                glassAlpha = prefs[Keys.GLASS_ALPHA] ?: 0.72f,
                borderGlowIntensity = prefs[Keys.GLASS_BORDER_GLOW] ?: 0.28f,
                haloIntensity = prefs[Keys.GLASS_HALO] ?: 0.35f,
                specularHighlightEnabled = prefs[Keys.GLASS_SPECULAR] ?: true,
                optionalHardwareBlurEnabled = prefs[Keys.GLASS_OPTIONAL_BLUR] ?: false,
                accentTintArgb = prefs[Keys.GLASS_TINT_ARGB] ?: 0xFF39DDF2L,
                cornerRadiusDp = prefs[Keys.GLASS_CORNER_DP] ?: 24
            )
            val updated = transform(current)
            prefs[Keys.GLASS_PRESET] = updated.preset.name
            prefs[Keys.GLASS_ALPHA] = updated.glassAlpha
            prefs[Keys.GLASS_BORDER_GLOW] = updated.borderGlowIntensity
            prefs[Keys.GLASS_HALO] = updated.haloIntensity
            prefs[Keys.GLASS_SPECULAR] = updated.specularHighlightEnabled
            prefs[Keys.GLASS_OPTIONAL_BLUR] = updated.optionalHardwareBlurEnabled
            prefs[Keys.GLASS_TINT_ARGB] = updated.accentTintArgb
            prefs[Keys.GLASS_CORNER_DP] = updated.cornerRadiusDp
        }
    }

    suspend fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings) {
        context.mikeDataStore.edit { prefs ->
            val current = PrivacySettings(
                hideNotificationContentWhenLocked = prefs[Keys.PRIVACY_HIDE_LOCKED] ?: true,
                privacyModeMaskPreview = prefs[Keys.PRIVACY_MASK_PREVIEW] ?: false
            )
            val updated = transform(current)
            prefs[Keys.PRIVACY_HIDE_LOCKED] = updated.hideNotificationContentWhenLocked
            prefs[Keys.PRIVACY_MASK_PREVIEW] = updated.privacyModeMaskPreview
        }
    }

    suspend fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        context.mikeDataStore.edit { prefs ->
            val current = AppearanceSettings(
                useDynamicMaterialColors = prefs[Keys.APPEARANCE_DYNAMIC_COLOR] ?: false,
                compactOneUiHeader = prefs[Keys.APPEARANCE_ONE_UI_COMPACT] ?: true,
                quickStarCompactClockBadge = prefs[Keys.APPEARANCE_QUICKSTAR_CLOCK] ?: true,
                quickStarHideIdleIndicators = prefs[Keys.APPEARANCE_QUICKSTAR_HIDE_IDLE] ?: true,
                hapticFeedbackEnabled = prefs[Keys.APPEARANCE_HAPTIC] ?: true
            )
            val updated = transform(current)
            prefs[Keys.APPEARANCE_DYNAMIC_COLOR] = updated.useDynamicMaterialColors
            prefs[Keys.APPEARANCE_ONE_UI_COMPACT] = updated.compactOneUiHeader
            prefs[Keys.APPEARANCE_QUICKSTAR_CLOCK] = updated.quickStarCompactClockBadge
            prefs[Keys.APPEARANCE_QUICKSTAR_HIDE_IDLE] = updated.quickStarHideIdleIndicators
            prefs[Keys.APPEARANCE_HAPTIC] = updated.hapticFeedbackEnabled
        }
    }
}
