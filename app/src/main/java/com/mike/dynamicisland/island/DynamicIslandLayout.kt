package com.mike.dynamicisland.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.mike.dynamicisland.quickglass.IslandBlueprintMetrics
import com.mike.dynamicisland.quickglass.LightingEffects
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.quickglass.QuickGlassConfig
import com.mike.dynamicisland.settings.IslandSettings
import kotlinx.coroutines.delay

/**
 * Mise en page minimaliste haute fidélité de MIKE Dynamic Island :
 * - Niveau 1 (Mini Island) : pilule noire #000000 épurée (36 dp) avec filet fin 0.75 dp.
 * - Niveau 2 (Smart Island) : carte contextuelle compacte strictement plafonnée (<= 216 dp)
 *   reproduisant avec précision les proportions Apple (Media Player, Appel entrant/actif, Notes vocales, Batterie, Accessoires).
 */
@Composable
fun DynamicIslandLayout(
    state: IslandUiState,
    islandSettings: IslandSettings,
    glassConfig: QuickGlassConfig,
    blueprint: IslandBlueprintMetrics,
    callbacks: IslandInteractionCallbacks,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn() + scaleIn(
            initialScale = 0.88f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut() + scaleOut(targetScale = 0.88f),
        modifier = modifier
    ) {
        val primary = state.primaryActivity ?: return@AnimatedVisibility
        val accent = primary.kind.accentColor

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .widthIn(max = blueprint.expandedMaxWidth)
                .heightIn(max = MikeDesignTokens.MaxFloatingWindowHeightDp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
        ) {
            AnimatedContent(
                targetState = state.displayMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "mike_island_level_transition"
            ) { mode ->
                when (mode) {
                    IslandDisplayMode.EXPANDED -> SmartIslandLevel2Card(
                        activity = primary,
                        secondaryActivity = state.secondaryActivity,
                        blueprint = blueprint,
                        glassConfig = glassConfig,
                        accent = accent,
                        callbacks = callbacks
                    )
                    IslandDisplayMode.COMPACT -> MiniIslandLevel1Pill(
                        activity = primary,
                        settings = islandSettings,
                        blueprint = blueprint,
                        glassConfig = glassConfig,
                        accent = accent,
                        onTap = callbacks.onToggleExpand
                    )
                    IslandDisplayMode.HIDDEN -> Spacer(Modifier.size(0.dp))
                }
            }

            val secondary = state.secondaryActivity
            if (state.displayMode == IslandDisplayMode.COMPACT && secondary != null) {
                SecondaryMiniBubble(
                    activity = secondary,
                    glassConfig = glassConfig,
                    onClick = callbacks.onSwapSecondary
                )
            }
        }
    }
}

