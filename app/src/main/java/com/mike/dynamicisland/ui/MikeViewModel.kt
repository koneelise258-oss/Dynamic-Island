package com.mike.dynamicisland.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mike.dynamicisland.MikeApplication
import com.mike.dynamicisland.core.MikeEngine
import com.mike.dynamicisland.core.MikeEventLog
import com.mike.dynamicisland.core.SystemAudioTelemetry
import com.mike.dynamicisland.faceid.FaceAnimationSettings
import com.mike.dynamicisland.faceid.FaceUnlockState
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.IslandUiState
import com.mike.dynamicisland.media.ListeningTelemetryState
import com.mike.dynamicisland.media.MusicGenre
import com.mike.dynamicisland.quickglass.QuickGlassConfig
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassPreset
import com.mike.dynamicisland.settings.AppearanceSettings
import com.mike.dynamicisland.settings.IslandSettings
import com.mike.dynamicisland.settings.MikeAppSettings
import com.mike.dynamicisland.settings.PrivacySettings
import com.mike.dynamicisland.utils.DeviceProfile
import com.mike.dynamicisland.utils.MemorySnapshot
import com.mike.dynamicisland.utils.PerformanceMonitor
import com.mike.dynamicisland.utils.PermissionAudit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MikeViewModel(application: Application) : AndroidViewModel(application) {

    val engine: MikeEngine = MikeApplication.getEngine(application.applicationContext)

    val settings: StateFlow<MikeAppSettings> = engine.settings
    val deviceProfile: StateFlow<DeviceProfile> = engine.deviceProfile
    val permissions: StateFlow<PermissionAudit> = engine.permissions
    val isOverlayServiceRunning: StateFlow<Boolean> = engine.isOverlayServiceRunning
    val faceUnlockState: StateFlow<FaceUnlockState> = engine.faceUnlockEngine.state
    val islandState: StateFlow<IslandUiState> = engine.dynamicIslandEngine.uiState
    val eventLogs: StateFlow<List<MikeEventLog>> = engine.eventManager.events
    val timerState: StateFlow<IslandActivity.Timer?> = engine.activityRegistry.timerEngine.state
    val stopwatchState: StateFlow<IslandActivity.Stopwatch?> = engine.activityRegistry.stopwatchEngine.state
    val alarmState: StateFlow<IslandActivity.Alarm?> = engine.activityRegistry.alarmEngine.state
    val mediaState: StateFlow<IslandActivity.Music?> = engine.mediaSessionController.currentMedia
    val listeningTelemetry: StateFlow<ListeningTelemetryState> = engine.mediaActivityController.listeningTelemetry
    val systemAudioTelemetry: StateFlow<SystemAudioTelemetry> = engine.systemActivityController.telemetry
    val batteryTelemetry = engine.batteryReceiver.telemetry
    val isNotificationListenerConnected = engine.notificationController.isListenerConnected
    val activeNotifications = engine.notificationController.activeEntries

    private val _memorySnapshot = MutableStateFlow(
        PerformanceMonitor.captureSnapshot(application.applicationContext)
    )
    val memorySnapshot: StateFlow<MemorySnapshot> = _memorySnapshot.asStateFlow()

    fun refreshDiagnosticsAndPermissions() {
        engine.refreshPermissions()
        _memorySnapshot.value = PerformanceMonitor.captureSnapshot(getApplication())
    }

    fun toggleOverlayService(enabled: Boolean): Boolean {
        val result = engine.setOverlayEnabled(enabled)
        refreshDiagnosticsAndPermissions()
        return result
    }

    // --- V7 Niveau 3 : Control Center (Audio, Volume, Mémo vocal, Accessoires) ---
    fun selectMusicGenre(genre: MusicGenre) {
        engine.mediaActivityController.selectGenre(genre)
    }

    fun updateMusicSearch(query: String) {
        engine.mediaActivityController.updateSearchQuery(query)
    }

    fun setMediaVolumePercent(percent: Int) {
        engine.systemActivityController.setMediaVolumePercent(percent)
    }

    fun refreshSystemAudioTelemetry() {
        engine.systemActivityController.refreshAllSnapshots()
    }

    fun publishVoiceMemoActivity(isRecording: Boolean) {
        if (isRecording) {
            engine.startOrResumeVoiceMemo(resetDuration = false)
        } else {
            engine.pauseVoiceMemo()
        }
    }

    fun toggleVoiceMemoPauseResume() {
        engine.toggleVoiceMemoPauseResume()
    }

    fun clearVoiceMemoActivity() {
        engine.stopVoiceMemo()
    }

    // --- Face ID ---
    fun updateFaceSettings(transform: (FaceAnimationSettings) -> FaceAnimationSettings) {
        viewModelScope.launch {
            engine.settingsRepository.updateFaceSettings(transform)
        }
    }

    fun triggerFaceIdTest(simulateInterrupt: Boolean = false) {
        engine.faceUnlockEngine.triggerManualTest(simulateInterrupt = simulateInterrupt)
    }

    fun dismissFaceIdAnimation() {
        engine.faceUnlockEngine.markDismissed()
    }

    // --- Dynamic Island ---
    fun updateIslandSettings(transform: (IslandSettings) -> IslandSettings) {
        viewModelScope.launch {
            engine.settingsRepository.updateIslandSettings(transform)
        }
    }

    fun toggleActivityEnabled(kind: ActivityKind, enabled: Boolean) {
        updateIslandSettings { current ->
            val next = if (enabled) current.enabledActivities + kind else current.enabledActivities - kind
            current.copy(enabledActivities = next)
        }
    }

    fun moveActivityPriority(kind: ActivityKind, moveUp: Boolean) {
        updateIslandSettings { current ->
            val list = current.priorityOrder.toMutableList()
            val idx = list.indexOf(kind)
            if (idx == -1) return@updateIslandSettings current
            val targetIdx = if (moveUp) idx - 1 else idx + 1
            if (targetIdx !in list.indices) return@updateIslandSettings current
            list.removeAt(idx)
            list.add(targetIdx, kind)
            current.copy(priorityOrder = list)
        }
    }

    fun toggleAllowedNotificationPackage(packageName: String, allowed: Boolean) {
        updateIslandSettings { current ->
            val next = if (allowed) {
                current.allowedNotificationPackages + packageName
            } else {
                current.allowedNotificationPackages - packageName
            }
            current.copy(allowedNotificationPackages = next)
        }
    }

    // --- Quick Glass ---
    fun updateQuickGlass(transform: (QuickGlassConfig) -> QuickGlassConfig) {
        viewModelScope.launch {
            engine.settingsRepository.updateQuickGlass(transform)
        }
    }

    fun applyQuickGlassPreset(preset: QuickGlassPreset) {
        val defaults = QuickGlassEngine.presetDefaults(preset)
        updateQuickGlass { current ->
            defaults.copy(optionalHardwareBlurEnabled = current.optionalHardwareBlurEnabled)
        }
    }

    // --- Privacy & Appearance ---
    fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings) {
        viewModelScope.launch {
            engine.settingsRepository.updatePrivacy(transform)
        }
    }

    fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch {
            engine.settingsRepository.updateAppearance(transform)
        }
    }

    fun resetAllDefaults() {
        viewModelScope.launch {
            engine.settingsRepository.updateFaceSettings { FaceAnimationSettings() }
            engine.settingsRepository.updateIslandSettings { IslandSettings() }
            engine.settingsRepository.updateQuickGlass { QuickGlassConfig() }
            engine.settingsRepository.updatePrivacy { PrivacySettings() }
            engine.settingsRepository.updateAppearance { AppearanceSettings() }
            engine.eventManager.record("Réglages", "Paramètres réinitialisés aux valeurs par défaut", true)
        }
    }

    // --- Now Bar Activities ---
    fun startTimer(seconds: Long, label: String = "Minuteur") {
        engine.activityRegistry.timerEngine.start(seconds, label)
    }

    fun startOrToggleStopwatch() {
        engine.activityRegistry.stopwatchEngine.toggle()
    }

    fun lapStopwatch() {
        engine.activityRegistry.stopwatchEngine.recordLap()
    }

    fun resetStopwatch() {
        engine.activityRegistry.stopwatchEngine.reset()
    }

    fun scheduleAlarm(seconds: Long, label: String = "Alarme Mike") {
        engine.activityRegistry.alarmEngine.scheduleInSeconds(seconds, label)
    }

    fun triggerAlarmTest() {
        engine.activityRegistry.alarmEngine.triggerRingingNow("Alarme test Mike")
    }

    fun clearLogs() {
        engine.eventManager.clear()
    }
}
