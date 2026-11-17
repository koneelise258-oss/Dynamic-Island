package com.mike.dynamicisland.activities

import android.os.SystemClock
import com.mike.dynamicisland.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Moteur de chronomètre réel basé sur SystemClock.elapsedRealtime() (Module 04).
 */
class StopwatchEngine(
    private val scope: CoroutineScope,
    private val elapsedTimeProvider: () -> Long = { SystemClock.elapsedRealtime() },
    private val onStateChanged: (IslandActivity.Stopwatch?) -> Unit,
    private val onLog: (String) -> Unit = {}
) {
    private val _state = MutableStateFlow<IslandActivity.Stopwatch?>(null)
    val state: StateFlow<IslandActivity.Stopwatch?> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var accumulatedMs: Long = 0L
    private var startedAtMs: Long = 0L

    fun startOrResume() {
        val current = _state.value
        if (current?.isRunning == true) return
        startedAtMs = elapsedTimeProvider()
        val next = (current ?: IslandActivity.Stopwatch(elapsedMillis = 0L, isRunning = true))
            .copy(isRunning = true, updatedAtMs = System.currentTimeMillis())
        emit(next)
        onLog("Chronomètre démarré")
        startTicker()
    }

    fun pause() {
        val current = _state.value ?: return
        if (!current.isRunning) return
        tickerJob?.cancel()
        val now = elapsedTimeProvider()
        accumulatedMs += (now - startedAtMs).coerceAtLeast(0L)
        emit(
            current.copy(
                elapsedMillis = accumulatedMs,
                isRunning = false,
                updatedAtMs = System.currentTimeMillis()
            )
        )
        onLog("Chronomètre en pause (${current.detailedText})")
    }

    fun toggle() {
        val current = _state.value
        if (current?.isRunning == true) pause() else startOrResume()
    }

    fun recordLap() {
        val current = _state.value ?: return
        val total = computeCurrentElapsed(current.isRunning)
        val updatedLaps = (listOf(total) + current.laps).take(20)
        emit(
            current.copy(
                elapsedMillis = total,
                laps = updatedLaps,
                updatedAtMs = System.currentTimeMillis()
            )
        )
        onLog("Tour enregistré (#${updatedLaps.size})")
    }

    fun reset() {
        tickerJob?.cancel()
        tickerJob = null
        accumulatedMs = 0L
        startedAtMs = 0L
        if (_state.value != null) {
            emit(null)
            onLog("Chronomètre réinitialisé")
        }
    }

    private fun computeCurrentElapsed(isRunning: Boolean): Long {
        return if (isRunning) {
            accumulatedMs + (elapsedTimeProvider() - startedAtMs).coerceAtLeast(0L)
        } else {
            accumulatedMs
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(200L)
                val current = _state.value ?: break
                if (!current.isRunning) break
                val total = computeCurrentElapsed(true)
                emit(
                    current.copy(
                        elapsedMillis = total,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun emit(stopwatch: IslandActivity.Stopwatch?) {
        _state.value = stopwatch
        onStateChanged(stopwatch)
    }
}
