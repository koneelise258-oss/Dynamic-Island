package com.mike.dynamicisland.core

import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity

data class ResolvedIslandSlots(
    val primary: IslandActivity?,
    val secondary: IslandActivity?,
    val ordered: List<IslandActivity>
)

/**
 * Moteur de priorité multi-activités (Module 02 & Module 10).
 * Garantit qu'un appel reste toujours prioritaire sur une notification ordinaire
 * et que la musique reste accessible en bulle secondaire pendant une autre activité.
 */
class PriorityManager {

    fun resolve(
        activities: List<IslandActivity>,
        priorityOrder: List<ActivityKind>,
        pinnedPrimaryId: String? = null,
        allowMultiple: Boolean = true
    ): ResolvedIslandSlots {
        if (activities.isEmpty()) {
            return ResolvedIslandSlots(primary = null, secondary = null, ordered = emptyList())
        }

        val mergedOrder = (priorityOrder + ActivityKind.entries).distinct()
        val rankByKind: Map<ActivityKind, Int> = mergedOrder
            .mapIndexed { index, kind -> kind to index }
            .toMap()

        val sorted = activities.sortedWith(
            compareBy<IslandActivity>(
                // Règle impérative : un appel en cours ne passe jamais derrière une notification ordinaire
                { if (it.kind == ActivityKind.CALL) -2 else if (it.kind == ActivityKind.ALARM && (it as? IslandActivity.Alarm)?.isRinging == true) -1 else 0 },
                { rankByKind[it.kind] ?: Int.MAX_VALUE },
                { -it.updatedAtMs }
            )
        )

        val primary = if (pinnedPrimaryId != null) {
            val pinned = sorted.firstOrNull { it.id == pinnedPrimaryId }
            val top = sorted.first()
            // Un appel ou une alarme qui sonne prend toujours la priorité même si autre chose était épinglé
            if (top.kind == ActivityKind.CALL || (top is IslandActivity.Alarm && top.isRinging)) {
                top
            } else {
                pinned ?: top
            }
        } else {
            sorted.first()
        }

        if (!allowMultiple || sorted.size < 2) {
            return ResolvedIslandSlots(primary = primary, secondary = null, ordered = sorted)
        }

        val remaining = sorted.filterNot { it.id == primary.id }
        // Garde la musique facilement accessible en activité secondaire si elle tourne
        val musicSecondary = remaining.firstOrNull { it.kind == ActivityKind.MUSIC }
        val secondary = if (primary.kind != ActivityKind.MUSIC && musicSecondary != null) {
            musicSecondary
        } else {
            remaining.firstOrNull()
        }

        return ResolvedIslandSlots(
            primary = primary,
            secondary = secondary,
            ordered = listOf(primary) + remaining
        )
    }
}
