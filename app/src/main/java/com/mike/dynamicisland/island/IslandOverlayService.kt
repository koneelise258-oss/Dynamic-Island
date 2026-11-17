package com.mike.dynamicisland.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.mike.dynamicisland.MikeApplication
import com.mike.dynamicisland.core.MikeEngine
import com.mike.dynamicisland.faceid.FaceUnlockPhase
import com.mike.dynamicisland.faceid.UnlockReceiver
import com.mike.dynamicisland.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Service de premier plan MIKE Dynamic Island V7.
 * Héberge DynamicIslandWindow (fenêtre strictement compacte en haut d'écran),
 * UnlockReceiver, BatteryEventReceiver et SystemActivityController.
 */
class IslandOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: MikeEngine
    private lateinit var dynamicIslandWindow: DynamicIslandWindow
    private lateinit var unlockReceiver: UnlockReceiver

    override fun onCreate() {
        super.onCreate()
        engine = MikeApplication.getEngine(applicationContext)
        dynamicIslandWindow = DynamicIslandWindow(
            context = applicationContext,
            engine = engine,
            onFaceSuccessHaptic = { performLightHaptic() }
        )
        unlockReceiver = UnlockReceiver(
            faceUnlockEngine = engine.faceUnlockEngine,
            isDegradedUnlockEnabled = { engine.settings.value.faceId.triggerOnDegradedKeyguardUnlock }
        )

        createNotificationChannel()
        startForegroundSafe()

        unlockReceiver.register(applicationContext)
        engine.batteryReceiver.register(applicationContext)
        engine.systemActivityController.startObserving()
        engine.mediaActivityController.start(applicationContext)
        engine.setOverlayServiceRunning(true)

        observeOverlayState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        dynamicIslandWindow.detachAll()
        unlockReceiver.unregister(applicationContext)
        engine.batteryReceiver.unregister(applicationContext)
        engine.systemActivityController.stopObserving()
        engine.setOverlayServiceRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeOverlayState() {
        serviceScope.launch {
            combine(
                engine.faceUnlockEngine.state,
                engine.dynamicIslandEngine.uiState,
                engine.settings
            ) { faceState, islandState, settings ->
                val showFace = settings.faceId.enabled &&
                    faceState.isScreenOn &&
                    faceState.phase != FaceUnlockPhase.IDLE
                val showIsland = faceState.isScreenOn && islandState.isVisible
                val expanded = islandState.displayMode == IslandDisplayMode.EXPANDED
                Triple(showFace, showIsland, expanded)
            }.collect { (showFace, showIsland, expanded) ->
                dynamicIslandWindow.syncOverlayVisibility(
                    shouldShowFaceId = showFace,
                    shouldShowIsland = showIsland,
                    isIslandExpanded = expanded
                )
            }
        }
    }

    private fun startForegroundSafe() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fgs_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, IslandOverlayService::class.java).setAction(ACTION_STOP_SERVICE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.fgs_title))
            .setContentText(getString(R.string.fgs_subtitle))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.fgs_action_stop), stopIntent)
            .build()
    }

    private fun performLightHaptic() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(24L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(24L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(24L)
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "mike_dynamic_island_engine"
        private const val NOTIFICATION_ID = 4001
        const val ACTION_STOP_SERVICE = "com.mike.dynamicisland.action.STOP_OVERLAY"

        fun startIfPermitted(context: Context): Boolean {
            if (!PermissionManager.hasOverlayPermission(context)) return false
            val intent = Intent(context, IslandOverlayService::class.java)
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, IslandOverlayService::class.java))
            }
        }
    }
}
