package com.mike.dynamicisland.faceid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/**
 * Récepteur dynamique des événements d'écran et de verrouillage (Module 01).
 * Obligatoirement enregistré dynamiquement via registerReceiver() sur Android 8+ / Android 10.
 */
class UnlockReceiver(
    private val faceUnlockEngine: FaceUnlockEngine,
    private val isDegradedUnlockEnabled: () -> Boolean = { true }
) : BroadcastReceiver() {

    private var isRegistered: Boolean = false

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_SCREEN_ON -> {
                val locked = faceUnlockEngine.isKeyguardCurrentlyLocked(context)
                faceUnlockEngine.onScreenOn(keyguardLocked = locked)
            }
            Intent.ACTION_SCREEN_OFF -> {
                faceUnlockEngine.onScreenOff()
            }
            Intent.ACTION_USER_PRESENT -> {
                faceUnlockEngine.onUserPresent(
                    allowDegradedUnlockAnimation = isDegradedUnlockEnabled()
                )
            }
        }
    }

    fun register(context: Context) {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(this, filter)
        }
        isRegistered = true
    }

    fun unregister(context: Context) {
        if (!isRegistered) return
        runCatching { context.unregisterReceiver(this) }
        isRegistered = false
    }
}
