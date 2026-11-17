package com.mike.dynamicisland.activities

import com.mike.dynamicisland.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Moteur de minuteur réel (Module 04 & 05) sans polling lorsqu'il est inactif.
 */
class TimerEngine(
    private val scope: CoroutineScope,
    private val onStateChanged: (IslandActivity.Timer?) -> Unit,
    private val onLog: (String) -> Unit = {}
) {
    private val _state = MutableStateFlow<IslandActivity.Timer?>(null)
    val state: StateFlow<IslandActivity.Timer?> = _state.asStateFlow()

    private var tickerJob: Job? = null

    fun start(durationSeconds: Long, label: String = "Minuteur") {
        val safeDuration = durationSeconds.coerceAtLeast(1L)
        tickerJob?.cancel()
        val initial = IslandActivity.Timer(
            title = label,
            remainingSeconds = safeDuration,
            totalSeconds = safeDuration,
            isPaused = false,
            isFinished = false
        )
        emit(initial)
        onLog("Minuteur démarré (${initial.compactText})")
        startTicker()
    }

    fun pause() {
        val current = _state.value ?: return
        if (current.isPaused || current.isFinished) return
        tickerJob?.cancel()
        emit(current.copy(isPaused = true, updatedAtMs = System.currentTimeMillis()))
        onLog("Minuteur mis en pause (${current.compactText})")
    }

    fun resume() {
        val current = _state.value ?: return
        if (!current.isPaused || current.isFinished) return
        emit(current.copy(isPaused = false, updatedAtMs = System.currentTimeMillis()))
        onLog("Minuteur repris (${current.compactText})")
        startTicker()
    }

    fun togglePauseResume() {
        val current = _state.value ?: return
        if (current.isPaused) resume() else pause()
    }

    fun addOneMinute() {
        val current = _state.value ?: return
        val updated = current.copy(
            remainingSeconds = current.remainingSeconds + 60L,
            totalSeconds = current.totalSeconds + 60L,
            isFinished = false,
            updatedAtMs = System.currentTimeMillis()
        )
        emit(updated)
        if (!updated.isPaused && tickerJob?.isActive != true) {
            startTicker()
        }
        onLog("Minuteur +1 min (${updated.compactText})")
    }

    fun cancel() {
        tickerJob?.cancel()
        tickerJob = null
        if (_state.value != null) {
            emit(null)
            onLog("Minuteur annulé")
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(1000L)
                val current = _state.value ?: break
                if (current.isPaused) break
                val nextRemaining = (current.remainingSeconds - 1L).coerceAtLeast(0L)
                if (nextRemaining == 0L) {
                    emit(
                        current.copy(
                            remainingSeconds = 0L,
                            isPaused = false,
                            isFinished = true,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    )
                    onLog("Minuteur terminé")
                    break
                } else {
                    emit(
                        current.copy(
                            remainingSeconds = nextRemaining,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private fun emit(timer: IslandActivity.Timer?) {
        _state.value = timer
        onStateChanged(timer)
    }
}
