package com.mike.dynamicisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.faceid.FacePositionMode
import com.mike.dynamicisland.faceid.FaceUnlockPhase
import com.mike.dynamicisland.faceid.StyledFaceScanCanvas
import com.mike.dynamicisland.faceid.rememberFaceAnimationController
import com.mike.dynamicisland.island.DynamicIslandHost
import com.mike.dynamicisland.island.MinimalBatteryCapsule
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface

@Composable
fun HomeScreen(
    viewModel: MikeViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val serviceRunning by viewModel.isOverlayServiceRunning.collectAsState()
    val faceState by viewModel.faceUnlockState.collectAsState()
    val islandState by viewModel.islandState.collectAsState()
    val battery by viewModel.batteryTelemetry.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    val faceController = rememberFaceAnimationController(
        unlockState = faceState,
        settings = settings.faceId,
        onCompleted = { viewModel.dismissFaceIdAnimation() }
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // En-tête minimaliste MIKE
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MikeDesignTokens.CardElevatedSurface)
                            .border(0.75.dp, MikeColors.Hairline, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mike_face_id),
                            contentDescription = "Mike Dynamic Island",
                            tint = MikeColors.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Mike Dynamic Island",
                            style = MaterialTheme.typography.titleLarge,
                            color = MikeColors.White
                        )
                        Text(
                            text = "Face ID · Dynamic Island · One UI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
                MikeStatusBadge(
                    text = if (serviceRunning) "Actif" else "Veille",
                    color = if (serviceRunning) MikeColors.Green else MikeColors.Secondary
                )
            }
        }

        // Carte principale : Interrupteur moteur flottant + scène Punch-Hole OLED en direct
        item {
            QuickGlassSurface(
                config = effectiveGlass,
                contentPadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Moteur flottant",
                            style = MaterialTheme.typography.titleMedium,
                            color = MikeColors.White
                        )
                        Text(
                            text = when {
                                serviceRunning -> "Fenêtre compacte active · Écran et Keyguard synchronisés"
                                !permissions.overlayGranted -> "Autorisez la superposition pour l'affichage système"
                                else -> "Prêt à démarrer en arrière-plan léger"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = serviceRunning,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable && !permissions.overlayGranted) {
                                onNavigate(MikeRoutes.PERMISSIONS)
                            } else {
                                viewModel.toggleOverlayService(wantEnable)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MikeColors.White,
                            checkedTrackColor = MikeColors.Green,
                            uncheckedThumbColor = MikeColors.White.copy(alpha = 0.72f),
                            uncheckedTrackColor = MikeColors.SurfaceElevated,
                            uncheckedBorderColor = MikeColors.OutlineSubtle
                        ),
                        modifier = Modifier.testTag("home_master_service_switch")
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Scène épurée noir pur (#000000) : Caméra Punch-Hole + Face ID + Dynamic Island réelle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 144.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MikeDesignTokens.IslandBackground)
                        .border(0.75.dp, MikeColors.Hairline, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    val isLeftPunchHole = settings.faceId.positionMode == FacePositionMode.TOP_LEFT_PUNCH_HOLE ||
                        (settings.faceId.positionMode == FacePositionMode.AUTO_CUTOUT && profile.isTecnoOrHiOsDevice)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(if (isLeftPunchHole) Alignment.TopStart else Alignment.TopCenter)
                            .size(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0E0E10))
                                .border(1.dp, Color(0xFF262629), CircleShape)
                        )
                        val showActive = faceController.isVisible || faceState.phase == FaceUnlockPhase.SCANNING
                        StyledFaceScanCanvas(
                            style = settings.faceId.style,
                            progress = if (showActive) faceController.progress.value else 0.72f,
                            success = faceController.isSuccess,
                            interrupted = faceController.isInterrupted,
                            runContinuousMotion = showActive && !faceController.isSuccess,
                            accentColor = settings.faceId.accentColor,
                            successColor = settings.faceId.successColor,
                            sizeDp = 66.dp,
                            strokeWidthDp = settings.faceId.strokeWidthDp.dp,
                            speedMultiplier = settings.faceId.speedMultiplier,
                            maxParticleCount = profile.maxParticleCount
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(top = 62.dp)
                    ) {
                        if (islandState.isVisible) {
                            DynamicIslandHost(
                                state = islandState,
                                islandSettings = settings.island,
                                glassConfig = effectiveGlass,
                                callbacks = viewModel.engine.islandCallbacks
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MikeDesignTokens.PillButtonSurface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MikeColors.Secondary)
                                )
                                Text(
                                    text = "Dynamic Island en veille",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MikeColors.Secondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2 actions directes, contraste propre Apple Dark
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.triggerFaceIdTest(simulateInterrupt = false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MikeDesignTokens.ActiveSurfaceLight,
                            contentColor = MikeDesignTokens.IslandBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("home_test_face_id_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mike_face_id),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Tester Face ID",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        onClick = { viewModel.startTimer(272L, "Minuteur") },
                        color = MikeDesignTokens.PillButtonSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .border(0.75.dp, MikeColors.Hairline, RoundedCornerShape(14.dp))
                            .testTag("home_quick_timer_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = MikeColors.Amber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Minuteur 4:32",
                                style = MaterialTheme.typography.labelLarge,
                                color = MikeColors.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Bandeau d'état système compact (3 indicateurs réels sur une seule surface sobre)
        item {
            QuickGlassSurface(
                config = effectiveGlass,
                contentPadding = 12.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactMetricCell(
                        label = "Permissions",
                        value = "${permissions.grantedCount}/${permissions.totalCount}",
                        valueColor = if (permissions.allCoreGranted) MikeColors.Green else MikeColors.Amber,
                        onClick = { onNavigate(MikeRoutes.PERMISSIONS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_permissions_card")
                    )
                    VerticalHairline()
                    CompactMetricCell(
                        label = "Activités",
                        value = "${islandState.allActivities.size}",
                        valueColor = if (islandState.isVisible) MikeColors.White else MikeColors.Secondary,
                        onClick = { onNavigate(MikeRoutes.ACTIVITIES) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_activities_card")
                    )
                    VerticalHairline()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigate(MikeRoutes.DIAGNOSTIC) }
                            .padding(vertical = 4.dp)
                            .testTag("home_battery_metric")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (battery.levelPercent > 0) {
                                MinimalBatteryCapsule(
                                    levelPercent = battery.levelPercent,
                                    color = if (battery.levelPercent <= 20) MikeColors.Coral else MikeColors.Green
                                )
                                Text(
                                    text = "${battery.levelPercent}%",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MikeColors.White
                                )
                            } else {
                                Text(
                                    text = "API ${profile.androidVersion}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MikeColors.White
                                )
                            }
                        }
                        Text(
                            text = "${profile.refreshRate.toInt()} Hz",
                            style = MaterialTheme.typography.labelSmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
            }
        }

        // Accès directs groupés façon Samsung One UI (épuré, sans grille surchargée)
        item {
            MikeSectionLabel("Accès rapide")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(
                config = effectiveGlass,
                contentPadding = 10.dp
            ) {
                MikeNavigationTile(
                    title = "MIKE Control Center",
                    subtitle = "Volume radial, sorties audio, genres et mémo vocal",
                    icon = Icons.Filled.Equalizer,
                    accent = MikeColors.Green,
                    trailingBadge = "Niveau 3",
                    testTag = "shortcut_control_center",
                    onClick = { onNavigate(MikeRoutes.CONTROL_CENTER) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Activités Now Bar",
                    subtitle = "Minuteur, chronomètre, alarme et événements batterie",
                    icon = Icons.Filled.Timer,
                    accent = MikeColors.Amber,
                    testTag = "shortcut_activities",
                    onClick = { onNavigate(MikeRoutes.ACTIVITIES) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Notifications",
                    subtitle = "${settings.island.allowedNotificationPackages.size} apps autorisées · Traitement local",
                    icon = Icons.Filled.Notifications,
                    accent = MikeColors.Blue,
                    testTag = "shortcut_notifications",
                    onClick = { onNavigate(MikeRoutes.NOTIFICATIONS) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Musique & Médias",
                    subtitle = "Lecture MediaSession native et commandes",
                    icon = Icons.Filled.MusicNote,
                    accent = MikeColors.Purple,
                    testTag = "shortcut_music",
                    onClick = { onNavigate(MikeRoutes.MUSIC) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Permissions & HiOS",
                    subtitle = if (permissions.allCoreGranted) "Autorisations principales actives" else "Superposition et accès requis",
                    icon = Icons.Filled.Security,
                    accent = if (permissions.allCoreGranted) MikeColors.Green else MikeColors.Amber,
                    trailingBadge = "${permissions.grantedCount}/${permissions.totalCount}",
                    testTag = "shortcut_permissions",
                    onClick = { onNavigate(MikeRoutes.PERMISSIONS) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Diagnostic & Audit matériel",
                    subtitle = "${profile.screenWidth}×${profile.screenHeight} · ${profile.totalRamMb} Mo RAM · Journal réel",
                    icon = Icons.Filled.BugReport,
                    accent = MikeColors.Secondary,
                    testTag = "shortcut_diagnostic",
                    onClick = { onNavigate(MikeRoutes.DIAGNOSTIC) }
                )
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun CompactMetricCell(
    label: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MikeColors.Secondary,
            maxLines = 1
        )
    }
}

@Composable
private fun VerticalHairline() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(28.dp)
            .background(MikeColors.DividerSubtle)
    )
}
