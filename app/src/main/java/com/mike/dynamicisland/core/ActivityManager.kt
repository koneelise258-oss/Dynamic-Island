package com.mike.dynamicisland.core

import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.IslandActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Gestionnaire central des activités en direct (Module 04 & Module 10).
 */
class MikeActivityManager {
    private val _activities = MutableStateFlow<Map<String, IslandActivity>>(emptyMap())
    val activities: StateFlow<Map<String, IslandActivity>> = _activities.asStateFlow()

    fun upsert(activity: IslandActivity) {
        _activities.update { current ->
            current + (activity.id to activity)
        }
    }

    fun remove(activityId: String) {
        _activities.update { current ->
            if (activityId in current) current - activityId else current
        }
    }

    fun removeByKind(kind: ActivityKind) {
        _activities.update { current ->
            current.filterValues { it.kind != kind }
        }
    }

    fun replaceKind(kind: ActivityKind, items: List<IslandActivity>) {
        _activities.update { current ->
            val withoutKind = current.filterValues { it.kind != kind }
            val added = items.associateBy { it.id }
            withoutKind + added
        }
    }

    fun clearAll() {
        _activities.value = emptyMap()
    }
}
