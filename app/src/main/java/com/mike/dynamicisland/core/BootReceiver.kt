package com.mike.dynamicisland.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mike.dynamicisland.MikeApplication
import com.mike.dynamicisland.island.IslandOverlayService
import com.mike.dynamicisland.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Redémarre proprement le service overlay après redémarrage uniquement si l'utilisateur
 * a activé l'option et accordé la permission de superposition.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val engine = MikeApplication.getEngine(context.applicationContext)
                val settings = engine.settingsRepository.settingsFlow.first()
                if (settings.island.autoStartOnBoot &&
                    settings.island.overlayServiceEnabled &&
                    PermissionManager.hasOverlayPermission(context.applicationContext)
                ) {
                    IslandOverlayService.startIfPermitted(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
