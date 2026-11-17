package com.mike.dynamicisland.core

import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity

/**
 * Gestionnaire de priorités V7 (PARTIE 4) prenant en charge l'ensemble des états système
 * (Appels entrants/actifs, Alarme, Face ID, Minuteur, Navigation, Musique, Silencieux, NPD, Accessoires, etc.).
 */
class ActivityPriorityManager(
    private val delegate: PriorityManager = PriorityManager()
) {
    fun resolve(
        activities: List<IslandActivity>,
        priorityOrder: List<ActivityKind>,
        pinnedPrimaryId: String? = null,
        allowMultiple: Boolean = true
    ): ResolvedIslandSlots {
        val mergedOrder = (priorityOrder + ActivityKind.entries).distinct()
        return delegate.resolve(
            activities = activities,
            priorityOrder = mergedOrder,
            pinnedPrimaryId = pinnedPrimaryId,
            allowMultiple = allowMultiple
        )
    }
}
