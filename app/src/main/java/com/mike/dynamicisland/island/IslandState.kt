package com.mike.dynamicisland.island

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MikeColors

/**
 * Niveau d'affichage de l'îlot (PARTIE 5) :
 * - MINI_ISLAND : Niveau 1 (indicateur compact discret)
 * - SMART_ISLAND : Niveau 2 (carte contextuelle compacte en haut de l'écran)
 */
enum class IslandLevel {
    MINI_ISLAND,
    SMART_ISLAND
}

enum class ActivityKind(
    val title: String,
    val defaultPriority: Int,
    val accentColor: Color,
    val preferredLevel: IslandLevel = IslandLevel.MINI_ISLAND
) {
    CALL("Appels", 100, MikeColors.Green, IslandLevel.SMART_ISLAND),
    ALARM("Alarme", 95, MikeColors.Coral, IslandLevel.SMART_ISLAND),
    FACE_ID("Face ID", 92, MikeColors.Green, IslandLevel.MINI_ISLAND),
    TIMER("Minuteur", 85, MikeColors.Amber, IslandLevel.SMART_ISLAND),
    NAVIGATION("Navigation", 80, MikeColors.Blue, IslandLevel.SMART_ISLAND),
    MUSIC("Musique", 75, MikeColors.Purple, IslandLevel.SMART_ISLAND),
    STOPWATCH("Chronomètre", 70, MikeColors.Blue, IslandLevel.SMART_ISLAND),
    SILENT_MODE("Mode Silencieux", 68, MikeColors.Amber, IslandLevel.MINI_ISLAND),
    DO_NOT_DISTURB("Ne pas déranger", 66, MikeColors.Purple, IslandLevel.MINI_ISLAND),
    ACCESSORY("Accessoires audio", 64, MikeColors.Green, IslandLevel.MINI_ISLAND),
    NOTIFICATION("Notifications", 60, MikeColors.Cyan, IslandLevel.MINI_ISLAND),
    VOICE_MEMO("Mémo vocal", 58, MikeColors.Coral, IslandLevel.MINI_ISLAND),
    DOWNLOAD("Transferts & Téléchargements", 55, MikeColors.Blue, IslandLevel.MINI_ISLAND),
    HOME_EVENT("Domotique", 52, MikeColors.Green, IslandLevel.MINI_ISLAND),
    BATTERY("Batterie", 50, MikeColors.Green, IslandLevel.MINI_ISLAND)
}

enum class IslandDisplayMode {
    HIDDEN,
    COMPACT,
    EXPANDED
}

enum class BatteryEventType(val label: String) {
    CHARGER_CONNECTED("Chargeur branché"),
    CHARGER_DISCONNECTED("Chargeur débranché"),
    BATTERY_LOW("Batterie faible"),
    BATTERY_FULL("Batterie pleine")
}

enum class RingerStateType(val label: String, val badge: String) {
    SILENT("Silencieux", "Silent"),
    VIBRATE("Vibreur", "Vibreur"),
    NORMAL("Sonnerie", "Sonnerie")
}

data class NotificationActionItem(
    val index: Int,
    val title: String
)

sealed interface IslandActivity {
    val id: String
    val kind: ActivityKind
    val title: String
    val compactText: String
    val updatedAtMs: Long

