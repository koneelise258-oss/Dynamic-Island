package com.mike.dynamicisland.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.settings.PrivacySettings

/**
 * Contrôleur d'activités issues des notifications (PARTIE 4).
 * Étend NotificationController pour détecter proprement les appels entrants/actifs,
 * les notifications domotiques locales et les transferts de fichiers sans jamais stocker hors appareil.
 */
class NotificationActivityController(
    val baseController: NotificationController,
    private val activityManager: MikeActivityManager,
    private val eventManager: EventManager
) {
    val isListenerConnected = baseController.isListenerConnected
    val activeEntries = baseController.activeEntries

    fun attachListener(connected: Boolean, onCancelNotification: ((String) -> Unit)? = null) {
        baseController.attachListener(connected, onCancelNotification)
    }

    fun updateFilters(
        allowedPackages: Set<String>,
        privacySettings: PrivacySettings,
        deviceLocked: Boolean
    ) {
        baseController.updateFilters(allowedPackages, privacySettings, deviceLocked)
    }

    fun onNotificationPosted(context: Context, sbn: StatusBarNotification) {
        baseController.onNotificationPosted(context, sbn)
        classifyExtendedCategories(sbn)
    }

    fun onNotificationRemoved(key: String) {
        baseController.onNotificationRemoved(key)
        activityManager.remove("home_$key")
        activityManager.remove("memo_$key")
    }

    fun dismissNotification(key: String) = baseController.dismissNotification(key)
    fun triggerAction(key: String, actionIndex: Int): Boolean = baseController.triggerAction(key, actionIndex)
    fun openNotification(key: String): Boolean = baseController.openNotification(key)

    private fun classifyExtendedCategories(sbn: StatusBarNotification) {
        val notif = sbn.notification ?: return
        val pkg = sbn.packageName.orEmpty()
        val title = notif.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = notif.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()

        // Détection des notifications domotiques réelles (Google Home, SmartThings, Mi Home)
        if (pkg in HOME_PACKAGES && (title.isNotBlank() || text.isNotBlank())) {
            activityManager.upsert(
                IslandActivity.HomeEvent(
                    id = "home_${sbn.key}",
                    deviceTitle = title.ifBlank { "Domotique" },
                    statusText = text.ifBlank { "Confirmé" },
                    updatedAtMs = sbn.postTime
                )
            )
            eventManager.record("Domotique", "Événement réel détecté ($pkg)", true)
        }

        // Détection d'un appel entrant (présence d'actions Répondre/Refuser ou fullScreenIntent)
        if (notif.category == Notification.CATEGORY_CALL) {
            val hasIncomingCue = notif.fullScreenIntent != null ||
                (notif.actions?.size ?: 0) >= 2
            val existing = activityManager.activities.value["call_${sbn.key}"] as? IslandActivity.Call
            if (existing != null && hasIncomingCue) {
                activityManager.upsert(
                    existing.copy(
                        isIncoming = true,
                        isVideoCall = text.contains("vidéo", ignoreCase = true) ||
                            text.contains("video", ignoreCase = true)
                    )
                )
            }
        }
    }

    companion object {
        private val HOME_PACKAGES = setOf(
            "com.google.android.apps.chromecast.app",
            "com.samsung.android.oneconnect",
            "com.xiaomi.smarthome"
        )
    }
}
