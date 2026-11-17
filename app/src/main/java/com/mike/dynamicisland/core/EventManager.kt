package com.mike.dynamicisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MikeEventLog(
    val id: Long,
    val timestampText: String,
    val category: String,
    val message: String,
    val isRealSystemEvent: Boolean
)

/**
 * Journal d'événements borné en mémoire (max 80 entrées pour 3 Go RAM)
 * distinguant systématiquement les événements système réels des tests manuels.
 */
class EventManager(private val maxEntries: Int = 80) {
    private val _events = MutableStateFlow<List<MikeEventLog>>(emptyList())
    val events: StateFlow<List<MikeEventLog>> = _events.asStateFlow()

    private var sequence = 0L
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)

    fun record(category: String, message: String, isRealSystemEvent: Boolean) {
        val nextId = ++sequence
        val timeText = synchronized(formatter) { formatter.format(Date()) }
        val entry = MikeEventLog(
            id = nextId,
            timestampText = timeText,
            category = category,
            message = message,
            isRealSystemEvent = isRealSystemEvent
        )
        _events.update { current ->
            (listOf(entry) + current).take(maxEntries)
        }
    }

    fun clear() {
        _events.value = emptyList()
    }
}
