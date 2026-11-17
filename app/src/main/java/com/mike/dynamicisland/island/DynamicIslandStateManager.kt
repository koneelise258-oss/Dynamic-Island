package com.mike.dynamicisland.island

import com.mike.dynamicisland.quickglass.IslandBlueprintMetrics
import com.mike.dynamicisland.utils.DeviceProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Gestionnaire d'état V7 (PARTIE 4) exposant le niveau courant
 * (Niveau 1 : Mini Island / Niveau 2 : Smart Island / Masqué) et les métriques Blueprint adaptées.
 */
class DynamicIslandStateManager(
    private val engine: DynamicIslandEngine,
    private val deviceProfile: StateFlow<DeviceProfile>
) {
    val uiState: StateFlow<IslandUiState> = engine.uiState

    fun currentBlueprintMetrics(): IslandBlueprintMetrics {
        return IslandBlueprintMetrics.forScreenWidthDp(deviceProfile.value.screenWidthDp)
    }

    fun isMiniIslandActive(): Boolean = uiState.value.displayMode == IslandDisplayMode.COMPACT

    fun isSmartIslandActive(): Boolean = uiState.value.displayMode == IslandDisplayMode.EXPANDED

    fun isHidden(): Boolean = !uiState.value.isVisible
}

/**
 * Contrôleur d'interactions de la Dynamic Island (PARTIE 4).
 * Coordonne les transitions Niveau 1 <-> Niveau 2 et la permutation multi-activités.
 */
class DynamicIslandController(
    private val engine: DynamicIslandEngine
) {
    fun expandToSmartIsland() = engine.expand()
    fun collapseToMiniIsland() = engine.collapse()
    fun toggleLevel() = engine.toggleExpanded()
    fun swapActivities() = engine.swapPrimaryAndSecondary()
    fun focusActivity(activityId: String) = engine.focusActivity(activityId)
}
