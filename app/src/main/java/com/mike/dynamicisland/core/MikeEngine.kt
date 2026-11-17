package com.mike.dynamicisland.core

import android.app.Activity
import android.content.Context
import com.mike.dynamicisland.activities.ActivityRegistry
import com.mike.dynamicisland.battery.BatteryEventReceiver
import com.mike.dynamicisland.faceid.FaceUnlockEngine
import com.mike.dynamicisland.faceid.FaceUnlockPhase
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.DynamicIslandController
import com.mike.dynamicisland.island.DynamicIslandEngine
import com.mike.dynamicisland.island.DynamicIslandStateManager
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.IslandInteractionCallbacks
import com.mike.dynamicisland.island.IslandOverlayService
import com.mike.dynamicisland.media.MediaActivityController
import com.mike.dynamicisland.media.MediaSessionController
import com.mike.dynamicisland.notifications.NotificationActivityController
import com.mike.dynamicisland.notifications.NotificationController
import com.mike.dynamicisland.settings.IslandSettings
import com.mike.dynamicisland.settings.MikeAppSettings
import com.mike.dynamicisland.settings.SettingsRepository
import com.mike.dynamicisland.utils.DeviceProfile
import com.mike.dynamicisland.utils.DeviceProfileDetector
import com.mike.dynamicisland.utils.DisplayCutoutManager
import com.mike.dynamicisland.utils.PermissionAudit
import com.mike.dynamicisland.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Orchestrateur principal MIKE Dynamic Island V7 (Module 10 & V7 Partie 4).
 * Relie le profil matériel, DisplayCutoutManager, FaceUnlockEngine, SystemActivityController,
 * MediaActivityController, NotificationActivityController, ActivityPriorityManager et DynamicIslandEngine.
 */