@Composable
private fun MiniIslandLevel1Pill(
    activity: IslandActivity,
    settings: IslandSettings,
    blueprint: IslandBlueprintMetrics,
    glassConfig: QuickGlassConfig,
    accent: Color,
    onTap: () -> Unit
) {
    val pillShape = RoundedCornerShape(MikeDesignTokens.BorderRadiusPill)
    val targetMinW = settings.compactWidthDp.dp.coerceIn(blueprint.idlePillWidth, blueprint.compactMaxWidth)
    val targetH = settings.compactHeightDp.dp.coerceIn(blueprint.compactHeight, 42.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .testTag("dynamic_island_compact_pill")
            .widthIn(min = targetMinW, max = blueprint.compactMaxWidth)
            .heightIn(min = targetH, max = 44.dp)
            .clip(pillShape)
            .background(MikeDesignTokens.IslandBackground)
            .border(
                width = 0.75.dp,
                brush = LightingEffects.specularBorderBrush(
                    accentColor = MikeDesignTokens.TextPrimary,
                    intensity = (glassConfig.borderGlowIntensity * 0.55f).coerceIn(0.08f, 0.22f)
                ),
                shape = pillShape
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        MiniLeadingContent(activity = activity, accent = accent)

        Text(
            text = activity.compactText,
            style = MaterialTheme.typography.labelLarge,
            color = MikeDesignTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        MiniTrailingContent(activity = activity, accent = accent)
    }
}

@Composable
private fun MiniLeadingContent(activity: IslandActivity, accent: Color) {
    when (activity) {
        is IslandActivity.Music -> {
            val bmp = activity.artwork
            if (bmp != null && !bmp.isRecycled) {
                val img = remember(bmp) { bmp.asImageBitmap() }
                Image(
                    bitmap = img,
                    contentDescription = "Pochette",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = "Musique", tint = accent, modifier = Modifier.size(17.dp))
            }
        }
        is IslandActivity.SilentMode -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MikeDesignTokens.AccentOrange.copy(alpha = 0.18f))
            ) {
                ActivityVectorGlyph(activity = activity, tint = MikeDesignTokens.AccentOrange, size = 14.dp)
            }
        }
        is IslandActivity.VoiceMemo -> {
            ActivityVectorGlyph(
                activity = activity,
                tint = if (activity.isRecording) MikeDesignTokens.AccentRed else MikeDesignTokens.AccentBlue,
                size = 17.dp
            )
        }
        else -> {
            ActivityVectorGlyph(activity = activity, tint = accent, size = 17.dp)
        }
    }
}

