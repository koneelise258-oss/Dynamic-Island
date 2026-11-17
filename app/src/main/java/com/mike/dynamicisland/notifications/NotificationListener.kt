package com.mike.dynamicisland.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mike.dynamicisland.MikeApplication

/**
 * Service Android officiel d'écoute locale des notifications (Module 06 & V7)
 * et point d'ancrage pour MediaSessionManager (Module 07).
 */
class MikeNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        val engine = MikeApplication.getEngine(applicationContext)
        engine.notificationActivityController.attachListener(
            connected = true,
            onCancelNotification = { key ->
                runCatching { cancelNotification(key) }
            }
        )
        runCatching { activeNotifications }
            .getOrNull()
            ?.forEach { sbn ->
                engine.notificationActivityController.onNotificationPosted(applicationContext, sbn)
            }
        engine.mediaActivityController.start(applicationContext)
    }

    override fun onListenerDisconnected() {
        val engine = MikeApplication.getEngine(applicationContext)
        engine.notificationActivityController.attachListener(connected = false)
        engine.mediaActivityController.stop()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        val engine = MikeApplication.getEngine(applicationContext)
        engine.notificationActivityController.onNotificationPosted(applicationContext, posted)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val removed = sbn ?: return
        val engine = MikeApplication.getEngine(applicationContext)
        engine.notificationActivityController.onNotificationRemoved(removed.key)
    }
}
