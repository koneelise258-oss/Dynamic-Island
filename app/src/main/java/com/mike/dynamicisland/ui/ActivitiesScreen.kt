package com.mike.dynamicisland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.island.formatDuration
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface

/**
 * Page Activités Samsung Now Bar (Module 04 & Module 09).
 */
@Composable
fun ActivitiesScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    val timer by viewModel.timerState.collectAsState()
    val stopwatch by viewModel.stopwatchState.collectAsState()
    val alarm by viewModel.alarmState.collectAsState()
    val battery by viewModel.batteryTelemetry.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Activités Now Bar",
                subtitle = "Minuteur, Chronomètre, Alarme et flux contextuels en direct",
                onBack = onBack
            )
        }

        // Minuteur réel
        item {
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Amber) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = MikeColors.Amber)
                        Text("Minuteur", style = MaterialTheme.typography.titleMedium, color = MikeColors.White)
                    }
                    Text(
                        text = timer?.compactText ?: "Inactif",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (timer != null) MikeColors.Amber else MikeColors.Secondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (timer == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(60L to "1 min", 180L to "3 min", 300L to "5 min", 600L to "10 min").forEach { (sec, label) ->
                            OutlinedButton(
                                onClick = { viewModel.startTimer(sec, "Minuteur $label") },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("activity_timer_${sec}s")
                            ) {
                                Text(label, color = MikeColors.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.engine.islandCallbacks.onTimerTogglePause() },
                            colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Amber),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("activity_timer_pause_btn")
                        ) {
                            Text(if (timer!!.isPaused) "Reprendre" else "Pause", color = MikeColors.Background)
                        }
                        OutlinedButton(
                            onClick = { viewModel.engine.islandCallbacks.onTimerAddMinute() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("+1m", color = MikeColors.White)
                        }
                        OutlinedButton(
                            onClick = { viewModel.engine.islandCallbacks.onTimerCancel() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("activity_timer_cancel_btn")
                        ) {
                            Text("Annuler", color = MikeColors.Coral)
                        }
                    }
                }
            }
        }

        // Chronomètre réel
        item {
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Blue) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Timer, contentDescription = null, tint = MikeColors.Blue)
                        Text("Chronomètre", style = MaterialTheme.typography.titleMedium, color = MikeColors.White)
                    }
                    Text(
                        text = stopwatch?.detailedText ?: "00:00.0",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (stopwatch != null) MikeColors.Blue else MikeColors.Secondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.startOrToggleStopwatch() },
                        colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Blue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("activity_stopwatch_toggle_btn")
                    ) {
                        Text(if (stopwatch?.isRunning == true) "Pause" else "Démarrer")
                    }
                    OutlinedButton(
                        onClick = { viewModel.lapStopwatch() },
                        enabled = stopwatch != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("activity_stopwatch_lap_btn")
                    ) {
                        Text("Tour", color = MikeColors.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetStopwatch() },
                        enabled = stopwatch != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("activity_stopwatch_reset_btn")
                    ) {
                        Text("Reset", color = MikeColors.Coral)
                    }
                }
                val laps = stopwatch?.laps.orEmpty()
                if (laps.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        laps.take(4).forEachIndexed { idx, lapMs ->
                            Text(
                                text = "Tour #${laps.size - idx} · ${formatDuration(lapMs / 1000L)}.${(lapMs % 1000L) / 100L}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MikeColors.Secondary
                            )
                        }
                    }
                }
            }
        }

        // Alarme contextuelle
        item {
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Coral) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Alarm, contentDescription = null, tint = MikeColors.Coral)
                        Text("Alarme locale", style = MaterialTheme.typography.titleMedium, color = MikeColors.White)
                    }
                    Text(
                        text = alarm?.compactText ?: "Aucune",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (alarm != null) MikeColors.Coral else MikeColors.Secondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (alarm == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.scheduleAlarm(60L, "Rappel 1 min") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Dans 1 min", color = MikeColors.White)
                        }
                        Button(
                            onClick = { viewModel.triggerAlarmTest() },
                            colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Coral),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("activity_alarm_test_btn")
                        ) {
                            Text("Déclencher", color = MikeColors.White)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.engine.islandCallbacks.onAlarmSnooze() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Répéter 5m", color = MikeColors.Amber)
                        }
                        Button(
                            onClick = { viewModel.engine.islandCallbacks.onAlarmDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Coral),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Arrêter", color = MikeColors.White)
                        }
                    }
                }
            }
        }

        // Activités système automatiques (Batterie, Navigation, Téléchargements)
        item {
            MikeSectionLabel("Sources système automatiques")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_mike_battery_bolt),
                        contentDescription = null,
                        tint = MikeColors.Green
                    )
                    Column {
                        Text("Batterie (${battery.levelPercent}%)", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                        Text(
                            text = if (battery.isCharging) "En charge · Chargeur détecté automatiquement" else "Sur batterie · Réagit aux événements système Android",
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                MikeDivider()
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, tint = MikeColors.Blue)
                    Column {
                        Text("Navigation (Google Maps / Waze)", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                        Text(
                            text = "Détectée automatiquement via NotificationListenerService lors d'un itinéraire actif.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                MikeDivider()
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = MikeColors.Blue)
                    Column {
                        Text("Téléchargements en cours", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                        Text(
                            text = "Affiche la progression réelle lorsque le gestionnaire système publie EXTRA_PROGRESS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
