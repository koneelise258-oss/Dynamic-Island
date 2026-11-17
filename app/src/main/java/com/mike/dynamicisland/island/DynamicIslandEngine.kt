package com.mike.dynamicisland.island

import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.core.PriorityManager
import com.mike.dynamicisland.settings.IslandSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Moteur principal de la Dynamic Island (Module 02 & Module 05).
 * Ne reste jamais affiché en permanence : bascule automatiquement en HIDDEN dès qu'aucune activité n'est active.
 */
class DynamicIslandEngine(
    private val scope: CoroutineScope,
    private val activityManager: MikeActivityManager,
    private val priorityManager: PriorityManager,
    private val settingsFlow: StateFlow<IslandSettings>,
    private val eventManager: EventManager
) {
    private val requestedExpanded = MutableStateFlow(false)
    private val pinnedPrimaryId = MutableStateFlow<String?>(null)
    private var autoCollapseJob: Job? = null

    val uiState: StateFlow<IslandUiState> = combine(
        activityManager.activities,
        settingsFlow,
        requestedExpanded,
        pinnedPrimaryId
    ) { activityMap, settings, isExpanded, pinnedId ->
        val filtered = activityMap.values.filter { it.kind in settings.enabledActivities }
        val resolved = priorityManager.resolve(
            activities = filtered,
            priorityOrder = settings.priorityOrder,
            pinnedPrimaryId = pinnedId,
            allowMultiple = settings.allowMultipleActivities
        )

        val mode = when {
            resolved.primary == null -> IslandDisplayMode.HIDDEN
            isExpanded && settings.tapToExpandEnabled -> IslandDisplayMode.EXPANDED
            else -> IslandDisplayMode.COMPACT
        }

        IslandUiState(
            displayMode = mode,
            primaryActivity = resolved.primary,
            secondaryActivity = resolved.secondary,
            allActivities = resolved.ordered
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = IslandUiState()
    )

    fun expand() {
        if (uiState.value.primaryActivity == null) return
        if (!settingsFlow.value.tapToExpandEnabled) return
        requestedExpanded.value = true
        scheduleAutoCollapse()
        eventManager.record("Island", "Dynamic Island développée", true)
    }

    fun collapse() {
        autoCollapseJob?.cancel()
        if (requestedExpanded.value) {
            requestedExpanded.value = false
            eventManager.record("Island", "Dynamic Island réduite", true)
        }
    }

    fun toggleExpanded() {
        if (requestedExpanded.value) collapse() else expand()
    }

    fun swapPrimaryAndSecondary() {
        val secondary = uiState.value.secondaryActivity ?: return
        pinnedPrimaryId.value = secondary.id
        eventManager.record("Island", "Bascule vers activité : ${secondary.kind.title}", true)
    }

    fun focusActivity(activityId: String) {
        pinnedPrimaryId.value = activityId
        expand()
    }

    private fun scheduleAutoCollapse() {
        autoCollapseJob?.cancel()
        val timeoutSec = settingsFlow.value.autoCollapseSeconds
        if (timeoutSec <= 0) return
        autoCollapseJob = scope.launch {
            delay(timeoutSec * 1000L)
            requestedExpanded.value = false
        }
    }
}
