package com.mike.dynamicisland.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mike.dynamicisland.notifications.MikeNotificationListenerService

data class PermissionAudit(
    val overlayGranted: Boolean,
    val notificationListenerGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val batteryOptimizationIgnored: Boolean
) {
    val allCoreGranted: Boolean
        get() = overlayGranted && postNotificationsGranted

    val grantedCount: Int
        get() = listOf(
            overlayGranted,
            notificationListenerGranted,
            postNotificationsGranted,
            batteryOptimizationIgnored
        ).count { it }

    val totalCount: Int = 4
}

/**
 * Gère la vérification réelle des permissions et l'ouverture des écrans système Android.
 */
object PermissionManager {

    fun audit(context: Context): PermissionAudit {
        return PermissionAudit(
            overlayGranted = hasOverlayPermission(context),
            notificationListenerGranted = hasNotificationListenerPermission(context),
            postNotificationsGranted = hasPostNotificationsPermission(context),
            batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context)
        )
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasNotificationListenerPermission(context: Context): Boolean {
        val enabledListeners = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
        val component = ComponentName(context, MikeNotificationListenerService::class.java)
        return enabledListeners.contains(component.flattenToString()) ||
            enabledListeners.contains(component.flattenToShortString())
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return runCatching {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(false)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
