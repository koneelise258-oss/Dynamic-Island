package com.mike.dynamicisland.media

import android.content.Context
import com.mike.dynamicisland.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class MusicGenre(val label: String, val isNewBadge: Boolean = false) {
    POP("Pop"),
    ROCK("Rock", isNewBadge = true),
    ELECTRONIC("Electronic"),
    JAZZ("Jazz")
}

data class ObservedTrackRecord(
    val title: String,
    val artist: String,
    val packageName: String,
    val genre: MusicGenre?,
    val playCount: Int,
    val lastPlayedAtMs: Long
)

data class ListeningTelemetryState(
    val selectedGenre: MusicGenre? = null,
    val searchQuery: String = "",
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val dailyMinutesByDayOfWeek: List<Int> = List(7) { 0 },
    val hourlyMinutesBucket: List<Int> = List(6) { 0 }, // 0h, 4h, 8h, 12h, 16h, 20h
    val observedTracks: List<ObservedTrackRecord> = emptyList(),
    val totalListeningMinutes: Int = 0
)

/**
 * Contrôleur Média V7 (PARTIE 4) reliant MediaSessionController natif aux micro-widgets
 * du Niveau 3 (MIKE Control Center).
 * Aucune donnée musicale fictive n'est inventée : seules les sessions MediaSession réelles
 * alimentent l'historique local et les statistiques d'écoute.
 */
class MediaActivityController(
    private val scope: CoroutineScope,
    val sessionController: MediaSessionController
) {
    val currentMedia: StateFlow<IslandActivity.Music?> = sessionController.currentMedia

    private val _listeningTelemetry = MutableStateFlow(ListeningTelemetryState())
    val listeningTelemetry: StateFlow<ListeningTelemetryState> = _listeningTelemetry.asStateFlow()

    private var lastObservedTrackKey: String? = null

    init {
        scope.launch {
            sessionController.currentMedia.collect { media ->
                if (media != null && media.title.isNotBlank()) {
                    recordObservedSession(media)
                }
            }
        }
    }

    fun start(context: Context) = sessionController.startListening(context)
    fun stop() = sessionController.stopListening()

    fun togglePlayPause(): Boolean = sessionController.togglePlayPause()
    fun skipToNext(): Boolean = sessionController.skipToNext()
    fun skipToPrevious(): Boolean = sessionController.skipToPrevious()

    fun toggleShuffle() {
        _listeningTelemetry.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun toggleRepeat() {
        _listeningTelemetry.update { it.copy(repeatEnabled = !it.repeatEnabled) }
    }

    fun selectGenre(genre: MusicGenre?) {
        _listeningTelemetry.update { current ->
            current.copy(selectedGenre = if (current.selectedGenre == genre) null else genre)
        }
    }

    fun updateSearchQuery(query: String) {
        _listeningTelemetry.update { it.copy(searchQuery = query) }
    }

    fun assignGenreToCurrentTrack(genre: MusicGenre) {
        val current = currentMedia.value ?: return
        _listeningTelemetry.update { state ->
            val updated = state.observedTracks.map { rec ->
                if (rec.title == current.title && rec.artist == current.artist) {
                    rec.copy(genre = genre)
                } else {
                    rec
                }
            }
            state.copy(observedTracks = updated)
        }
    }

    private fun recordObservedSession(media: IslandActivity.Music) {
        val trackKey = "${media.packageName}|${media.title}|${media.artist}"
        val isNewPlay = trackKey != lastObservedTrackKey && media.isPlaying
        if (!isNewPlay) return
        lastObservedTrackKey = trackKey

        val cal = Calendar.getInstance()
        val dayIndex = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7).coerceIn(0, 6) // Lundi=0..Dimanche=6
        val hourBucket = (cal.get(Calendar.HOUR_OF_DAY) / 4).coerceIn(0, 5)
        val estimatedMin = ((media.durationMs / 60_000L).toInt()).coerceIn(1, 15)

        _listeningTelemetry.update { state ->
            val existing = state.observedTracks.firstOrNull {
                it.title == media.title && it.artist == media.artist
            }
            val nextTracks = if (existing != null) {
                state.observedTracks.map {
                    if (it == existing) it.copy(playCount = it.playCount + 1, lastPlayedAtMs = System.currentTimeMillis())
                    else it
                }
            } else {
                (listOf(
                    ObservedTrackRecord(
                        title = media.title,
                        artist = media.artist,
                        packageName = media.packageName,
                        genre = state.selectedGenre,
                        playCount = 1,
                        lastPlayedAtMs = System.currentTimeMillis()
                    )
                ) + state.observedTracks).take(25)
            }

            val nextDaily = state.dailyMinutesByDayOfWeek.toMutableList().also {
                it[dayIndex] = it[dayIndex] + estimatedMin
            }
            val nextHourly = state.hourlyMinutesBucket.toMutableList().also {
                it[hourBucket] = it[hourBucket] + estimatedMin
            }

            state.copy(
                observedTracks = nextTracks,
                dailyMinutesByDayOfWeek = nextDaily,
                hourlyMinutesBucket = nextHourly,
                totalListeningMinutes = state.totalListeningMinutes + estimatedMin
            )
        }
    }
}
