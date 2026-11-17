package com.mike.dynamicisland.activities

import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.island.ActivityKind
import kotlinx.coroutines.CoroutineScope

/**
 * Registre unifié des moteurs d'activités locales Now Bar (Minuteur, Chronomètre, Alarme).
 */
class ActivityRegistry(
    scope: CoroutineScope,
    private val activityManager: MikeActivityManager,
    private val eventManager: EventManager
) {
    val timerEngine = TimerEngine(
        scope = scope,
        onStateChanged = { timer ->
            if (timer != null) activityManager.upsert(timer)
            else activityManager.removeByKind(ActivityKind.TIMER)
        },
        onLog = { msg -> eventManager.record("Minuteur", msg, true) }
    )

    val stopwatchEngine = StopwatchEngine(
        scope = scope,
        onStateChanged = { stopwatch ->
            if (stopwatch != null) activityManager.upsert(stopwatch)
            else activityManager.removeByKind(ActivityKind.STOPWATCH)
        },
        onLog = { msg -> eventManager.record("Chronomètre", msg, true) }
    )

    val alarmEngine = AlarmEngine(
        scope = scope,
        onStateChanged = { alarm ->
            if (alarm != null) activityManager.upsert(alarm)
            else activityManager.removeByKind(ActivityKind.ALARM)
        },
        onLog = { msg -> eventManager.record("Alarme", msg, true) }
    )
}
