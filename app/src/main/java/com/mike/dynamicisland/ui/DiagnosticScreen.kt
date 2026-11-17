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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.quickglass.IslandBlueprintMetrics
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface
import com.mike.dynamicisland.utils.DisplayCutoutManager

/**
 * Écran Diagnostic matériel, mémoire et banc de test réel (Modules 11 & 12).
 */
@Composable
fun DiagnosticScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val memory by viewModel.memorySnapshot.collectAsState()
    val faceState by viewModel.faceUnlockState.collectAsState()
    val logs by viewModel.eventLogs.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Diagnostic & Tests",
                subtitle = "Profil matériel détecté, mémoire réelle & journal d'événements",
                badgeText = "API ${profile.androidVersion}",
                badgeColor = MikeColors.Blue,
                onBack = onBack
            )
        }

        // Profil matériel réel (Module 11)
        item {
            QuickGlassSurface(config = effectiveGlass) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MikeColors.White)
                    Text("DeviceProfile réel", style = MaterialTheme.typography.titleMedium, color = MikeColors.White)
                }
                Spacer(Modifier.height(10.dp))
                val cutoutZone = DisplayCutoutManager.detectZone(profile)
                val blueprint = IslandBlueprintMetrics.forScreenWidthDp(profile.screenWidthDp)
                DiagnosticKeyValue("Constructeur / Modèle", "${profile.manufacturer} ${profile.model}".trim().ifBlank { "Inconnu" })
                DiagnosticKeyValue("Android SDK", "API ${profile.androidVersion}")
                DiagnosticKeyValue("RAM totale / dispo", "${profile.totalRamMb} Mo / ${profile.availableRamMb} Mo")
                DiagnosticKeyValue("Stockage libre", "${profile.freeStorageMb} Mo")
                DiagnosticKeyValue("Écran", "${profile.screenWidth} × ${profile.screenHeight} px @ ${profile.refreshRate.toInt()} Hz")
                DiagnosticKeyValue("Dalle", profile.displayPanelNote)
                DiagnosticKeyValue("Zone Caméra", cutoutZone.label)
                DiagnosticKeyValue("Blueprint 390pt → dp", "${profile.screenWidthDp.toInt()} dp (×${String.format("%.2f", blueprint.scaleFactor)})")
                DiagnosticKeyValue("Profil perf", profile.performanceTier.label)
                DiagnosticKeyValue("Particules max", "${profile.maxParticleCount} (60 Hz safe)")
                DiagnosticKeyValue("Flou RenderEffect", if (profile.supportsHardwareBlur) "Supporté (API 31+)" else "Désactivé (verre stratifié Android 10)")
                DiagnosticKeyValue("Mode Face ID", faceState.capability.operatingModeLabel)
            }
        }

        // Audit Architectural V7 (Règle Absolue Partie 1 & Limites Android 10 sans root)
        item {
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Green) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MikeColors.Green)
                    Text("Audit V7 · Fenêtre compacte & API", style = MaterialTheme.typography.titleMedium, color = MikeColors.White)
                }
                Spacer(Modifier.height(8.dp))
                DiagnosticKeyValue("Plafond Face ID", "≤ ${MikeDesignTokens.MaxFaceWindowSizeDp.value.toInt()} dp (jamais plein écran)")
                DiagnosticKeyValue("Plafond Smart Island", "≤ ${MikeDesignTokens.MaxFloatingWindowHeightDp.value.toInt()} dp (jamais MATCH_PARENT)")
                DiagnosticKeyValue("FLAG_LAYOUT_NO_LIMITS", "Interdit / Retiré (barre d'état intacte)")
                DiagnosticKeyValue("Widgets Source #1", "Isolés dans Control Center (Niveau 3)")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Transparence Android 10 (sans root) : la différenciation biométrique Face vs PIN HiOS, la batterie propriétaire par écouteur AirPods (W1/H1) et AirDrop/AirPlay/FaceTime/HomeKit ne sont pas exposés par les API publiques Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeColors.Secondary
                )
            }
        }

        // Consommation mémoire réelle
        item {
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Green) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Memory, contentDescription = null, tint = MikeColors.Green)
                        Text("Mémoire temps réel", style = MaterialTheme.typography.titleMedium, color = MikeColors.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.refreshDiagnosticsAndPermissions() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("diag_refresh_memory_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser", tint = MikeColors.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                DiagnosticKeyValue("Heap JVM app", String.format("%.1f / %.0f Mo", memory.appUsedHeapMb, memory.appMaxHeapMb))
                DiagnosticKeyValue("Heap natif", String.format("%.1f Mo", memory.nativeHeapMb))
                DiagnosticKeyValue("RAM dispo système", "${memory.systemAvailableRamMb} / ${memory.systemTotalRamMb} Mo")
                DiagnosticKeyValue("Alerte mémoire faible", if (memory.isSystemLowMemory) "OUI" else "Non")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { memory.heapUsageRatio },
                    color = if (memory.heapUsageRatio > 0.75f) MikeColors.Coral else MikeColors.Green,
                    trackColor = MikeColors.SurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }

        // Banc de vérification rapide Module 12
        item {
            MikeSectionLabel("Vérification interactive (Module 12)")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.triggerFaceIdTest(simulateInterrupt = false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MikeDesignTokens.ActiveSurfaceLight,
                            contentColor = MikeDesignTokens.IslandBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("diag_test_face_btn")
                    ) {
                        Text("Test Face ID")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.startTimer(180L, "Minuteur test")
                            viewModel.startOrToggleStopwatch()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("diag_test_multi_btn")
                    ) {
                        Text("Multi-activités", color = MikeColors.White)
                    }
                }
            }
        }

        // Journal d'événements horodaté (RÉEL vs TEST)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MikeSectionLabel("Journal d'événements (${logs.size})")
                OutlinedButton(
                    onClick = { viewModel.clearLogs() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Vider", tint = MikeColors.Secondary)
                }
            }
        }

        item {
            QuickGlassSurface(config = effectiveGlass) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Aucun événement enregistré pour le moment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MikeColors.Secondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        logs.take(30).forEach { entry ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = entry.timestampText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MikeColors.Secondary
                                )
                                MikeStatusBadge(
                                    text = if (entry.isRealSystemEvent) "RÉEL" else "TEST",
                                    color = if (entry.isRealSystemEvent) MikeColors.Green else MikeColors.Amber
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${entry.category} · ${entry.message}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MikeColors.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DiagnosticKeyValue(key: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(text = key, style = MaterialTheme.typography.bodySmall, color = MikeColors.Secondary)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
            color = MikeColors.White
        )
    }
}
