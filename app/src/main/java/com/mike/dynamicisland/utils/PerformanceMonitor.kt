package com.mike.dynamicisland.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

data class MemorySnapshot(
    val appUsedHeapMb: Float,
    val appMaxHeapMb: Float,
    val nativeHeapMb: Float,
    val systemAvailableRamMb: Long,
    val systemTotalRamMb: Long,
    val isSystemLowMemory: Boolean
) {
    val heapUsageRatio: Float
        get() = if (appMaxHeapMb > 0f) (appUsedHeapMb / appMaxHeapMb).coerceIn(0f, 1f) else 0f
}

/**
 * Mesure réelle de la mémoire JVM, native et système (aucune valeur fictive).
 */
object PerformanceMonitor {

    fun captureSnapshot(context: Context): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        val maxHeapMb = runtime.maxMemory() / (1024f * 1024f)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024f * 1024f)

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val availMb = memInfo.availMem / (1024L * 1024L)
        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)

        return MemorySnapshot(
            appUsedHeapMb = usedHeapMb,
            appMaxHeapMb = maxHeapMb,
            nativeHeapMb = nativeHeapMb,
            systemAvailableRamMb = availMb,
            systemTotalRamMb = totalMb,
            isSystemLowMemory = memInfo.lowMemory
        )
    }
}
