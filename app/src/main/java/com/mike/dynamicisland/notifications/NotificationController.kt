package com.mike.dynamicisland.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.NotificationActionItem
import com.mike.dynamicisland.settings.PrivacySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PriorityAppInfo(
    val packageName: String,
    val displayName: String,
    val category: String
)

data class StoredNotificationEntry(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAtMs: Long,
    val isOngoing: Boolean,
    val category: String?,
    val actions: List<NotificationActionItem>,
    val actionIntents: List<PendingIntent?>,
    val contentIntent: PendingIntent?,
    val progress: Int,
    val progressMax: Int,
    val progressIndeterminate: Boolean
)

/**
 * Intelligence des notifications strictement locale en mémoire (Module 06).
 * Aucune donnée n'est copiée vers un serveur ou une base distante.
 */
class NotificationController(
    private val activityManager: MikeActivityManager,
    private val eventManager: EventManager
) {
    private val _isListenerConnected = MutableStateFlow(false)
    val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

    private val _activeEntries = MutableStateFlow<Map<String, StoredNotificationEntry>>(emptyMap())
    val activeEntries: StateFlow<Map<String, StoredNotificationEntry>> = _activeEntries.asStateFlow()

    @Volatile
    private var allowedPackages: Set<String> = PRIORITY_APPS.map { it.packageName }.toSet()

    @Volatile
    private var privacySettings: PrivacySettings = PrivacySettings()

    @Volatile
    private var isDeviceLocked: Boolean = false

    @Volatile
    private var cancelDelegate: ((String) -> Unit)? = null

    fun attachListener(connected: Boolean, onCancelNotification: ((String) -> Unit)? = null) {
        _isListenerConnected.value = connected
        cancelDelegate = if (connected) onCancelNotification else null
        eventManager.record(
            "Notifications",
            if (connected) "NotificationListenerService connecté" else "NotificationListenerService déconnecté",
            true
        )
        if (!connected) {
            _activeEntries.value = emptyMap()
            activityManager.removeByKind(ActivityKind.NOTIFICATION)
            activityManager.removeByKind(ActivityKind.CALL)
            activityManager.removeByKind(ActivityKind.NAVIGATION)
            activityManager.removeByKind(ActivityKind.DOWNLOAD)
        }
    }

    fun updateFilters(
        allowedPackages: Set<String>,
        privacySettings: PrivacySettings,
        deviceLocked: Boolean
    ) {
        this.allowedPackages = allowedPackages
        this.privacySettings = privacySettings
        this.isDeviceLocked = deviceLocked
        republishToIsland()
    }

    fun onNotificationPosted(context: Context, sbn: StatusBarNotification) {
        if (sbn.packageName == context.packageName) return
        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty().trim()

        if (title.isBlank() && text.isBlank() && notification.category != Notification.CATEGORY_CALL) {
            return
        }

        val appName = resolveAppLabel(context, sbn.packageName)
        val rawActions = notification.actions?.take(3).orEmpty()
        val actionItems = rawActions.mapIndexed { index, action ->
            NotificationActionItem(index = index, title = action.title?.toString().orEmpty().ifBlank { "Action" })
        }
        val actionIntents = rawActions.map { it.actionIntent }

        val progress = extras?.getInt(Notification.EXTRA_PROGRESS, 0) ?: 0
        val progressMax = extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0
        val progressIndeterminate = extras?.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) ?: false

        val entry = StoredNotificationEntry(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title.ifBlank { appName },
            text = text,
            postedAtMs = sbn.postTime,
            isOngoing = sbn.isOngoing,
            category = notification.category,
            actions = actionItems,
            actionIntents = actionIntents,
            contentIntent = notification.contentIntent,
            progress = progress,
            progressMax = progressMax,
            progressIndeterminate = progressIndeterminate
        )

        ingestEntry(entry, isRealSystemEvent = true)
    }

    /**
     * Méthode testable directement (JVM/Robolectric) pour l'ingestion et le filtrage.
     */
    fun ingestEntry(entry: StoredNotificationEntry, isRealSystemEvent: Boolean = true) {
        _activeEntries.update { it + (entry.key to entry) }
        eventManager.record(
            "Notifications",
            "Reçue : ${entry.appName} (${entry.category ?: "standard"})",
            isRealSystemEvent
        )
        republishToIsland()
    }

    fun onNotificationRemoved(key: String) {
        if (key !in _activeEntries.value) return
        _activeEntries.update { it - key }
        republishToIsland()
    }

    fun dismissNotification(key: String) {
        cancelDelegate?.invoke(key)
        onNotificationRemoved(key)
        eventManager.record("Notifications", "Notification ignorée", true)
    }

    fun triggerAction(key: String, actionIndex: Int): Boolean {
        val entry = _activeEntries.value[key] ?: return false
        val pendingIntent = entry.actionIntents.getOrNull(actionIndex) ?: return false
        return runCatching {
            pendingIntent.send()
            eventManager.record("Notifications", "Action exécutée sur ${entry.appName}", true)
            true
        }.getOrDefault(false)
    }

    fun openNotification(key: String): Boolean {
        val entry = _activeEntries.value[key] ?: return false
        val contentIntent = entry.contentIntent ?: return false
        return runCatching {
            contentIntent.send()
            eventManager.record("Notifications", "Ouverture ${entry.appName}", true)
            true
        }.getOrDefault(false)
    }

    private fun republishToIsland() {
        val entries = _activeEntries.value.values.toList()
        val maskContent = privacySettings.privacyModeMaskPreview ||
            (privacySettings.hideNotificationContentWhenLocked && isDeviceLocked)

        // 1. Appels actifs (priorité maximale)
        val calls = entries
            .filter { it.category == Notification.CATEGORY_CALL }
            .map { entry ->
                IslandActivity.Call(
                    id = "call_${entry.key}",
                    callerName = entry.title,
                    statusText = entry.text.ifBlank { "Appel en cours" },
                    startedAtElapsedMs = SystemClock.elapsedRealtime(),
                    packageName = entry.packageName,
                    notificationKey = entry.key,
                    actions = entry.actions,
                    updatedAtMs = entry.postedAtMs
                )
            }
        activityManager.replaceKind(ActivityKind.CALL, calls)

        // 2. Navigation en cours (Google Maps / Waze / CATEGORY_NAVIGATION)
        val navs = entries
            .filter {
                it.category == Notification.CATEGORY_NAVIGATION ||
                    (it.isOngoing && it.packageName in NAVIGATION_PACKAGES)
            }
            .map { entry ->
                IslandActivity.Navigation(
                    id = "nav_${entry.key}",
                    packageName = entry.packageName,
                    title = entry.title,
                    instruction = entry.text.ifBlank { entry.title },
                    etaText = entry.appName,
                    updatedAtMs = entry.postedAtMs
                )
            }
        activityManager.replaceKind(ActivityKind.NAVIGATION, navs)

        // 3. Téléchargements avec progression réelle
        val downloads = entries
            .filter {
                it.isOngoing && (it.progressMax > 0 || it.progressIndeterminate) &&
                    it.category != Notification.CATEGORY_CALL &&
                    it.category != Notification.CATEGORY_TRANSPORT
            }
            .map { entry ->
                val percent = if (entry.progressMax > 0) {
                    ((entry.progress * 100f) / entry.progressMax).toInt().coerceIn(0, 100)
                } else 0
                IslandActivity.Download(
                    id = "dl_${entry.key}",
                    packageName = entry.packageName,
                    title = entry.title,
                    progressPercent = percent,
                    isIndeterminate = entry.progressIndeterminate,
                    updatedAtMs = entry.postedAtMs
                )
            }
        activityManager.replaceKind(ActivityKind.DOWNLOAD, downloads)

        // 4. Notifications standard filtrées et regroupées par application
        val standardEntries = entries.filter { entry ->
            entry.category != Notification.CATEGORY_CALL &&
                entry.category != Notification.CATEGORY_TRANSPORT &&
                entry.category != Notification.CATEGORY_NAVIGATION &&
                !(entry.isOngoing && (entry.progressMax > 0 || entry.progressIndeterminate)) &&
                (allowedPackages.isEmpty() || entry.packageName in allowedPackages)
        }

        val groupedActivities = standardEntries
            .groupBy { it.packageName }
            .mapNotNull { (pkg, pkgEntries) ->
                val latest = pkgEntries.maxByOrNull { it.postedAtMs } ?: return@mapNotNull null
                val displayTitle = if (maskContent) latest.appName else latest.title
                val displayMessage = if (maskContent) {
                    if (pkgEntries.size > 1) "${pkgEntries.size} messages masqués (Confidentialité)"
                    else "Contenu masqué — Mode confidentialité"
                } else {
                    latest.text.ifBlank { "Nouvelle notification" }
                }
                IslandActivity.Notification(
                    id = "notif_$pkg",
                    notificationKey = latest.key,
                    packageName = pkg,
                    appName = latest.appName,
                    title = displayTitle,
                    message = displayMessage,
                    groupedCount = pkgEntries.size,
                    isContentMasked = maskContent,
                    actions = if (maskContent) emptyList() else latest.actions,
                    updatedAtMs = latest.postedAtMs
                )
            }
        activityManager.replaceKind(ActivityKind.NOTIFICATION, groupedActivities)
    }

    private fun resolveAppLabel(context: Context, packageName: String): String {
        PRIORITY_APPS.firstOrNull { it.packageName == packageName }?.let { return it.displayName }
        return runCatching {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrElse {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    companion object {
        val PRIORITY_APPS = listOf(
            PriorityAppInfo("com.whatsapp", "WhatsApp", "Messagerie"),
            PriorityAppInfo("com.whatsapp.w4b", "WhatsApp Business", "Messagerie"),
            PriorityAppInfo("com.google.android.apps.messaging", "Messages", "SMS"),
            PriorityAppInfo("com.android.mms", "SMS Système", "SMS"),
            PriorityAppInfo("com.google.android.gm", "Gmail", "E-mail"),
            PriorityAppInfo("org.telegram.messenger", "Telegram", "Messagerie"),
            PriorityAppInfo("com.google.android.dialer", "Téléphone", "Appels"),
            PriorityAppInfo("com.android.dialer", "Téléphone (Système)", "Appels")
        )

        private val NAVIGATION_PACKAGES = setOf(
            "com.google.android.apps.maps",
            "com.waze"
        )
    }
}