class MikeEngine(private val appContext: Context) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val eventManager = EventManager()
    val activityManager = MikeActivityManager()
    val priorityManager = PriorityManager()
    val activityPriorityManager = ActivityPriorityManager(priorityManager)
    val settingsRepository = SettingsRepository(appContext)

    private val _deviceProfile = MutableStateFlow(DeviceProfileDetector.detect(appContext))
    val deviceProfile: StateFlow<DeviceProfile> = _deviceProfile.asStateFlow()

    private val _permissions = MutableStateFlow(PermissionManager.audit(appContext))
    val permissions: StateFlow<PermissionAudit> = _permissions.asStateFlow()

    private val _isOverlayServiceRunning = MutableStateFlow(false)
    val isOverlayServiceRunning: StateFlow<Boolean> = _isOverlayServiceRunning.asStateFlow()

    val settings: StateFlow<MikeAppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = MikeAppSettings()
    )

    private val islandSettingsFlow: StateFlow<IslandSettings> = settings
        .map { it.island }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = IslandSettings()
        )

    val faceUnlockEngine = FaceUnlockEngine(
        onEventLogged = { category, message, isReal ->
            eventManager.record(category, message, isReal)
        }
    )

    val activityRegistry = ActivityRegistry(
        scope = scope,
        activityManager = activityManager,
        eventManager = eventManager
    )

    val notificationController = NotificationController(
        activityManager = activityManager,
        eventManager = eventManager
    )

    val notificationActivityController = NotificationActivityController(
        baseController = notificationController,
        activityManager = activityManager,
        eventManager = eventManager
    )

    val mediaSessionController = MediaSessionController(
        activityManager = activityManager,
        eventManager = eventManager,
        deviceProfileProvider = { _deviceProfile.value }
    )

    val mediaActivityController = MediaActivityController(
        scope = scope,
        sessionController = mediaSessionController
    )

    val systemActivityController = SystemActivityController(
        context = appContext,
        scope = scope,
        activityManager = activityManager,
        eventManager = eventManager
    )

    val batteryReceiver = BatteryEventReceiver(
        scope = scope,
        activityManager = activityManager,
        eventManager = eventManager
    )

    val dynamicIslandEngine = DynamicIslandEngine(
        scope = scope,
        activityManager = activityManager,
        priorityManager = priorityManager,
        settingsFlow = islandSettingsFlow,
        eventManager = eventManager
    )

    val dynamicIslandStateManager = DynamicIslandStateManager(
        engine = dynamicIslandEngine,
        deviceProfile = deviceProfile
    )

    val dynamicIslandController = DynamicIslandController(
        engine = dynamicIslandEngine
    )

    private var voiceMemoJob: Job? = null
    private var voiceMemoElapsedSec: Long = 0L

    val islandCallbacks = IslandInteractionCallbacks(
        onToggleExpand = { dynamicIslandController.toggleLevel() },
        onCollapse = { dynamicIslandController.collapseToMiniIsland() },
        onSwapSecondary = { dynamicIslandController.swapActivities() },
        onTimerTogglePause = { activityRegistry.timerEngine.togglePauseResume() },
        onTimerAddMinute = { activityRegistry.timerEngine.addOneMinute() },
        onTimerCancel = { activityRegistry.timerEngine.cancel() },
        onStopwatchToggle = { activityRegistry.stopwatchEngine.toggle() },
        onStopwatchLap = { activityRegistry.stopwatchEngine.recordLap() },
        onStopwatchReset = { activityRegistry.stopwatchEngine.reset() },
        onAlarmSnooze = { activityRegistry.alarmEngine.snooze(5) },
        onAlarmDismiss = { activityRegistry.alarmEngine.dismiss() },
        onMusicTogglePlayPause = { mediaActivityController.togglePlayPause() },
        onMusicPrevious = { mediaActivityController.skipToPrevious() },
        onMusicNext = { mediaActivityController.skipToNext() },
        onVoiceMemoTogglePause = { toggleVoiceMemoPauseResume() },
        onVoiceMemoStop = { stopVoiceMemo() },
        onNotificationOpen = { key -> notificationActivityController.openNotification(key) },
        onNotificationDismiss = { key -> notificationActivityController.dismissNotification(key) },
        onNotificationAction = { key, idx -> notificationActivityController.triggerAction(key, idx) }
    )

    init {
        faceUnlockEngine.inspectCapabilities(appContext)
        val profile = _deviceProfile.value
        eventManager.record(
            "Matériel",
            "API ${profile.androidVersion} · ${profile.totalRamMb} Mo RAM (${profile.availableRamMb} Mo dispo) · ${profile.screenWidth}x${profile.screenHeight} @ ${profile.refreshRate.toInt()} Hz",
            true
        )

        scope.launch {
            combine(settings, faceUnlockEngine.state) { appSettings, faceState ->
                Triple(
                    appSettings.island.allowedNotificationPackages,
                    appSettings.privacy,
                    faceState.isKeyguardLocked
                )
            }.collect { (allowed, privacy, locked) ->
                notificationActivityController.updateFilters(
                    allowedPackages = allowed,
                    privacySettings = privacy,
                    deviceLocked = locked
                )
            }
        }

        // Synchronise l'indicateur compact Face ID (Niveau 1 Mini Island) lors de la validation
        scope.launch {
            faceUnlockEngine.state.collect { faceState ->
                if (faceState.phase == FaceUnlockPhase.SUCCESS && faceState.isScreenOn) {
                    activityManager.upsert(
                        IslandActivity.FaceIdIndicator(
                            isVerified = true,
                            statusLabel = "Déverrouillé"
                        )
                    )
                    delay(1400L)
                    activityManager.removeByKind(ActivityKind.FACE_ID)
                } else if (!faceState.isScreenOn) {
                    activityManager.removeByKind(ActivityKind.FACE_ID)
                }
            }
        }
    }

    fun refreshPermissions() {
        val audit = PermissionManager.audit(appContext)
        _permissions.value = audit
        systemActivityController.refreshAllSnapshots()
        if (audit.notificationListenerGranted) {
            mediaActivityController.start(appContext)
        }
    }

    fun refineCutoutFromActivity(activity: Activity) {
        val updated = DisplayCutoutManager.updateProfileFromActivity(activity, _deviceProfile.value)
        if (updated != _deviceProfile.value) {
            _deviceProfile.value = updated
            updated.displayCutoutBounds?.let { bounds ->
                val zone = DisplayCutoutManager.detectZone(updated)
                eventManager.record(
                    "Caméra",
                    "DisplayCutout ${zone.label} : (${bounds.leftPx}, ${bounds.topPx}) ${bounds.widthPx}x${bounds.heightPx} px",
                    true
                )
            }
        }
    }

    fun setOverlayServiceRunning(running: Boolean) {
        _isOverlayServiceRunning.value = running
        eventManager.record(
            "Service",
            if (running) "IslandOverlayService actif (fenêtre compacte)" else "IslandOverlayService arrêté",
            true
        )
    }

    fun startOrResumeVoiceMemo(resetDuration: Boolean = false) {
        voiceMemoJob?.cancel()
        if (resetDuration) {
            voiceMemoElapsedSec = 0L
        }
        val wasNew = voiceMemoElapsedSec == 0L
        eventManager.record(
            "Mémo vocal",
            if (wasNew) "Mémo vocal démarré (Mini / Smart Island)" else "Mémo vocal repris à ${voiceMemoElapsedSec}s",
            true
        )
        activityManager.upsert(
            IslandActivity.VoiceMemo(
                id = VOICE_MEMO_ID,
                label = "Mémo vocal",
                durationSeconds = voiceMemoElapsedSec,
                isRecording = true
            )
        )
        voiceMemoJob = scope.launch {
            while (true) {
                delay(1000L)
                voiceMemoElapsedSec += 1L
                activityManager.upsert(
                    IslandActivity.VoiceMemo(
                        id = VOICE_MEMO_ID,
                        label = "Mémo vocal",
                        durationSeconds = voiceMemoElapsedSec,
                        isRecording = true
                    )
                )
            }
        }
    }

    fun pauseVoiceMemo() {
        voiceMemoJob?.cancel()
        voiceMemoJob = null
        val current = activityManager.activities.value[VOICE_MEMO_ID] as? IslandActivity.VoiceMemo
        val elapsed = current?.durationSeconds ?: voiceMemoElapsedSec
        voiceMemoElapsedSec = elapsed
        activityManager.upsert(
            IslandActivity.VoiceMemo(
                id = VOICE_MEMO_ID,
                label = "Mémo vocal",
                durationSeconds = elapsed,
                isRecording = false
            )
        )
        eventManager.record("Mémo vocal", "Mémo vocal en pause (${elapsed}s)", true)
    }

    fun toggleVoiceMemoPauseResume() {
        val current = activityManager.activities.value[VOICE_MEMO_ID] as? IslandActivity.VoiceMemo
        if (current != null && current.isRecording) {
            pauseVoiceMemo()
        } else {
            startOrResumeVoiceMemo(resetDuration = current == null)
        }
    }

    fun stopVoiceMemo() {
        voiceMemoJob?.cancel()
        voiceMemoJob = null
        voiceMemoElapsedSec = 0L
        activityManager.removeByKind(ActivityKind.VOICE_MEMO)
        eventManager.record("Mémo vocal", "Mémo vocal arrêté", true)
    }

    fun setOverlayEnabled(enabled: Boolean): Boolean {
        scope.launch {
            settingsRepository.updateIslandSettings { it.copy(overlayServiceEnabled = enabled) }
        }
        return if (enabled) {
            IslandOverlayService.startIfPermitted(appContext)
        } else {
            IslandOverlayService.stop(appContext)
            true
        }
    }

    companion object {
        const val VOICE_MEMO_ID = "mike_voice_memo"
    }
}