    data class Timer(
        override val id: String = "timer_primary",
        override val title: String = "Minuteur",
        val remainingSeconds: Long,
        val totalSeconds: Long,
        val isPaused: Boolean,
        val isFinished: Boolean = false,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.TIMER
        override val compactText: String
            get() = if (isFinished) "Terminé" else formatDuration(remainingSeconds)
        val progress: Float
            get() = if (totalSeconds > 0L) {
                (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
            } else 0f
    }

    data class Stopwatch(
        override val id: String = "stopwatch_primary",
        override val title: String = "Chronomètre",
        val elapsedMillis: Long,
        val isRunning: Boolean,
        val laps: List<Long> = emptyList(),
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.STOPWATCH
        override val compactText: String
            get() = formatDuration(elapsedMillis / 1000L)
        val detailedText: String
            get() {
                val totalSec = elapsedMillis / 1000L
                val tenths = (elapsedMillis % 1000L) / 100L
                return "${formatDuration(totalSec)}.$tenths"
            }
    }

    data class Alarm(
        override val id: String = "alarm_primary",
        override val title: String = "Alarme",
        val label: String,
        val isRinging: Boolean,
        val remainingSeconds: Long,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.ALARM
        override val compactText: String
            get() = if (isRinging) "Sonnerie" else formatDuration(remainingSeconds)
    }

    data class Music(
        override val id: String = "music_session",
        val packageName: String,
        override val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val artwork: Bitmap? = null,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.MUSIC
        override val compactText: String
            get() = title.ifBlank { "Média" }
        val progress: Float
            get() = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
    }

    data class Call(
        override val id: String,
        val callerName: String,
        val statusText: String,
        val startedAtElapsedMs: Long,
        val packageName: String,
        val isIncoming: Boolean = false,
        val isVideoCall: Boolean = false,
        val isEnded: Boolean = false,
        val notificationKey: String? = null,
        val actions: List<NotificationActionItem> = emptyList(),
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.CALL
        override val title: String = callerName.ifBlank {
            when {
                isEnded -> "Appel terminé"
                isIncoming -> "Appel entrant"
                else -> "Appel en cours"
            }
        }
        override val compactText: String = statusText.ifBlank {
            when {
                isEnded -> "Terminé"
                isIncoming -> "Entrant"
                else -> "Appel"
            }
        }
    }

    data class Notification(
        override val id: String,
        val notificationKey: String,
        val packageName: String,
        val appName: String,
        override val title: String,
        val message: String,
        val groupedCount: Int = 1,
        val isContentMasked: Boolean = false,
        val actions: List<NotificationActionItem> = emptyList(),
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.NOTIFICATION
        override val compactText: String
            get() = if (groupedCount > 1) "$appName ($groupedCount)" else appName
    }

    data class Battery(
        override val id: String = "battery_event",
        val eventType: BatteryEventType,
        val levelPercent: Int,
        val isCharging: Boolean,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.BATTERY
        override val title: String = eventType.label
        override val compactText: String = "$levelPercent%"
    }

    data class Navigation(
        override val id: String,
        val packageName: String,
        override val title: String,
        val instruction: String,
        val etaText: String,
        val distanceText: String = "",
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.NAVIGATION
        override val compactText: String = distanceText.ifBlank { etaText.ifBlank { instruction.take(14) } }
    }

    data class Download(
        override val id: String,
        val packageName: String,
        override val title: String,
        val progressPercent: Int,
        val isIndeterminate: Boolean,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.DOWNLOAD
        override val compactText: String = if (isIndeterminate) "Transfert" else "$progressPercent%"
    }

    // Nouveaux états système réels du Design System JSON V7 (Source #2)
    data class SilentMode(
        override val id: String = "system_silent_mode",
        val ringerState: RingerStateType,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.SILENT_MODE
        override val title: String = ringerState.label
        override val compactText: String = ringerState.badge
    }

    data class DoNotDisturb(
        override val id: String = "system_dnd",
        val isActive: Boolean,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.DO_NOT_DISTURB
        override val title: String = "Ne pas déranger"
        override val compactText: String = if (isActive) "Activé" else "Désactivé"
    }

    data class AccessoryConnected(
        override val id: String = "system_accessory",
        val deviceName: String,
        val isBluetooth: Boolean,
        val batteryPercent: Int? = null,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.ACCESSORY
        override val title: String = deviceName
        override val compactText: String = batteryPercent?.let { "$it%" } ?: "Connecté"
    }

    data class FaceIdIndicator(
        override val id: String = "face_id_indicator",
        val isVerified: Boolean,
        val statusLabel: String,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.FACE_ID
        override val title: String = "Face ID"
        override val compactText: String = statusLabel
    }

    data class VoiceMemo(
        override val id: String,
        val label: String,
        val durationSeconds: Long,
        val isRecording: Boolean,
        val isPaused: Boolean = !isRecording,
        val maxReferenceSeconds: Long = 60L,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.VOICE_MEMO
        override val title: String = label
        override val compactText: String = formatDuration(durationSeconds)
        val progress: Float
            get() = if (maxReferenceSeconds > 0L) {
                ((durationSeconds % maxReferenceSeconds).toFloat() / maxReferenceSeconds.toFloat())
                    .coerceIn(if (durationSeconds > 0L) 0.05f else 0f, 1f)
            } else 0f
    }

    data class HomeEvent(
        override val id: String,
        val deviceTitle: String,
        val statusText: String,
        override val updatedAtMs: Long = System.currentTimeMillis()
    ) : IslandActivity {
        override val kind: ActivityKind = ActivityKind.HOME_EVENT
        override val title: String = deviceTitle
        override val compactText: String = statusText.take(14)
    }
}

data class IslandUiState(
    val displayMode: IslandDisplayMode = IslandDisplayMode.HIDDEN,
    val primaryActivity: IslandActivity? = null,
    val secondaryActivity: IslandActivity? = null,
    val allActivities: List<IslandActivity> = emptyList()
) {
    val isVisible: Boolean
        get() = displayMode != IslandDisplayMode.HIDDEN && primaryActivity != null

    val hasMultipleActivities: Boolean
        get() = secondaryActivity != null

    val currentLevel: IslandLevel
        get() = when (displayMode) {
            IslandDisplayMode.EXPANDED -> IslandLevel.SMART_ISLAND
            else -> IslandLevel.MINI_ISLAND
        }
}

fun formatDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val seconds = safe % 60L
    return if (hours > 0L) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
