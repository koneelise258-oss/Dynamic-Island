package com.mike.dynamicisland.island

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.mike.dynamicisland.quickglass.IslandBlueprintMetrics
import com.mike.dynamicisland.quickglass.QuickGlassConfig
import com.mike.dynamicisland.settings.IslandSettings

/**
 * Callbacks réels exécutés depuis la Dynamic Island (aucun bouton décoratif).
 */
data class IslandInteractionCallbacks(
    val onToggleExpand: () -> Unit = {},
    val onCollapse: () -> Unit = {},
    val onSwapSecondary: () -> Unit = {},
    val onTimerTogglePause: () -> Unit = {},
    val onTimerAddMinute: () -> Unit = {},
    val onTimerCancel: () -> Unit = {},
    val onStopwatchToggle: () -> Unit = {},
    val onStopwatchLap: () -> Unit = {},
    val onStopwatchReset: () -> Unit = {},
    val onAlarmSnooze: () -> Unit = {},
    val onAlarmDismiss: () -> Unit = {},
    val onMusicTogglePlayPause: () -> Unit = {},
    val onMusicPrevious: () -> Unit = {},
    val onMusicNext: () -> Unit = {},
    val onNotificationOpen: (String) -> Unit = {},
    val onNotificationDismiss: (String) -> Unit = {},
    val onNotificationAction: (String, Int) -> Unit = { _, _ -> },
    val onVoiceMemoTogglePause: () -> Unit = {},
    val onVoiceMemoStop: () -> Unit = {}
)

/**
 * Point d'entrée compatible de la Dynamic Island (délègue vers DynamicIslandLayout V7
 * avec métriques Blueprint scalées dynamiquement selon l'espace disponible).
 */
@Composable
fun DynamicIslandHost(
    state: IslandUiState,
    islandSettings: IslandSettings,
    glassConfig: QuickGlassConfig,
    callbacks: IslandInteractionCallbacks,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val availableWidthDp = if (maxWidth.value.isFinite() && maxWidth.value >= 280f) {
            maxWidth.value
        } else {
            360f
        }
        val blueprint = IslandBlueprintMetrics.forScreenWidthDp(availableWidthDp)
        DynamicIslandLayout(
            state = state,
            islandSettings = islandSettings,
            glassConfig = glassConfig,
            blueprint = blueprint,
            callbacks = callbacks
        )
    }
}

fun iconForActivity(activity: IslandActivity): ImageVector = v7IconForActivity(activity)
