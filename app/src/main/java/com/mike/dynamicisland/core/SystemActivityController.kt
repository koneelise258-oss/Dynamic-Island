package com.mike.dynamicisland.core

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.RingerStateType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectedAudioAccessory(
    val name: String,
    val isBluetooth: Boolean,
    val deviceTypeLabel: String,
    val batteryPercent: Int? = null
)

data class SystemAudioTelemetry(
    val mediaVolumePercent: Int = 50,
    val ringerState: RingerStateType = RingerStateType.NORMAL,
    val isDoNotDisturbOn: Boolean = false,
    val connectedAccessory: ConnectedAudioAccessory? = null
)

/**
 * Contrôleur des états système natifs Android (PARTIE 4 & JSON Source #2) :
 * - Mode Silencieux / Vibreur (AudioManager.RINGER_MODE_CHANGED_ACTION)
 * - Ne pas déranger (NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
 * - Accessoires audio Bluetooth / filaires (AudioDeviceCallback — sans permission invasive)
 * - Volume média réel (AudioManager.STREAM_MUSIC) pour le widget Volume Radial du Control Center
 */
class SystemActivityController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val activityManager: MikeActivityManager,
    private val eventManager: EventManager,
    private val transientBannerMs: Long = 3500L
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _telemetry = MutableStateFlow(SystemAudioTelemetry())
    val telemetry: StateFlow<SystemAudioTelemetry> = _telemetry.asStateFlow()

    private var isRegistered = false
    private var silentAutoHideJob: Job? = null
    private var dndAutoHideJob: Job? = null
    private var accessoryAutoHideJob: Job? = null

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            val accessory = addedDevices?.firstOrNull { isExternalHeadset(it) }?.toConnectedAccessory()
            refreshAccessorySnapshot()
            if (accessory != null) {
                publishAccessoryBanner(accessory)
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAccessorySnapshot()
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val state = readCurrentRingerState()
                    _telemetry.value = _telemetry.value.copy(ringerState = state)
                    publishSilentModeBanner(state)
                }
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                    val dndOn = readDoNotDisturbActive()
                    _telemetry.value = _telemetry.value.copy(isDoNotDisturbOn = dndOn)
                    publishDndBanner(dndOn)
                }
            }
        }
    }

    init {
        refreshAllSnapshots()
    }

    fun startObserving() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(systemReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(systemReceiver, filter)
            }
            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
            isRegistered = true
            refreshAllSnapshots()
        }
    }

    fun stopObserving() {
        if (!isRegistered) return
        silentAutoHideJob?.cancel()
        dndAutoHideJob?.cancel()
        accessoryAutoHideJob?.cancel()
        runCatching { context.unregisterReceiver(systemReceiver) }
        runCatching { audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback) }
        isRegistered = false
    }

    fun refreshAllSnapshots() {
        val vol = readMediaVolumePercent()
        val ringer = readCurrentRingerState()
        val dnd = readDoNotDisturbActive()
        val acc = findConnectedAccessory()
        _telemetry.value = SystemAudioTelemetry(
            mediaVolumePercent = vol,
            ringerState = ringer,
            isDoNotDisturbOn = dnd,
            connectedAccessory = acc
        )
    }

    /**
     * Règle réellement le volume média Android (STREAM_MUSIC) depuis le widget Volume Radial.
     */
    fun setMediaVolumePercent(percent: Int) {
        val am = audioManager ?: return
        val clamped = percent.coerceIn(0, 100)
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val targetIndex = ((clamped / 100f) * maxVol).toInt().coerceIn(0, maxVol)
        runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
            _telemetry.value = _telemetry.value.copy(mediaVolumePercent = readMediaVolumePercent())
        }
    }

    fun publishSilentModeBanner(state: RingerStateType) {
        activityManager.upsert(IslandActivity.SilentMode(ringerState = state))
        eventManager.record("Système", "Mode audio : ${state.label}", true)
        silentAutoHideJob?.cancel()
        silentAutoHideJob = scope.launch {
            delay(transientBannerMs)
            activityManager.removeByKind(ActivityKind.SILENT_MODE)
        }
    }

    fun publishDndBanner(isActive: Boolean) {
        activityManager.upsert(IslandActivity.DoNotDisturb(isActive = isActive))
        eventManager.record(
            "Système",
            if (isActive) "Ne pas déranger activé" else "Ne pas déranger désactivé",
            true
        )
        dndAutoHideJob?.cancel()
        dndAutoHideJob = scope.launch {
            delay(transientBannerMs)
            activityManager.removeByKind(ActivityKind.DO_NOT_DISTURB)
        }
    }

    fun publishAccessoryBanner(accessory: ConnectedAudioAccessory) {
        activityManager.upsert(
            IslandActivity.AccessoryConnected(
                deviceName = accessory.name,
                isBluetooth = accessory.isBluetooth,
                batteryPercent = accessory.batteryPercent
            )
        )
        eventManager.record("Accessoire", "Connecté : ${accessory.name} (${accessory.deviceTypeLabel})", true)
        accessoryAutoHideJob?.cancel()
        accessoryAutoHideJob = scope.launch {
            delay(transientBannerMs + 1000L)
            activityManager.removeByKind(ActivityKind.ACCESSORY)
        }
    }

    private fun refreshAccessorySnapshot() {
        _telemetry.value = _telemetry.value.copy(connectedAccessory = findConnectedAccessory())
    }

    private fun readMediaVolumePercent(): Int {
        val am = audioManager ?: return 50
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maxVol)
        return ((current * 100f) / maxVol.toFloat()).toInt().coerceIn(0, 100)
    }

    private fun readCurrentRingerState(): RingerStateType {
        return when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerStateType.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerStateType.VIBRATE
            else -> RingerStateType.NORMAL
        }
    }

    private fun readDoNotDisturbActive(): Boolean {
        val filter = runCatching { notificationManager?.currentInterruptionFilter }
            .getOrDefault(NotificationManager.INTERRUPTION_FILTER_ALL)
        return filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
            filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    private fun findConnectedAccessory(): ConnectedAudioAccessory? {
        val devices = runCatching { audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }
            .getOrNull() ?: return null
        return devices.firstOrNull { isExternalHeadset(it) }?.toConnectedAccessory()
    }

    private fun isExternalHeadset(info: AudioDeviceInfo): Boolean {
        return info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            info.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

    private fun AudioDeviceInfo.toConnectedAccessory(): ConnectedAudioAccessory {
        val isBt = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        val typeLabel = when {
            isBt -> "Bluetooth Audio"
            type == AudioDeviceInfo.TYPE_USB_HEADSET -> "Casque USB"
            else -> "Casque filaire"
        }
        val cleanName = productName?.toString()?.trim().orEmpty()
            .ifBlank { if (isBt) "Écouteurs Bluetooth" else "Casque audio" }
        // Android public API (API 29) ne fournit pas les niveaux gauche/droite propriétaires AirPods :
        // batteryPercent reste null tant qu'une source système fiable ne le fournit pas.
        return ConnectedAudioAccessory(
            name = cleanName,
            isBluetooth = isBt,
            deviceTypeLabel = typeLabel,
            batteryPercent = null
        )
    }
}
