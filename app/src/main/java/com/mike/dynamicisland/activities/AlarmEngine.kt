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
 * Moteur d'alarme locale contextuelle pour la Now Bar / Dynamic Island (Module 04).
 */
class AlarmEngine(
    private val scope: CoroutineScope,
    private val onStateChanged: (IslandActivity.Alarm?) -> Unit,
    private val onLog: (String) -> Unit = {}
) {
    private val _state = MutableStateFlow<IslandActivity.Alarm?>(null)
    val state: StateFlow<IslandActivity.Alarm?> = _state.asStateFlow()

    private var countdownJob: Job? = null

    fun scheduleInSeconds(seconds: Long, label: String = "Alarme Mike") {
        val safeSeconds = seconds.coerceAtLeast(5L)
        countdownJob?.cancel()
        val initial = IslandActivity.Alarm(
            label = label,
            isRinging = false,
            remainingSeconds = safeSeconds
        )
        emit(initial)
        onLog("Alarme programmée dans ${initial.compactText} ($label)")
        countdownJob = scope.launch {
            var remaining = safeSeconds
            while (remaining > 0L) {
                delay(1000L)
                remaining -= 1L
                val current = _state.value ?: return@launch
                if (remaining <= 0L) {
                    emit(
                        current.copy(
                            remainingSeconds = 0L,
                            isRinging = true,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    )
                    onLog("Alarme déclenchée ($label)")
                } else {
                    emit(
                        current.copy(
                            remainingSeconds = remaining,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun triggerRingingNow(label: String = "Alarme Mike") {
        countdownJob?.cancel()
        emit(
            IslandActivity.Alarm(
                label = label,
                isRinging = true,
                remainingSeconds = 0L
            )
        )
        onLog("Alarme active ($label)")
    }

    fun snooze(minutes: Int = 5) {
        val label = _state.value?.label ?: "Alarme Mike"
        scheduleInSeconds(minutes.coerceAtLeast(1) * 60L, label)
        onLog("Alarme répétée (+$minutes min)")
    }

    fun dismiss() {
        countdownJob?.cancel()
        countdownJob = null
        if (_state.value != null) {
            emit(null)
            onLog("Alarme arrêtée")
        }
    }

    private fun emit(alarm: IslandActivity.Alarm?) {
        _state.value = alarm
        onStateChanged(alarm)
    }
}