@Composable
private fun MiniTrailingContent(activity: IslandActivity, accent: Color) {
    when (activity) {
        is IslandActivity.Music -> AnimatedWaveformBars(
            color = if (activity.isPlaying) MikeDesignTokens.AccentPurple else MikeDesignTokens.TextSecondary,
            isActive = activity.isPlaying,
            barCount = 4
        )
        is IslandActivity.Call -> AnimatedWaveformBars(
            color = if (activity.isEnded) MikeDesignTokens.AccentRed else MikeDesignTokens.AccentGreen,
            isActive = !activity.isEnded && !activity.isIncoming,
            barCount = 4
        )
        is IslandActivity.VoiceMemo -> AnimatedWaveformBars(
            color = if (activity.isRecording) MikeDesignTokens.AccentRed else MikeDesignTokens.AccentBlue,
            isActive = activity.isRecording,
            barCount = 4
        )
        is IslandActivity.Battery -> {
            val c = when (activity.eventType) {
                BatteryEventType.BATTERY_LOW -> MikeDesignTokens.AccentRed
                BatteryEventType.CHARGER_DISCONNECTED -> MikeDesignTokens.AccentOrange
                else -> MikeDesignTokens.AccentGreen
            }
            MinimalBatteryCapsule(levelPercent = activity.levelPercent, color = c)
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun SecondaryMiniBubble(
    activity: IslandActivity,
    glassConfig: QuickGlassConfig,
    onClick: () -> Unit
) {
    val accent = activity.kind.accentColor
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .testTag("dynamic_island_secondary_bubble")
            .size(36.dp)
            .clip(CircleShape)
            .background(MikeDesignTokens.IslandBackground)
            .border(
                width = 0.75.dp,
                brush = LightingEffects.specularBorderBrush(
                    accentColor = MikeDesignTokens.TextPrimary,
                    intensity = (glassConfig.borderGlowIntensity * 0.55f).coerceIn(0.08f, 0.22f)
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        ActivityVectorGlyph(activity = activity, tint = accent, size = 17.dp)
    }
}

@Composable
private fun SmartIslandLevel2Card(
    activity: IslandActivity,
    secondaryActivity: IslandActivity?,
    blueprint: IslandBlueprintMetrics,
    glassConfig: QuickGlassConfig,
    accent: Color,
    callbacks: IslandInteractionCallbacks
) {
    if (activity is IslandActivity.Call && activity.isIncoming && !activity.isEnded) {
        IncomingCallBlueprintBanner(
            call = activity,
            blueprint = blueprint,
            glassConfig = glassConfig,
            callbacks = callbacks
        )
        return
    }

    if (activity is IslandActivity.Music) {
        ExpandedMediaBlueprintCard(
            music = activity,
            secondaryActivity = secondaryActivity,
            blueprint = blueprint,
            glassConfig = glassConfig,
            callbacks = callbacks
        )
        return
    }

    val cardShape = RoundedCornerShape(MikeDesignTokens.BorderRadiusCardLg)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .testTag("dynamic_island_expanded_card")
            .widthIn(min = 280.dp, max = blueprint.expandedMaxWidth)
            .heightIn(max = MikeDesignTokens.MaxFloatingWindowHeightDp)
            .clip(cardShape)
            .background(MikeDesignTokens.IslandBackground)
            .border(
                width = 0.75.dp,
                brush = LightingEffects.specularBorderBrush(
                    accentColor = MikeDesignTokens.TextPrimary,
                    intensity = (glassConfig.borderGlowIntensity * 0.65f).coerceIn(0.10f, 0.24f)
                ),
                shape = cardShape
            )
            .clickable(onClick = callbacks.onCollapse)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f))
                ) {
                    ActivityVectorGlyph(activity = activity, tint = accent, size = 15.dp)
                }
                Text(
                    text = activity.kind.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MikeDesignTokens.TextSecondary
                )
            }

            if (secondaryActivity != null) {
                Surface(
                    color = MikeDesignTokens.PillButtonSurface,
                    shape = RoundedCornerShape(50),
                    onClick = callbacks.onSwapSecondary,
                    modifier = Modifier.testTag("island_switch_secondary_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        ActivityVectorGlyph(
                            activity = secondaryActivity,
                            tint = secondaryActivity.kind.accentColor,
                            size = 13.dp
                        )
                        Text(
                            text = secondaryActivity.compactText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MikeDesignTokens.TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        when (activity) {
            is IslandActivity.Timer -> SmartTimerBody(activity, callbacks)
            is IslandActivity.Stopwatch -> SmartStopwatchBody(activity, callbacks)
            is IslandActivity.Alarm -> SmartAlarmBody(activity, callbacks)
            is IslandActivity.Call -> SmartActiveOrEndedCallBody(activity, callbacks)
            is IslandActivity.Navigation -> SmartNavigationBody(activity)
            is IslandActivity.Notification -> SmartNotificationBody(activity, callbacks)
            is IslandActivity.Battery -> SmartBatteryBody(activity)
            is IslandActivity.Download -> SmartDownloadBody(activity)
            is IslandActivity.VoiceMemo -> SmartVoiceMemoBody(activity, callbacks)
            is IslandActivity.SilentMode -> SmartStatusSummaryBody(
                headline = activity.ringerState.label,
                detail = "Profil audio Android",
                accent = MikeDesignTokens.AccentOrange
            )
            is IslandActivity.DoNotDisturb -> SmartStatusSummaryBody(
                headline = if (activity.isActive) "Ne pas déranger activé" else "Ne pas déranger désactivé",
                detail = "Filtre d'interruptions système",
                accent = MikeDesignTokens.AccentPurple
            )
            is IslandActivity.AccessoryConnected -> SmartStatusSummaryBody(
                headline = activity.deviceName,
                detail = activity.batteryPercent?.let { "Connecté · $it%" }
                    ?: if (activity.isBluetooth) "Écouteurs Bluetooth connectés" else "Sortie audio filaire connectée",
                accent = MikeDesignTokens.AccentGreen
            )
            is IslandActivity.FaceIdIndicator -> SmartStatusSummaryBody(
                headline = activity.statusLabel,
                detail = if (activity.isVerified) "Déverrouillage Keyguard confirmé" else "Analyse autour de la caméra",
                accent = if (activity.isVerified) MikeDesignTokens.AccentGreen else MikeDesignTokens.AccentBlue
            )
            is IslandActivity.HomeEvent -> SmartStatusSummaryBody(
                headline = activity.deviceTitle,
                detail = activity.statusText,
                accent = MikeDesignTokens.AccentGreen
            )
            is IslandActivity.Music -> Unit
        }
    }
}

@Composable
private fun IncomingCallBlueprintBanner(
    call: IslandActivity.Call,
    blueprint: IslandBlueprintMetrics,
    glassConfig: QuickGlassConfig,
    callbacks: IslandInteractionCallbacks
) {
    val shape = RoundedCornerShape(MikeDesignTokens.BorderRadiusCardLg)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .testTag("dynamic_island_incoming_call_banner")
            .widthIn(min = 290.dp, max = blueprint.expandedMaxWidth)
            .heightIn(min = blueprint.expandedCallHeight, max = 86.dp)
            .clip(shape)
            .background(MikeDesignTokens.IslandBackground)
            .border(
                width = 0.75.dp,
                brush = LightingEffects.specularBorderBrush(
                    accentColor = MikeDesignTokens.TextPrimary,
                    intensity = (glassConfig.borderGlowIntensity * 0.65f).coerceIn(0.10f, 0.24f)
                ),
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(blueprint.contactAvatarSize)
                .clip(CircleShape)
                .background(MikeDesignTokens.PillButtonSurface)
        ) {
            Text(
                text = call.callerName.take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleLarge,
                color = MikeDesignTokens.TextPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.statusText.ifBlank { "mobile" },
                style = MaterialTheme.typography.labelSmall,
                color = MikeDesignTokens.TextSecondary,
                maxLines = 1
            )
            Text(
                text = call.callerName,
                style = MaterialTheme.typography.titleMedium,
                color = MikeDesignTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val notifKey = call.notificationKey
        Surface(
            shape = CircleShape,
            color = MikeDesignTokens.AccentRed,
            onClick = {
                if (notifKey != null && call.actions.size >= 2) {
                    callbacks.onNotificationAction(notifKey, call.actions.last().index)
                } else if (notifKey != null) {
                    callbacks.onNotificationDismiss(notifKey)
                }
            },
            modifier = Modifier
                .size(blueprint.callActionButtonSize)
                .testTag("island_call_decline_btn")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mike_call_decline),
                    contentDescription = "Refuser l'appel",
                    tint = MikeDesignTokens.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MikeDesignTokens.AccentGreen,
            onClick = {
                if (notifKey != null && call.actions.isNotEmpty()) {
                    callbacks.onNotificationAction(notifKey, call.actions.first().index)
                } else if (notifKey != null) {
                    callbacks.onNotificationOpen(notifKey)
                }
            },
            modifier = Modifier
                .size(blueprint.callActionButtonSize)
                .testTag("island_call_accept_btn")
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (call.isVideoCall) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Accepter l'appel vidéo",
                        tint = MikeDesignTokens.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mike_call_accept),
                        contentDescription = "Accepter l'appel",
                        tint = MikeDesignTokens.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedMediaBlueprintCard(
    music: IslandActivity.Music,
    secondaryActivity: IslandActivity?,
    blueprint: IslandBlueprintMetrics,
    glassConfig: QuickGlassConfig,
    callbacks: IslandInteractionCallbacks
) {
    val shape = RoundedCornerShape(MikeDesignTokens.BorderRadiusCardLg)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .testTag("dynamic_island_expanded_card")
            .widthIn(min = 290.dp, max = blueprint.expandedMaxWidth)
            .heightIn(max = blueprint.expandedMediaHeight)
            .clip(shape)
            .background(MikeDesignTokens.IslandBackground)
            .border(
                width = 0.75.dp,
                brush = LightingEffects.specularBorderBrush(
                    accentColor = MikeDesignTokens.TextPrimary,
                    intensity = (glassConfig.borderGlowIntensity * 0.65f).coerceIn(0.10f, 0.24f)
                ),
                shape = shape
            )
            .clickable(onClick = callbacks.onCollapse)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val bmp = music.artwork
            if (bmp != null && !bmp.isRecycled) {
                val img = remember(bmp) { bmp.asImageBitmap() }
                Image(
                    bitmap = img,
                    contentDescription = "Pochette ${music.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(blueprint.albumArtSize)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(blueprint.albumArtSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MikeDesignTokens.PillButtonSurface)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MikeDesignTokens.TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = music.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MikeDesignTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeDesignTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedWaveformBars(
                color = if (music.isPlaying) MikeDesignTokens.AccentPurple else MikeDesignTokens.TextSecondary,
                isActive = music.isPlaying,
                barCount = 5
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinearProgressIndicator(
                progress = { music.progress },
                color = MikeDesignTokens.TextPrimary,
                trackColor = MikeDesignTokens.PillButtonSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(50))
            )
            if (music.durationMs > 0L) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDuration(music.positionMs / 1000L),
                        style = MaterialTheme.typography.labelSmall,
                        color = MikeDesignTokens.TextSecondary
                    )
                    Text(
                        text = "-${formatDuration(((music.durationMs - music.positionMs).coerceAtLeast(0L)) / 1000L)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MikeDesignTokens.TextSecondary
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = callbacks.onMusicPrevious,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("island_music_prev_btn")
            ) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Précédent", tint = MikeDesignTokens.TextPrimary)
            }
            IconButton(
                onClick = callbacks.onMusicTogglePlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("island_music_play_pause_btn")
            ) {
                Icon(
                    imageVector = if (music.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (music.isPlaying) "Pause" else "Lecture",
                    tint = MikeDesignTokens.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(
                onClick = callbacks.onMusicNext,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("island_music_next_btn")
            ) {
                Icon(Icons.Filled.FastForward, contentDescription = "Suivant", tint = MikeDesignTokens.TextPrimary)
            }
            if (secondaryActivity != null) {
                IconButton(
                    onClick = callbacks.onSwapSecondary,
                    modifier = Modifier.size(44.dp)
                ) {
                    ActivityVectorGlyph(
                        activity = secondaryActivity,
                        tint = secondaryActivity.kind.accentColor,
                        size = 20.dp
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mike_audio_output),
                    contentDescription = "Sortie audio",
                    tint = MikeDesignTokens.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartTimerBody(timer: IslandActivity.Timer, callbacks: IslandInteractionCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = timer.compactText,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                color = if (timer.isFinished) MikeDesignTokens.AccentRed else MikeDesignTokens.AccentOrange
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactActionPill(
                    text = if (timer.isPaused) "Reprendre" else "Pause",
                    icon = if (timer.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    accent = MikeDesignTokens.AccentOrange,
                    onClick = callbacks.onTimerTogglePause,
                    modifier = Modifier.testTag("island_timer_pause_btn")
                )
                CompactActionPill(
                    text = "Arrêt",
                    icon = Icons.Filled.Close,
                    accent = MikeDesignTokens.AccentRed,
                    onClick = callbacks.onTimerCancel,
                    modifier = Modifier.testTag("island_timer_cancel_btn")
                )
            }
        }
        LinearProgressIndicator(
            progress = { timer.progress },
            color = MikeDesignTokens.AccentOrange,
            trackColor = MikeDesignTokens.PillButtonSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@Composable
private fun SmartStopwatchBody(stopwatch: IslandActivity.Stopwatch, callbacks: IslandInteractionCallbacks) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stopwatch.detailedText,
            style = MaterialTheme.typography.headlineMedium,
            color = MikeDesignTokens.TextPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactActionPill(
                text = if (stopwatch.isRunning) "Pause" else "Start",
                icon = if (stopwatch.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                accent = MikeDesignTokens.AccentBlue,
                onClick = callbacks.onStopwatchToggle,
                modifier = Modifier.testTag("island_stopwatch_toggle_btn")
            )
            CompactActionPill(
                text = "Tour",
                icon = Icons.Filled.Flag,
                accent = MikeDesignTokens.TextPrimary,
                onClick = callbacks.onStopwatchLap,
                modifier = Modifier.testTag("island_stopwatch_lap_btn")
            )
            CompactActionPill(
                text = "Reset",
                icon = Icons.Filled.Refresh,
                accent = MikeDesignTokens.AccentRed,
                onClick = callbacks.onStopwatchReset,
                modifier = Modifier.testTag("island_stopwatch_reset_btn")
            )
        }
    }
}

@Composable
private fun SmartAlarmBody(alarm: IslandActivity.Alarm, callbacks: IslandInteractionCallbacks) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(alarm.label, style = MaterialTheme.typography.titleMedium, color = MikeDesignTokens.TextPrimary, maxLines = 1)
            Text(
                text = if (alarm.isRinging) "Sonnerie active" else "Dans ${alarm.compactText}",
                style = MaterialTheme.typography.bodySmall,
                color = MikeDesignTokens.AccentRed
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactActionPill(
                text = "5m",
                icon = Icons.Filled.Snooze,
                accent = MikeDesignTokens.AccentOrange,
                onClick = callbacks.onAlarmSnooze
            )
            CompactActionPill(
                text = "Stop",
                icon = Icons.Filled.Close,
                accent = MikeDesignTokens.AccentRed,
                onClick = callbacks.onAlarmDismiss
            )
        }
    }
}

@Composable
private fun SmartActiveOrEndedCallBody(call: IslandActivity.Call, callbacks: IslandInteractionCallbacks) {
    var elapsedSec by remember(call.id, call.startedAtElapsedMs, call.isEnded) {
        mutableLongStateOf(
            if (!call.isEnded && call.startedAtElapsedMs > 0L) {
                ((System.currentTimeMillis() - call.startedAtElapsedMs) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
        )
    }

    LaunchedEffect(call.id, call.isEnded, call.startedAtElapsedMs) {
        if (!call.isEnded && call.startedAtElapsedMs > 0L) {
            while (true) {
                delay(1000L)
                elapsedSec += 1L
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(call.callerName, style = MaterialTheme.typography.titleMedium, color = MikeDesignTokens.TextPrimary, maxLines = 1)
            val subtitle = when {
                call.isEnded -> "Appel terminé"
                call.startedAtElapsedMs > 0L -> formatDuration(elapsedSec)
                else -> call.statusText
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = if (call.isEnded) MikeDesignTokens.AccentRed else MikeDesignTokens.AccentGreen
            )
        }
        AnimatedWaveformBars(
            color = if (call.isEnded) MikeDesignTokens.TextSecondary else MikeDesignTokens.AccentGreen,
            isActive = !call.isEnded,
            barCount = 5
        )
        val key = call.notificationKey
        if (!call.isEnded && key != null && call.actions.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            CompactActionPill(
                text = call.actions.first().title,
                icon = null,
                accent = MikeDesignTokens.AccentRed,
                onClick = { callbacks.onNotificationAction(key, call.actions.first().index) }
            )
        }
    }
}

@Composable
private fun SmartVoiceMemoBody(memo: IslandActivity.VoiceMemo, callbacks: IslandInteractionCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedWaveformBars(
                    color = if (memo.isRecording) MikeDesignTokens.AccentRed else MikeDesignTokens.AccentBlue,
                    isActive = memo.isRecording,
                    barCount = 5
                )
                Text(
                    text = memo.compactText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MikeDesignTokens.TextPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactActionPill(
                    text = if (memo.isRecording) "Pause" else "Reprendre",
                    icon = if (memo.isRecording) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    accent = MikeDesignTokens.AccentBlue,
                    onClick = callbacks.onVoiceMemoTogglePause,
                    modifier = Modifier.testTag("island_voice_memo_toggle_btn")
                )
                CompactActionPill(
                    text = "Stop",
                    icon = Icons.Filled.Close,
                    accent = MikeDesignTokens.AccentRed,
                    onClick = callbacks.onVoiceMemoStop,
                    modifier = Modifier.testTag("island_voice_memo_stop_btn")
                )
            }
        }
        LinearProgressIndicator(
            progress = { memo.progress },
            color = if (memo.isRecording) MikeDesignTokens.AccentRed else MikeDesignTokens.AccentBlue,
            trackColor = MikeDesignTokens.PillButtonSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@Composable
private fun SmartNavigationBody(nav: IslandActivity.Navigation) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MikeDesignTokens.AccentBlue.copy(alpha = 0.18f))
        ) {
            Icon(Icons.Filled.TurnRight, contentDescription = "Direction", tint = MikeDesignTokens.TextPrimary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nav.distanceText.ifBlank { nav.title },
                style = MaterialTheme.typography.titleMedium,
                color = MikeDesignTokens.TextPrimary,
                maxLines = 1
            )
            Text(
                text = nav.instruction,
                style = MaterialTheme.typography.bodySmall,
                color = MikeDesignTokens.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmartNotificationBody(notif: IslandActivity.Notification, callbacks: IslandInteractionCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = MikeDesignTokens.TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
            if (notif.groupedCount > 1) {
                Text("+${notif.groupedCount}", style = MaterialTheme.typography.labelSmall, color = MikeDesignTokens.TextSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (notif.isContentMasked) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MikeDesignTokens.TextSecondary, modifier = Modifier.size(13.dp))
            }
            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MikeDesignTokens.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactActionPill("Ouvrir", null, MikeDesignTokens.AccentBlue, { callbacks.onNotificationOpen(notif.notificationKey) })
            notif.actions.firstOrNull()?.let { action ->
                CompactActionPill(action.title, null, MikeDesignTokens.TextPrimary, { callbacks.onNotificationAction(notif.notificationKey, action.index) })
            }
            CompactActionPill("Ignorer", Icons.Filled.Close, MikeDesignTokens.TextSecondary, { callbacks.onNotificationDismiss(notif.notificationKey) })
        }
    }
}

@Composable
private fun SmartBatteryBody(battery: IslandActivity.Battery) {
    val barColor = when (battery.eventType) {
        BatteryEventType.BATTERY_LOW -> MikeDesignTokens.AccentRed
        BatteryEventType.CHARGER_DISCONNECTED -> MikeDesignTokens.AccentOrange
        else -> MikeDesignTokens.AccentGreen
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(battery.eventType.label, style = MaterialTheme.typography.titleSmall, color = MikeDesignTokens.TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MinimalBatteryCapsule(levelPercent = battery.levelPercent, color = barColor)
                Text("${battery.levelPercent}%", style = MaterialTheme.typography.titleMedium, color = barColor)
            }
        }
        LinearProgressIndicator(
            progress = { (battery.levelPercent / 100f).coerceIn(0f, 1f) },
            color = barColor,
            trackColor = MikeDesignTokens.PillButtonSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@Composable
private fun SmartDownloadBody(download: IslandActivity.Download) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(download.title, style = MaterialTheme.typography.titleSmall, color = MikeDesignTokens.TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
            Text(download.compactText, style = MaterialTheme.typography.labelMedium, color = MikeDesignTokens.AccentBlue)
        }
        if (download.isIndeterminate) {
            LinearProgressIndicator(color = MikeDesignTokens.AccentBlue, trackColor = MikeDesignTokens.PillButtonSurface, modifier = Modifier.fillMaxWidth().height(3.5.dp))
        } else {
            LinearProgressIndicator(progress = { (download.progressPercent / 100f).coerceIn(0f, 1f) }, color = MikeDesignTokens.AccentBlue, trackColor = MikeDesignTokens.PillButtonSurface, modifier = Modifier.fillMaxWidth().height(3.5.dp))
        }
    }
}

@Composable
private fun SmartStatusSummaryBody(headline: String, detail: String, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(headline, style = MaterialTheme.typography.titleMedium, color = MikeDesignTokens.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
