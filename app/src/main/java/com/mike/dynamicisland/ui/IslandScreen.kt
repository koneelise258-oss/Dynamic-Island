package com.mike.dynamicisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.island.ActivityKind
import com.mike.dynamicisland.island.DynamicIslandHost
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface
import com.mike.dynamicisland.settings.IslandAlignment
import kotlin.math.roundToInt

/**
 * Centre de configuration de la Dynamic Island (Module 02 & Module 09).
 */
@Composable
fun IslandScreen(
    viewModel: MikeViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val islandSettings = settings.island
    val islandState by viewModel.islandState.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Dynamic Island",
                subtitle = "Mini Island, Smart Island & multi-activités · Jamais plein écran",
                badgeText = "${islandState.allActivities.size} active(s)",
                badgeColor = if (islandState.isVisible) MikeColors.Green else MikeColors.Secondary
            )
        }

        // Aperçu interactif temps réel de la Dynamic Island
        item {
            QuickGlassSurface(
                config = effectiveGlass,
                contentPadding = 16.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 122.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(0.75.dp, MikeColors.Hairline, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    if (islandState.isVisible) {
                        DynamicIslandHost(
                            state = islandState,
                            islandSettings = islandSettings,
                            glassConfig = effectiveGlass,
                            callbacks = viewModel.engine.islandCallbacks
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Îlot automatiquement masqué",
                                style = MaterialTheme.typography.titleSmall,
                                color = MikeColors.White
                            )
                            Text(
                                text = "Démarrez un minuteur ou un chronomètre ci-dessous pour tester le mode compact, développé et multi-activités.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MikeColors.Secondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.startTimer(272L, "Minuteur") },
                        colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Amber),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("island_launch_timer_btn")
                    ) {
                        Text("Minuteur", color = MikeColors.Background, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { viewModel.startOrToggleStopwatch() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("island_launch_stopwatch_btn")
                    ) {
                        Text("Chrono", color = MikeColors.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { viewModel.engine.dynamicIslandEngine.toggleExpanded() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("island_toggle_expand_btn")
                    ) {
                        Text("Développer", color = MikeColors.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Accès direct aux pages Activités, Notifications et Musique
        item {
            QuickGlassSurface(config = effectiveGlass, contentPadding = 10.dp) {
                MikeNavigationTile(
                    title = "Activités Now Bar",
                    subtitle = "Contrôler Minuteur, Chronomètre et Alarme",
                    icon = Icons.Filled.Timer,
                    accent = MikeColors.Amber,
                    testTag = "island_goto_activities",
                    onClick = { onNavigate(MikeRoutes.ACTIVITIES) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Applications autorisées (Notifications)",
                    subtitle = "${islandSettings.allowedNotificationPackages.size} apps filtrées · WhatsApp, Gmail, Telegram",
                    icon = Icons.Filled.Notifications,
                    accent = MikeColors.Green,
                    testTag = "island_goto_notifications",
                    onClick = { onNavigate(MikeRoutes.NOTIFICATIONS) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Expérience Musique (MediaSession)",
                    subtitle = "Contrôles lecture, pochette et piste active",
                    icon = Icons.Filled.MusicNote,
                    accent = MikeColors.Purple,
                    testTag = "island_goto_music",
                    onClick = { onNavigate(MikeRoutes.MUSIC) }
                )
            }
        }

        // Position, dimensions et expansion
        item {
            MikeSectionLabel("Position, dimensions & interaction")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                Text("Alignement supérieur", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IslandAlignment.entries.forEach { alignment ->
                        val selected = islandSettings.alignment == alignment
                        Surface(
                            onClick = { viewModel.updateIslandSettings { it.copy(alignment = alignment) } },
                            color = if (selected) MikeColors.Blue.copy(alpha = 0.24f) else MikeColors.SurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MikeColors.Blue else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = alignment.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MikeColors.White else MikeColors.Secondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                MikeSliderRow(
                    title = "Largeur compacte",
                    valueLabel = "${islandSettings.compactWidthDp.roundToInt()} dp",
                    value = islandSettings.compactWidthDp,
                    valueRange = 136f..240f,
                    onValueChange = { v -> viewModel.updateIslandSettings { it.copy(compactWidthDp = v) } },
                    accent = MikeColors.Cyan
                )
                MikeSliderRow(
                    title = "Hauteur compacte",
                    valueLabel = "${islandSettings.compactHeightDp.roundToInt()} dp",
                    value = islandSettings.compactHeightDp,
                    valueRange = 34f..48f,
                    onValueChange = { v -> viewModel.updateIslandSettings { it.copy(compactHeightDp = v) } },
                    accent = MikeColors.Cyan
                )
                MikeSliderRow(
                    title = "Décalage vertical",
                    valueLabel = "${islandSettings.verticalOffsetDp.roundToInt()} dp",
                    value = islandSettings.verticalOffsetDp,
                    valueRange = 0f..48f,
                    onValueChange = { v -> viewModel.updateIslandSettings { it.copy(verticalOffsetDp = v) } },
                    accent = MikeColors.Blue
                )
                MikeSliderRow(
                    title = "Réduction automatique",
                    valueLabel = "${islandSettings.autoCollapseSeconds} s",
                    value = islandSettings.autoCollapseSeconds.toFloat(),
                    valueRange = 4f..20f,
                    onValueChange = { v -> viewModel.updateIslandSettings { it.copy(autoCollapseSeconds = v.roundToInt()) } },
                    accent = MikeColors.Blue
                )

                MikeToggleRow(
                    title = "Expansion au toucher",
                    subtitle = "Affiche les commandes contextuelles en touchant la pilule",
                    checked = islandSettings.tapToExpandEnabled,
                    onCheckedChange = { v -> viewModel.updateIslandSettings { it.copy(tapToExpandEnabled = v) } },
                    icon = Icons.Filled.TouchApp,
                    accent = MikeColors.Blue,
                    testTag = "island_tap_expand_switch"
                )
                MikeDivider()
                MikeToggleRow(
                    title = "Multi-activités (bulle secondaire)",
                    subtitle = "Garde la musique ou une seconde activité accessible",
                    checked = islandSettings.allowMultipleActivities,
                    onCheckedChange = { v -> viewModel.updateIslandSettings { it.copy(allowMultipleActivities = v) } },
                    icon = Icons.Filled.Layers,
                    accent = MikeColors.Purple,
                    testTag = "island_multi_activity_switch"
                )
            }
        }

        // Sélection des activités contextuelles
        item {
            MikeSectionLabel("Activités contextuelles (${ActivityKind.entries.size})")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                ActivityKind.entries.forEachIndexed { idx, kind ->
                    val enabled = kind in islandSettings.enabledActivities
                    MikeToggleRow(
                        title = kind.title,
                        subtitle = if (enabled) "Visible dans la Dynamic Island lorsque actif" else "Désactivé",
                        checked = enabled,
                        onCheckedChange = { viewModel.toggleActivityEnabled(kind, it) },
                        accent = kind.accentColor,
                        testTag = "island_activity_toggle_${kind.name.lowercase()}"
                    )
                    if (idx < ActivityKind.entries.lastIndex) {
                        MikeDivider()
                    }
                }
            }
        }

        // Ordre de priorité personnalisable
        item {
            MikeSectionLabel("Ordre de priorité multi-activités")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                Text(
                    text = "Règle de sécurité : un appel actif conserve toujours la priorité absolue sur une notification ordinaire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeColors.Secondary
                )
                Spacer(Modifier.height(8.dp))
                islandSettings.priorityOrder.forEachIndexed { index, kind ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = kind.accentColor
                            )
                            Text(
                                text = kind.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MikeColors.White
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { viewModel.moveActivityPriority(kind, moveUp = true) },
                                enabled = index > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowUpward,
                                    contentDescription = "Monter ${kind.title}",
                                    tint = if (index > 0) MikeColors.White else MikeColors.Secondary.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveActivityPriority(kind, moveUp = false) },
                                enabled = index < islandSettings.priorityOrder.lastIndex,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = "Descendre ${kind.title}",
                                    tint = if (index < islandSettings.priorityOrder.lastIndex) MikeColors.White else MikeColors.Secondary.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
