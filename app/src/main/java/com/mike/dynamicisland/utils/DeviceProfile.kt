package com.mike.dynamicisland.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager

enum class PerformanceTier(val label: String) {
    ECO_LOW_RAM("Éco / 3 Go RAM (Optimisé Helio P22)"),
    BALANCED("Équilibré (60 Hz)"),
    HIGH_PERFORMANCE("Haute Performance (90/120 Hz)")
}

data class CutoutBounds(
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
    val isDetectedFromSystem: Boolean
) {
    val centerXPx: Int get() = (leftPx + rightPx) / 2
    val centerYPx: Int get() = (topPx + bottomPx) / 2
    val widthPx: Int get() = (rightPx - leftPx).coerceAtLeast(0)
    val heightPx: Int get() = (bottomPx - topPx).coerceAtLeast(0)
}

/**
 * Profil matériel réel détecté au lancement (Module 11 & V7 Partie 3).
 * Cible prioritaire : TECNO Camon 15 Air (CD6/CD6S, Android 10, HiOS 6.0, 3 Go RAM, 720×1600 LCD, 60 Hz).
 */
data class DeviceProfile(
    val androidVersion: Int,
    val totalRamMb: Long,
    val screenWidth: Int,
    val screenHeight: Int,
    val refreshRate: Float,
    val hasDisplayCutout: Boolean,
    val densityDpi: Int = DisplayMetrics.DENSITY_XHIGH,
    val manufacturer: String = Build.MANUFACTURER.orEmpty(),
    val model: String = Build.MODEL.orEmpty(),
    val displayCutoutBounds: CutoutBounds? = null,
    val availableRamMb: Long = 1024L,
    val freeStorageMb: Long = 5400L
) {
    val density: Float
        get() = (densityDpi / 160f).coerceAtLeast(1f)

    val screenWidthDp: Float
        get() = (screenWidth / density).coerceAtLeast(320f)

    val isLowRamProfile: Boolean
        get() = totalRamMb <= 3600L || availableRamMb <= 1400L || androidVersion <= Build.VERSION_CODES.Q

    val isTecnoOrHiOsDevice: Boolean
        get() = manufacturer.contains("TECNO", ignoreCase = true) ||
            Build.BRAND.orEmpty().contains("TECNO", ignoreCase = true) ||
            model.contains("CD6", ignoreCase = true)

    val displayPanelNote: String
        get() = if (isTecnoOrHiOsDevice) {
            "LCD IPS 720×1600 (thème noir visuel — pas d'OLED matériel)"
        } else {
            "Thème sombre adaptatif (sans supposition OLED matérielle)"
        }

    val performanceTier: PerformanceTier
        get() = when {
            isLowRamProfile -> PerformanceTier.ECO_LOW_RAM
            refreshRate >= 89f && totalRamMb >= 6000L -> PerformanceTier.HIGH_PERFORMANCE
            else -> PerformanceTier.BALANCED
        }

    val maxParticleCount: Int
        get() = when (performanceTier) {
            PerformanceTier.ECO_LOW_RAM -> 8
            PerformanceTier.BALANCED -> 12
            PerformanceTier.HIGH_PERFORMANCE -> 16
        }

    val supportsHardwareBlur: Boolean
        get() = androidVersion >= Build.VERSION_CODES.S && !isLowRamProfile

    val maxAlbumArtSizePx: Int
        get() = if (isLowRamProfile) 96 else 144

    fun recommendedPunchHoleBoundsPx(): CutoutBounds {
        displayCutoutBounds?.let { return it }
        val estimatedDiameter = (32f * density).toInt()
        val topMargin = (10f * density).toInt()
        // Sur TECNO Camon 15 Air (CD6/CD6S), la caméra punch-hole est située en haut à gauche
        val leftX = if (isTecnoOrHiOsDevice) {
            (20f * density).toInt()
        } else {
            (screenWidth - estimatedDiameter) / 2
        }
        return CutoutBounds(
            leftPx = leftX,
            topPx = topMargin,
            rightPx = leftX + estimatedDiameter,
            bottomPx = topMargin + estimatedDiameter,
            isDetectedFromSystem = false
        )
    }
}

object DeviceProfileDetector {

    fun detect(context: Context): DeviceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamMb = (memoryInfo.totalMem / (1024L * 1024L)).coerceAtLeast(512L)
        val availRamMb = (memoryInfo.availMem / (1024L * 1024L)).coerceAtLeast(128L)

        val freeStorageMb = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBytes / (1024L * 1024L)).coerceAtLeast(0L)
        }.getOrDefault(0L)

        val displayMetrics = context.resources.displayMetrics
        var widthPx = displayMetrics.widthPixels
        var heightPx = displayMetrics.heightPixels
        var refreshRate = 60f
        var cutoutBounds: CutoutBounds? = null

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            val metrics = windowManager.currentWindowMetrics
            widthPx = metrics.bounds.width()
            heightPx = metrics.bounds.height()
            cutoutBounds = DisplayCutoutManager.extractTopCutout(metrics.windowInsets.displayCutout)
        } else if (windowManager != null) {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            if (display != null) {
                val realMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                display.getRealMetrics(realMetrics)
                if (realMetrics.widthPixels > 0) widthPx = realMetrics.widthPixels
                if (realMetrics.heightPixels > 0) heightPx = realMetrics.heightPixels
                refreshRate = display.refreshRate.takeIf { it > 1f } ?: 60f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cutoutBounds = DisplayCutoutManager.extractTopCutout(display.cutout)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val display = context.display
                if (display != null && display.refreshRate > 1f) {
                    refreshRate = display.refreshRate
                }
            }
        }

        return DeviceProfile(
            androidVersion = Build.VERSION.SDK_INT,
            totalRamMb = totalRamMb,
            screenWidth = widthPx.coerceAtLeast(360),
            screenHeight = heightPx.coerceAtLeast(640),
            refreshRate = refreshRate,
            hasDisplayCutout = cutoutBounds != null,
            densityDpi = displayMetrics.densityDpi,
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            displayCutoutBounds = cutoutBounds,
            availableRamMb = availRamMb,
            freeStorageMb = freeStorageMb
        )
    }

    fun updateCutoutFromActivity(activity: Activity, current: DeviceProfile): DeviceProfile {
        return DisplayCutoutManager.updateProfileFromActivity(activity, current)
    }
}
