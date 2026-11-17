package com.mike.dynamicisland.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.core.ConnectedAudioAccessory
import com.mike.dynamicisland.island.AnimatedWaveformBars
import com.mike.dynamicisland.island.IslandActivity
import com.mike.dynamicisland.island.formatDuration
import com.mike.dynamicisland.media.ListeningTelemetryState
import com.mike.dynamicisland.media.MusicGenre
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import kotlin.math.roundToInt

/**
 * Widget #1 JSON (vertical_nav_dock) : 48dp × 148dp, rayon 24dp.
 */
@Composable
fun VerticalNavDockWidget(
    onHomeClick: () -> Unit,
    onNextClick: () -> Unit,
    onMusicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("cc_vertical_nav_dock")
            .width(52.dp)
            .height(152.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(6.dp)
    ) {
        IconButton(onClick = onHomeClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Home, contentDescription = "Accueil", tint = MikeDesignTokens.TextSecondary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onNextClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.FastForward, contentDescription = "Piste suivante", tint = MikeDesignTokens.TextPrimary, modifier = Modifier.size(20.dp))
        }
        Surface(
            shape = CircleShape,
            color = MikeDesignTokens.ActiveSurfaceLight,
            onClick = onMusicClick,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, contentDescription = "Musique active", tint = MikeDesignTokens.IslandBackground, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Widget #2 JSON (genre_selector_card) : Filtre par genre (Pop vert, Rock rouge+new, Electronic bleu, Jazz jaune).
 */
@Composable
fun GenreSelectorCardWidget(
    selectedGenre: MusicGenre?,
    onSelectGenre: (MusicGenre) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .testTag("cc_genre_selector_card")
            .height(152.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Genre",
            style = MaterialTheme.typography.titleSmall,
            color = MikeDesignTokens.TextSecondary
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MusicGenre.entries.forEach { genre ->
                val color = genreAccentColor(genre)
                val isSelected = selectedGenre == genre
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) color.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { onSelectGenre(genre) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = genre.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MikeDesignTokens.TextPrimary else MikeDesignTokens.TextPrimary.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                    if (genre.isNewBadge) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MikeDesignTokens.AccentYellow)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "new",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = MikeDesignTokens.IslandBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Widget #3 JSON (volume_radial_slider) :
 * Jauge tactile réelle à 9 sphères lumineuses vertes (#30D158) pilotant AudioManager.STREAM_MUSIC
 * + piste MediaSession réelle en cours.
 */
@Composable
fun VolumeRadialSliderWidget(
    volumePercent: Int,
    currentMedia: IslandActivity.Music?,
    onVolumeChange: (Int) -> Unit,
    onOutputClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .testTag("cc_volume_radial_slider")
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Volume", style = MaterialTheme.typography.titleMedium, color = MikeDesignTokens.TextPrimary)
                Text(
                    text = "$volumePercent%",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MikeDesignTokens.AccentGreen
                )
            }
            IconButton(onClick = onOutputClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mike_audio_output),
                    contentDescription = "Sortie audio",
                    tint = MikeDesignTokens.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Jauge interactive à 9 sphères lumineuses vertes (#30D158) avec halo central
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onVolumeChange((ratio * 100f).roundToInt())
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onVolumeChange((ratio * 100f).roundToInt())
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                val sphereCount = 9
                val activeCount = ((volumePercent / 100f) * sphereCount).roundToInt().coerceIn(0, sphereCount)
                val spacing = size.width / (sphereCount + 1)
                val centerY = size.height / 2f

                // Halo central subtil sous les sphères actives
                if (activeCount > 0) {
                    val glowCenterX = spacing * activeCount.coerceAtLeast(1)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MikeDesignTokens.AccentGreen.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(glowCenterX, centerY),
                            radius = size.height * 1.1f
                        ),
                        radius = size.height * 1.1f,
                        center = Offset(glowCenterX, centerY)
                    )
                }

                for (i in 1..sphereCount) {
                    val cx = spacing * i
                    val isLit = i <= activeCount
                    val isPeak = i == activeCount && activeCount > 0
                    val radius = when {
                        isPeak -> 9.dp.toPx()
                        isLit -> 6.5.dp.toPx()
                        else -> 5.dp.toPx()
                    }
                    drawCircle(
                        color = if (isLit) MikeDesignTokens.AccentGreen else MikeDesignTokens.PillButtonSurface,
                        radius = radius,
                        center = Offset(cx, centerY)
                    )
                    if (isPeak) {
                        drawCircle(
                            color = MikeDesignTokens.TextPrimary.copy(alpha = 0.55f),
                            radius = radius * 0.38f,
                            center = Offset(cx - radius * 0.22f, centerY - radius * 0.22f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Morceau réel en cours (MediaSession) + Timeline
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val bmp = currentMedia?.artwork
            if (bmp != null && !bmp.isRecycled) {
                val img = remember(bmp) { bmp.asImageBitmap() }
                Image(
                    bitmap = img,
                    contentDescription = "Pochette",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(MikeDesignTokens.PillButtonSurface)
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MikeDesignTokens.AccentGreen, modifier = Modifier.size(18.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentMedia?.title ?: "Aucune lecture MediaSession",
                    style = MaterialTheme.typography.titleSmall,
                    color = MikeDesignTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentMedia?.artist ?: "Lancez une app musicale pour synchroniser",
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeDesignTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (currentMedia != null && currentMedia.durationMs > 0L) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { currentMedia.progress },
                color = MikeDesignTokens.AccentGreen,
                trackColor = MikeDesignTokens.PillButtonSurface,
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50))
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(formatDuration(currentMedia.positionMs / 1000L), style = MaterialTheme.typography.labelSmall, color = MikeDesignTokens.TextSecondary)
                Text(formatDuration(currentMedia.durationMs / 1000L), style = MaterialTheme.typography.labelSmall, color = MikeDesignTokens.TextSecondary)
            }
        }
    }
}

/**
 * Widget #4 JSON (preferences_equalizer_search) :
 * Égaliseur hebdomadaire + recherche locale + filtre genre.
 */
@Composable
fun PreferencesEqualizerSearchWidget(
    telemetry: ListeningTelemetryState,
    onSearchChange: (String) -> Unit,
    onCycleGenre: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .testTag("cc_preferences_equalizer_search")
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Preferences", style = MaterialTheme.typography.titleMedium, color = MikeDesignTokens.TextPrimary)
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "Paramètres", tint = MikeDesignTokens.TextSecondary)
            }
        }

        // Égaliseur hebdomadaire (L M M J V S D) basé sur les sessions réellement observées
        val days = listOf("L", "M", "M", "J", "V", "S", "D")
        val maxVal = (telemetry.dailyMinutesByDayOfWeek.maxOrNull() ?: 0).coerceAtLeast(10)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp)
        ) {
            days.forEachIndexed { idx, label ->
                val minutes = telemetry.dailyMinutesByDayOfWeek.getOrElse(idx) { 0 }
                val barFraction = (minutes.toFloat() / maxVal.toFloat()).coerceIn(0.14f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .fillMaxHeight(barFraction * 0.72f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (minutes > 0) MikeDesignTokens.AccentBlue else MikeDesignTokens.PillButtonSurface
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MikeDesignTokens.TextSecondary)
                }
            }
        }

        // Champ de recherche musicale + sélecteur Genres
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MikeDesignTokens.PillButtonSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MikeDesignTokens.TextSecondary, modifier = Modifier.size(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (telemetry.searchQuery.isEmpty()) {
                    Text("Filtrer titres observés...", style = MaterialTheme.typography.bodySmall, color = MikeDesignTokens.TextSecondary)
                }
                BasicTextField(
                    value = telemetry.searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MikeDesignTokens.TextPrimary),
                    cursorBrush = SolidColor(MikeDesignTokens.AccentBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Surface(
                color = MikeDesignTokens.CardElevatedSurface,
                shape = RoundedCornerShape(50),
                onClick = onCycleGenre
            ) {
                Text(
                    text = telemetry.selectedGenre?.label ?: "Genres",
                    style = MaterialTheme.typography.labelSmall,
                    color = MikeDesignTokens.TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Widget #5 JSON (compact_transport_bar) :
 * Barre 52dp avec sortie, minuteur, pilule blanche centrale (Shuffle, Précédent, Play, Suivant, Repeat), volume.
 */
@Composable
fun CompactTransportBarWidget(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    onOutputClick: () -> Unit,
    onTimerClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onVolumeBoostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .testTag("cc_compact_transport_bar")
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp)
    ) {
        IconButton(onClick = onOutputClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_mike_audio_output),
                contentDescription = "Diffusion audio",
                tint = MikeDesignTokens.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onTimerClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Timer, contentDescription = "Minuteur rapide", tint = MikeDesignTokens.AccentOrange, modifier = Modifier.size(18.dp))
        }

        // Pilule blanche centrale (#FFFFFF)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(MikeDesignTokens.ActiveSurfaceLight)
                .padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = onShuffleClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Aléatoire",
                    tint = if (shuffleEnabled) MikeDesignTokens.AccentBlue else MikeDesignTokens.IslandBackground.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onPreviousClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Précédent", tint = MikeDesignTokens.IslandBackground, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Lecture",
                    tint = MikeDesignTokens.IslandBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onNextClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.FastForward, contentDescription = "Suivant", tint = MikeDesignTokens.IslandBackground, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRepeatClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = "Répéter",
                    tint = if (repeatEnabled) MikeDesignTokens.AccentBlue else MikeDesignTokens.IslandBackground.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        IconButton(onClick = onVolumeBoostClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "Volume", tint = MikeDesignTokens.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Widget #6 JSON (social_listening_pill) :
 * Pilule de partage audio 52dp : 3 pastilles audio superposées + onde dynamique + partage Android réel.
 */
@Composable
fun SocialListeningPillWidget(
    isAudioPlaying: Boolean,
    onShareTrackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .testTag("cc_social_listening_pill")
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(0.75.dp, MikeColors.Hairline, RoundedCornerShape(26.dp))
            .padding(horizontal = 8.dp)
    ) {
        // 3 pastilles vectorielles audio superposées (zéro faux profil utilisateur)
        Box(modifier = Modifier.width(64.dp).height(30.dp), contentAlignment = Alignment.CenterStart) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 0.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MikeDesignTokens.PillButtonSurface)
                    .border(1.5.dp, MikeDesignTokens.CardElevatedSurface, CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mike_earbuds),
                    contentDescription = null,
                    tint = MikeDesignTokens.AccentGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 17.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MikeDesignTokens.PillButtonSurface)
                    .border(1.5.dp, MikeDesignTokens.CardElevatedSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MikeDesignTokens.AccentBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 34.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MikeDesignTokens.PillButtonSurface)
                    .border(1.5.dp, MikeDesignTokens.CardElevatedSurface, CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mike_audio_output),
                    contentDescription = null,
                    tint = MikeDesignTokens.TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        AnimatedWaveformBars(
            color = if (isAudioPlaying) MikeDesignTokens.AccentGreen else MikeDesignTokens.TextSecondary,
            isActive = isAudioPlaying,
            barCount = 5
        )

        Surface(
            shape = CircleShape,
            color = MikeDesignTokens.ActiveSurfaceLight,
            onClick = onShareTrackClick,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Share, contentDescription = "Partager le titre", tint = MikeDesignTokens.IslandBackground, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Widget #7 JSON (favorites_analytics_card) :
 * Graphique Gantt coloré par genre + compteur local réel + liste des pistes observées.
 */
@Composable
fun FavoritesAnalyticsCardWidget(
    telemetry: ListeningTelemetryState,
    modifier: Modifier = Modifier
) {
    val filteredTracks = remember(telemetry.observedTracks, telemetry.selectedGenre, telemetry.searchQuery) {
        telemetry.observedTracks.filter { track ->
            val genreMatch = telemetry.selectedGenre == null || track.genre == telemetry.selectedGenre
            val queryMatch = telemetry.searchQuery.isBlank() ||
                track.title.contains(telemetry.searchQuery, ignoreCase = true) ||
                track.artist.contains(telemetry.searchQuery, ignoreCase = true)
            genreMatch && queryMatch
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .testTag("cc_favorites_analytics_card")
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Favorites", style = MaterialTheme.typography.titleMedium, color = MikeDesignTokens.TextPrimary)
            Column(horizontalAlignment = Alignment.End) {
                Text("SESSIONS LOCALES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MikeDesignTokens.TextSecondary)
                Text(
                    text = "${telemetry.observedTracks.size} titre(s)",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MikeDesignTokens.AccentGreen
                )
            }
        }

        // Graphique Gantt coloré par genre
        val total = telemetry.observedTracks.size.coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MusicGenre.entries.forEachIndexed { idx, genre ->
                val count = telemetry.observedTracks.count { it.genre == genre }
                val ratio = if (count > 0) (count.toFloat() / total.toFloat()).coerceIn(0.18f, 1f) else 0.12f
                val startOffset = (idx * 0.14f).coerceAtMost(0.45f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(genre.label, style = MaterialTheme.typography.labelSmall, color = MikeDesignTokens.TextSecondary, modifier = Modifier.width(66.dp))
                    Canvas(modifier = Modifier.weight(1f).height(10.dp)) {
                        drawRoundRect(
                            color = MikeDesignTokens.PillButtonSurface,
                            size = size,
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                        val left = size.width * startOffset
                        val barW = (size.width * (1f - startOffset) * ratio).coerceAtLeast(12.dp.toPx())
                        drawRoundRect(
                            color = genreAccentColor(genre),
                            topLeft = Offset(left, 0f),
                            size = Size(barW.coerceAtMost(size.width - left), size.height),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                    }
                }
            }
        }

        if (filteredTracks.isEmpty()) {
            Text(
                text = "Aucun morceau local enregistré pour ce filtre. Les titres écoutés via MediaSession apparaîtront ici automatiquement.",
                style = MaterialTheme.typography.bodySmall,
                color = MikeDesignTokens.TextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredTracks.take(4).forEach { track ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MikeDesignTokens.PillButtonSurface)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.labelLarge, color = MikeDesignTokens.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MikeDesignTokens.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${track.playCount}×", style = MaterialTheme.typography.labelMedium, color = MikeDesignTokens.AccentGreen)
                    }
                }
            }
        }
    }
}

/**
 * Widget #8 JSON (airpods_battery_card) :
 * État matériel réel des écouteurs connectés (AudioDeviceCallback) + arcs verts gauche/droite.
 */
@Composable
fun AudioAccessoryBatteryCardWidget(
    accessory: ConnectedAudioAccessory?,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = accessory != null
    val battery = accessory?.batteryPercent
    val sweepFraction = if (!isConnected) 0.08f else ((battery ?: 100) / 100f).coerceIn(0.15f, 1f)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .testTag("cc_airpods_battery_card")
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .clickable(onClick = onRefreshClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accessory?.name ?: "Écouteurs & Accessoires Audio",
                    style = MaterialTheme.typography.titleMedium,
                    color = MikeDesignTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isConnected) "${accessory!!.deviceTypeLabel} · Connecté" else "Aucun casque Bluetooth ou filaire détecté",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) MikeDesignTokens.AccentGreen else MikeDesignTokens.TextSecondary
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_mike_earbuds),
                contentDescription = "Accessoire audio",
                tint = if (isConnected) MikeDesignTokens.AccentGreen else MikeDesignTokens.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            EarbudArcIndicator(label = "G", progress = sweepFraction, active = isConnected)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = battery?.let { "$it%" } ?: if (isConnected) "ON" else "--",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (isConnected) MikeDesignTokens.TextPrimary else MikeDesignTokens.TextSecondary
                )
                Text(
                    text = if (battery != null) "batterie" else if (isConnected) "Niveau non exposé API 29" else "En attente",
                    style = MaterialTheme.typography.labelSmall,
                    color = MikeDesignTokens.TextSecondary
                )
            }
            EarbudArcIndicator(label = "D", progress = sweepFraction, active = isConnected)
        }
    }
}

