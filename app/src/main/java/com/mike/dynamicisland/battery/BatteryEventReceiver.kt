package com.mike.dynamicisland.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.BatteryEventType
import com.mike.dynamicisland.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BatteryTelemetry(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val isLow: Boolean = false,
    val lastEvent: BatteryEventType? = null
)

/**
 * Récepteur d'événements batterie Android (Module 08).
 * Évite toute mise à jour superflue en filtrant les valeurs inchangées
 * et masque automatiquement l'animation temporaire après quelques secondes.
 */
class BatteryEventReceiver(
    private val scope: CoroutineScope,
    private val activityManager: MikeActivityManager,
    private val eventManager: EventManager,
    private val bannerDurationMs: Long = 4500L
) : BroadcastReceiver() {

    private val _telemetry = MutableStateFlow(BatteryTelemetry())
    val telemetry: StateFlow<BatteryTelemetry> = _telemetry.asStateFlow()

    private var isRegistered = false
    private var autoHideJob: Job? = null
    private var lastReportedFull = false
    private var lastReportedLow = false

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val current = _telemetry.value.copy(isCharging = true, lastEvent = BatteryEventType.CHARGER_CONNECTED)
                _telemetry.value = current
                publishBatteryBanner(BatteryEventType.CHARGER_CONNECTED, current.levelPercent, isCharging = true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val current = _telemetry.value.copy(
                    isCharging = false,
                    isFull = false,
                    lastEvent = BatteryEventType.CHARGER_DISCONNECTED
                )
                lastReportedFull = false
                _telemetry.value = current
                publishBatteryBanner(BatteryEventType.CHARGER_DISCONNECTED, current.levelPercent, isCharging = false)
            }
            Intent.ACTION_BATTERY_LOW -> {
                val current = _telemetry.value.copy(isLow = true, lastEvent = BatteryEventType.BATTERY_LOW)
                lastReportedLow = true
                _telemetry.value = current
                publishBatteryBanner(BatteryEventType.BATTERY_LOW, current.levelPercent, current.isCharging)
            }
            Intent.ACTION_BATTERY_OKAY -> {
                lastReportedLow = false
                _telemetry.value = _telemetry.value.copy(isLow = false)
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                handleBatteryChanged(intent)
            }
        }
    }

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        if (level < 0) return

        val percent = ((level * 100f) / scale.toFloat()).toInt().coerceIn(0, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || (charging && percent >= 100)
        val isLow = !charging && percent <= 15

        val prev = _telemetry.value
        if (prev.levelPercent == percent && prev.isCharging == charging && prev.isFull == isFull && prev.isLow == isLow) {
            return // Aucune actualisation inutile
        }

        _telemetry.value = prev.copy(
            levelPercent = percent,
            isCharging = charging,
            isFull = isFull,
            isLow = isLow
        )

        if (isFull && !lastReportedFull) {
            lastReportedFull = true
            publishBatteryBanner(BatteryEventType.BATTERY_FULL, percent, isCharging = true)
        } else if (!isFull) {
            lastReportedFull = false
        }

        if (isLow && !lastReportedLow) {
            lastReportedLow = true
            publishBatteryBanner(BatteryEventType.BATTERY_LOW, percent, isCharging = false)
        } else if (!isLow) {
            lastReportedLow = false
        }
    }

    private fun publishBatteryBanner(type: BatteryEventType, levelPercent: Int, isCharging: Boolean) {
        val activity = IslandActivity.Battery(
            eventType = type,
            levelPercent = levelPercent,
            isCharging = isCharging
        )
        activityManager.upsert(activity)
        eventManager.record("Batterie", "${type.label} ($levelPercent%)", true)

        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(bannerDurationMs)
            activityManager.removeByKind(ActivityKind.BATTERY)
        }
    }

    fun register(context: Context) {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        val stickyIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(this, filter)
        }
        stickyIntent?.let { handleBatteryChanged(it) }
        isRegistered = true
    }

    fun unregister(context: Context) {
        if (!isRegistered) return
        autoHideJob?.cancel()
        runCatching { context.unregisterReceiver(this) }
        isRegistered = false
    }
}
