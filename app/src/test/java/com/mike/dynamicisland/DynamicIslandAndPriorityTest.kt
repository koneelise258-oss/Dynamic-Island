package com.mike.dynamicisland

import com.mike.dynamicisland.activities.AlarmEngine
import com.mike.dynamicisland.activities.StopwatchEngine
import com.mike.dynamicisland.activities.TimerEngine
import com.mike.dynamicisland.core.EventManager
import com.mike.dynamicisland.core.MikeActivityManager
import com.mike.dynamicisland.core.PriorityManager
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.DynamicIslandEngine
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.IslandDisplayMode
import com.mike.dynamicisland.settings.IslandSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DynamicIslandAndPriorityTest {

    @Test
    fun `island stays hidden when empty and transitions compact to expanded to hidden`() = runTest {
        val activityManager = MikeActivityManager()
        val settingsFlow = MutableStateFlow(IslandSettings(autoCollapseSeconds = 0))
        val engine = DynamicIslandEngine(
            scope = backgroundScope,
            activityManager = activityManager,
            priorityManager = PriorityManager(),
            settingsFlow = settingsFlow,
            eventManager = EventManager()
        )

        runCurrent()
        assertEquals(IslandDisplayMode.HIDDEN, engine.uiState.value.displayMode)
        assertFalse(engine.uiState.value.isVisible)

        val timer = IslandActivity.Timer(remainingSeconds = 272L, totalSeconds = 300L, isPaused = false)
        activityManager.upsert(timer)
        runCurrent()
        assertEquals(IslandDisplayMode.COMPACT, engine.uiState.value.displayMode)
        assertEquals("04:32", engine.uiState.value.primaryActivity?.compactText)

        engine.expand()
        runCurrent()
        assertEquals(IslandDisplayMode.EXPANDED, engine.uiState.value.displayMode)

        activityManager.remove(timer.id)
        runCurrent()
        assertEquals(IslandDisplayMode.HIDDEN, engine.uiState.value.displayMode)
        assertFalse(engine.uiState.value.isVisible)
    }

    @Test
    fun `call always preempts ordinary notification and keeps music in secondary slot`() {
        val priorityManager = PriorityManager()
        val notification = IslandActivity.Notification(
            id = "notif_whatsapp",
            notificationKey = "k1",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alex",
            message = "Salut"
        )
        val music = IslandActivity.Music(
            packageName = "com.spotify.music",
            title = "Midnight",
            artist = "Synth",
            isPlaying = true,
            positionMs = 10_000L,
            durationMs = 180_000L
        )
        val call = IslandActivity.Call(
            id = "call_1",
            callerName = "Maman",
            statusText = "00:42",
            startedAtElapsedMs = 100L,
            packageName = "com.android.dialer"
        )

        val resolved = priorityManager.resolve(
            activities = listOf(notification, music, call),
            // Même si l'utilisateur place Notification devant Call dans l'ordre, Call reste prioritaire
            priorityOrder = listOf(ActivityKind.NOTIFICATION, ActivityKind.MUSIC, ActivityKind.CALL),
            pinnedPrimaryId = notification.id,
            allowMultiple = true
        )

        assertEquals(ActivityKind.CALL, resolved.primary?.kind)
        assertEquals(ActivityKind.MUSIC, resolved.secondary?.kind)
    }

    @Test
    fun `timer, stopwatch, and alarm engines execute real state transitions`() = runTest {
        var lastTimer: IslandActivity.Timer? = null
        val timerEngine = TimerEngine(
            scope = backgroundScope,
            onStateChanged = { lastTimer = it }
        )

        timerEngine.start(durationSeconds = 10L, label = "Cuisson")
        assertEquals(10L, lastTimer?.remainingSeconds)
        advanceTimeBy(3000L)
        runCurrent()
        assertEquals(7L, lastTimer?.remainingSeconds)

        timerEngine.pause()
        assertTrue(lastTimer?.isPaused == true)
        advanceTimeBy(2000L)
        runCurrent()
        assertEquals(7L, lastTimer?.remainingSeconds)

        timerEngine.addOneMinute()
        assertEquals(67L, lastTimer?.remainingSeconds)
        timerEngine.cancel()
        assertNull(lastTimer)

        var fakeClock = 1000L
        var lastStopwatch: IslandActivity.Stopwatch? = null
        val stopwatchEngine = StopwatchEngine(
            scope = backgroundScope,
            elapsedTimeProvider = { fakeClock },
            onStateChanged = { lastStopwatch = it }
        )
        stopwatchEngine.startOrResume()
        fakeClock = 3500L
        stopwatchEngine.recordLap()
        assertEquals(1, lastStopwatch?.laps?.size)
        assertEquals(2500L, lastStopwatch?.elapsedMillis)
        stopwatchEngine.pause()
        assertFalse(lastStopwatch?.isRunning ?: true)
        stopwatchEngine.reset()
        assertNull(lastStopwatch)

        var lastAlarm: IslandActivity.Alarm? = null
        val alarmEngine = AlarmEngine(
            scope = backgroundScope,
            onStateChanged = { lastAlarm = it }
        )
        alarmEngine.triggerRingingNow("Réveil")
        assertNotNull(lastAlarm)
        assertTrue(lastAlarm!!.isRinging)
        alarmEngine.snooze(5)
        assertFalse(lastAlarm!!.isRinging)
        assertEquals(300L, lastAlarm!!.remainingSeconds)
        alarmEngine.dismiss()
        assertNull(lastAlarm)
    }
}