@Composable
private fun EarbudArcIndicator(label: String, progress: Float, active: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val stroke = 4.dp.toPx()
            drawArc(
                color = MikeDesignTokens.PillButtonSurface,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke)
            )
            drawArc(
                color = if (active) MikeDesignTokens.AccentGreen else MikeDesignTokens.TextSecondary.copy(alpha = 0.35f),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(label, style = MaterialTheme.typography.titleSmall, color = MikeDesignTokens.TextPrimary)
    }
}

/**
 * Widget #9 JSON (listening_statistics_card) :
 * Courbe linéaire jaune (#FFD60A) + légende Average time.
 */
@Composable
fun ListeningStatisticsCardWidget(
    telemetry: ListeningTelemetryState,
    modifier: Modifier = Modifier
) {
    val buckets = telemetry.hourlyMinutesBucket
    val maxVal = (buckets.maxOrNull() ?: 0).coerceAtLeast(10)

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .testTag("cc_listening_statistics_card")
            .height(155.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Statistic", style = MaterialTheme.typography.titleSmall, color = MikeDesignTokens.TextPrimary)
            Text(
                text = "${telemetry.totalListeningMinutes} min",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MikeDesignTokens.AccentYellow
            )
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(68.dp)) {
            val path = Path()
            val stepX = size.width / (buckets.size - 1).coerceAtLeast(1)
            buckets.forEachIndexed { idx, value ->
                val x = idx * stepX
                val normalized = (value.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 0.95f)
                val y = size.height * (1f - normalized)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = MikeDesignTokens.AccentYellow,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            // Repère du pic horaire
            val peakIndex = buckets.indices.maxByOrNull { buckets[it] } ?: 3
            val peakX = peakIndex * stepX
            val peakNorm = (buckets.getOrElse(peakIndex) { 0 }.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 0.95f)
            val peakY = size.height * (1f - peakNorm)
            drawCircle(color = MikeDesignTokens.AccentYellow, radius = 4.dp.toPx(), center = Offset(peakX, peakY))
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Temps moyen", style = MaterialTheme.typography.labelSmall, color = MikeDesignTokens.TextSecondary)
            Text("0h · 12h · 20h", style = MaterialTheme.typography.labelSmall, color = MikeDesignTokens.TextSecondary)
        }
    }
}

