package com.mike.dynamicisland.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.RingerStateType
import com.mike.dynamicisland.media.MusicGenre
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface

/**
 * NIVEAU 3 : MIKE CONTROL CENTER (PARTIE 5).
 * Regroupe les 10 micro-widgets du Design System JSON (Source #1) — jamais placés dans la fenêtre flottante —
 * ainsi que l'accès aux états système du Niveau 1 (Silencieux, NPD, Accessoires, Mémo vocal).
 */
@Composable
fun ControlCenterScreen(
    viewModel: MikeViewModel,
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    val media by viewModel.mediaState.collectAsState()
    val listeningTelemetry by viewModel.listeningTelemetry.collectAsState()
    val systemAudio by viewModel.systemAudioTelemetry.collectAsState()
    val islandState by viewModel.islandState.collectAsState()

    val activeVoiceMemo = islandState.allActivities
        .filterIsInstance<IslandActivity.VoiceMemo>()
        .firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "MIKE Control Center",
                subtitle = "Niveau 3 · Suite Audio, Statistiques & Accessoires (hors fenêtre flottante)",
                badgeText = "Niveau 3",
                badgeColor = MikeDesignTokens.AccentGreen,
                onBack = onBack
            )
        }

        // Ligne 1 : Widget #1 (Vertical Nav Dock) + Widget #2 (Genre Selector)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VerticalNavDockWidget(
                    onHomeClick = { onNavigate(MikeRoutes.HOME) },
                    onNextClick = { viewModel.engine.mediaActivityController.skipToNext() },
                    onMusicClick = { onNavigate(MikeRoutes.MUSIC) }
                )
                GenreSelectorCardWidget(
                    selectedGenre = listeningTelemetry.selectedGenre,
                    onSelectGenre = { viewModel.selectMusicGenre(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Widget #5 : Barre de transport compacte (52dp, pilule blanche centrale)
        item {
            CompactTransportBarWidget(
                isPlaying = media?.isPlaying == true,
                shuffleEnabled = listeningTelemetry.shuffleEnabled,
                repeatEnabled = listeningTelemetry.repeatEnabled,
                onOutputClick = { viewModel.refreshSystemAudioTelemetry() },
                onTimerClick = { viewModel.startTimer(300L, "Minuteur 5m") },
                onShuffleClick = { viewModel.engine.mediaActivityController.toggleShuffle() },
                onPreviousClick = { viewModel.engine.mediaActivityController.skipToPrevious() },
                onPlayPauseClick = { viewModel.engine.mediaActivityController.togglePlayPause() },
                onNextClick = { viewModel.engine.mediaActivityController.skipToNext() },
                onRepeatClick = { viewModel.engine.mediaActivityController.toggleRepeat() },
                onVolumeBoostClick = {
                    val next = (systemAudio.mediaVolumePercent + 10).coerceAtMost(100)
                    viewModel.setMediaVolumePercent(next)
                }
            )
        }

        // Widget #3 : Volume Radial Slider (9 sphères vertes réelles AudioManager)
        item {
            VolumeRadialSliderWidget(
                volumePercent = systemAudio.mediaVolumePercent,
                currentMedia = media,
                onVolumeChange = { viewModel.setMediaVolumePercent(it) },
                onOutputClick = { viewModel.refreshSystemAudioTelemetry() }
            )
        }

        // Widget #6 (Social Listening Pill) + Widget #10 (Voice Note Waveform Pill)
        item {
            SocialListeningPillWidget(
                isAudioPlaying = media?.isPlaying == true,
                onShareTrackClick = {
                    val shareText = media?.let { "En écoute sur Mike Dynamic Island : ${it.title} — ${it.artist}" }
                        ?: "Mike Dynamic Island — Contrôle audio & activités en direct"
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(Intent.createChooser(sendIntent, "Partager")) }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            VoiceNoteWaveformPillWidget(
                voiceMemo = activeVoiceMemo,
                onToggleVoiceNote = { viewModel.toggleVoiceMemoPauseResume() },
                onStopVoiceNote = { viewModel.clearVoiceMemoActivity() }
            )
        }

        // Widget #8 : Accessoires Audio / Écouteurs (état réel AudioDeviceCallback)
        item {
            AudioAccessoryBatteryCardWidget(
                accessory = systemAudio.connectedAccessory,
                onRefreshClick = { viewModel.refreshSystemAudioTelemetry() }
            )
        }

        // Widget #4 : Préférences, Égaliseur hebdomadaire & Recherche
        item {
            PreferencesEqualizerSearchWidget(
                telemetry = listeningTelemetry,
                onSearchChange = { viewModel.updateMusicSearch(it) },
                onCycleGenre = {
                    val genres = MusicGenre.entries
                    val currentIdx = listeningTelemetry.selectedGenre?.let { genres.indexOf(it) } ?: -1
                    val next = genres.getOrNull(currentIdx + 1)
                    viewModel.engine.mediaActivityController.selectGenre(next)
                },
                onOpenSettings = { onNavigate(MikeRoutes.SETTINGS) }
            )
        }

        // Widget #9 (Listening Statistics) + Widget #7 (Favorites Analytics)
        item {
            ListeningStatisticsCardWidget(
                telemetry = listeningTelemetry,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            FavoritesAnalyticsCardWidget(telemetry = listeningTelemetry)
        }

        // Raccourcis de vérification des états système Niveau 1 (Silencieux, NPD)
        item {
            MikeSectionLabel("Vérification des états Mini Island")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.engine.systemActivityController.publishSilentModeBanner(RingerStateType.SILENT) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("cc_preview_silent_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mike_silent_bell),
                            contentDescription = null,
                            tint = MikeDesignTokens.AccentOrange,
                            modifier = Modifier.padding(end = 6.dp).size(16.dp)
                        )
                        Text("Silencieux", color = MikeColors.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.engine.systemActivityController.publishDndBanner(true) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("cc_preview_dnd_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mike_dnd_moon),
                            contentDescription = null,
                            tint = MikeDesignTokens.AccentPurple,
                            modifier = Modifier.padding(end = 6.dp).size(16.dp)
                        )
                        Text("NPD", color = MikeColors.White)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