/**
 * Widget #10 JSON (voice_note_waveform_pill) :
 * Forme d'onde bleue #0A84FF + badge temps + Pause/Reprise et Arrêt connectés au moteur MikeEngine.
 */
@Composable
fun VoiceNoteWaveformPillWidget(
    voiceMemo: IslandActivity.VoiceMemo?,
    onToggleVoiceNote: () -> Unit,
    onStopVoiceNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = voiceMemo?.isRecording == true
    val hasActiveSession = voiceMemo != null

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .testTag("cc_voice_note_waveform_pill")
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MikeDesignTokens.CardElevatedSurface)
            .border(0.75.dp, MikeColors.Hairline, RoundedCornerShape(26.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasActiveSession) {
                    AnimatedWaveformBars(
                        color = if (isRecording) MikeDesignTokens.AccentBlue else MikeDesignTokens.TextSecondary,
                        isActive = isRecording,
                        barCount = 6
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mike_voice_memo),
                        contentDescription = "Mémo vocal",
                        tint = MikeDesignTokens.AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = voiceMemo?.compactText ?: "00:00",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MikeDesignTokens.TextPrimary
                    )
                    Text(
                        text = when {
                            isRecording -> "Enregistrement actif"
                            hasActiveSession -> "En pause"
                            else -> "Mémo vocal prêt"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MikeDesignTokens.TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MikeDesignTokens.PillButtonSurface,
                    onClick = onStopVoiceNote,
                    enabled = hasActiveSession,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Arrêter note vocale",
                            tint = if (hasActiveSession) MikeDesignTokens.AccentRed else MikeDesignTokens.TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MikeDesignTokens.AccentBlue,
                    onClick = onToggleVoiceNote,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRecording) "Pause note vocale" else "Lancer note vocale",
                            tint = MikeDesignTokens.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (voiceMemo != null) {
            LinearProgressIndicator(
                progress = { voiceMemo.progress },
                color = MikeDesignTokens.AccentBlue,
                trackColor = MikeDesignTokens.PillButtonSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

private fun genreAccentColor(genre: MusicGenre): Color = when (genre) {
    MusicGenre.POP -> MikeDesignTokens.AccentGreen
    MusicGenre.ROCK -> MikeDesignTokens.AccentRed
    MusicGenre.ELECTRONIC -> MikeDesignTokens.AccentBlue
    MusicGenre.JAZZ -> MikeDesignTokens.AccentYellow
}
