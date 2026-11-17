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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.faceid.FaceAccentPresets
import com.mike.dynamicisland.faceid.FacePositionMode
import com.mike.dynamicisland.faceid.FaceScanStyle
import com.mike.dynamicisland.faceid.FaceUnlockPhase
import com.mike.dynamicisland.faceid.StyledFaceScanCanvas
import com.mike.dynamicisland.faceid.rememberFaceAnimationController
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface
import kotlin.math.roundToInt

/**
 * Studio de personnalisation Face ID (PRIORITÉ ABSOLUE — Module 01 & Module 09).
 */
@Composable
fun FaceIdScreen(
    viewModel: MikeViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val faceSettings = settings.faceId
    val faceState by viewModel.faceUnlockState.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    var loopPreview by remember { mutableStateOf(false) }

    val controller = rememberFaceAnimationController(
        unlockState = faceState,
        settings = faceSettings,
        onCompleted = { viewModel.dismissFaceIdAnimation() }
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Face ID Animation",
                subtitle = "Moteur prioritaire autour de la caméra Punch-Hole · 6 styles natifs",
                badgeText = faceState.phase.label,
                badgeColor = when (faceState.phase) {
                    FaceUnlockPhase.SUCCESS -> MikeColors.Green
                    FaceUnlockPhase.SCANNING -> MikeColors.Blue
                    FaceUnlockPhase.INTERRUPTED -> MikeColors.Coral
                    FaceUnlockPhase.IDLE -> MikeColors.Secondary
                }
            )
        }

        // Scène de prévisualisation temps réel avec objectif caméra simulé et calibration X/Y
        item {
            QuickGlassSurface(
                config = effectiveGlass,
                contentPadding = 16.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MikeDesignTokens.IslandBackground)
                        .border(0.75.dp, MikeColors.Hairline, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    val stageAlignment = when (faceSettings.positionMode) {
                        FacePositionMode.TOP_LEFT_PUNCH_HOLE -> Alignment.TopStart
                        FacePositionMode.TOP_CENTER -> Alignment.TopCenter
                        FacePositionMode.AUTO_CUTOUT -> if (profile.isTecnoOrHiOsDevice) Alignment.TopStart else Alignment.TopCenter
                        FacePositionMode.MANUAL -> Alignment.TopStart
                    }

                    val previewOffsetX = (faceSettings.manualOffsetXDp * 0.4f).dp
                    val previewOffsetY = (faceSettings.manualOffsetYDp * 0.4f).dp

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(stageAlignment)
                            .offset(x = previewOffsetX, y = previewOffsetY)
                    ) {
                        // Objectif Punch-Hole toujours visible au centre (jamais masqué)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF101624))
                                .border(1.5.dp, Color(0xFF33415C), CircleShape)
                        )

                        val activelyRunning = controller.isVisible || faceState.phase == FaceUnlockPhase.SCANNING
                        StyledFaceScanCanvas(
                            style = faceSettings.style,
                            progress = if (activelyRunning) controller.progress.value else 0.78f,
                            success = controller.isSuccess,
                            interrupted = controller.isInterrupted,
                            runContinuousMotion = (activelyRunning && !controller.isSuccess) || loopPreview,
                            accentColor = faceSettings.accentColor,
                            successColor = faceSettings.successColor,
                            sizeDp = faceSettings.sizeDp.dp.coerceIn(52.dp, 118.dp),
                            strokeWidthDp = faceSettings.strokeWidthDp.dp,
                            speedMultiplier = faceSettings.speedMultiplier,
                            maxParticleCount = profile.maxParticleCount
                        )
                    }

                    // Indication de provenance en bas de la scène
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = faceSettings.style.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MikeColors.White
                        )
                        Surface(
                            color = if (loopPreview) MikeColors.Blue.copy(alpha = 0.20f) else MikeColors.SurfaceElevated,
                            shape = RoundedCornerShape(50),
                            onClick = { loopPreview = !loopPreview },
                            modifier = Modifier.testTag("face_id_loop_preview_chip")
                        ) {
                            Text(
                                text = if (loopPreview) "Boucle active" else "Boucle au repos",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (loopPreview) MikeColors.Blue else MikeColors.Secondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

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
                            .testTag("face_id_test_success_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mike_face_id),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Tester validation", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = { viewModel.triggerFaceIdTest(simulateInterrupt = true) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("face_id_test_interrupt_button")
                    ) {
                        Icon(Icons.Filled.StopCircle, contentDescription = null, tint = MikeColors.Coral, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Interruption", color = MikeColors.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Transparence totale sur le mode dégradé Android 10 / HiOS 6.0
        item {
            QuickGlassSurface(
                config = effectiveGlass,
                accentColor = MikeColors.Amber,
                contentPadding = 14.dp
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Mode dégradé",
                        tint = MikeColors.Amber,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = faceState.capability.operatingModeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MikeColors.Amber
                        )
                        Text(
                            text = faceState.capability.honestExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                MikeDivider()
                MikeToggleRow(
                    title = "Activer l'animation Face ID",
                    subtitle = "Synchronisée avec l'écran allumé et Keyguard",
                    checked = faceSettings.enabled,
                    onCheckedChange = { enabled -> viewModel.updateFaceSettings { it.copy(enabled = enabled) } },
                    accent = MikeColors.Green,
                    testTag = "face_id_enabled_switch"
                )
                MikeDivider()
                MikeToggleRow(
                    title = "Déclencher sur USER_PRESENT (Mode dégradé)",
                    subtitle = "Valide visuellement lors du déverrouillage système",
                    checked = faceSettings.triggerOnDegradedKeyguardUnlock,
                    onCheckedChange = { enabled ->
                        viewModel.updateFaceSettings { it.copy(triggerOnDegradedKeyguardUnlock = enabled) }
                    },
                    accent = MikeColors.Amber,
                    testTag = "face_id_degraded_switch"
                )
            }
        }

        // Sélection des 6 styles Face ID
        item {
            MikeSectionLabel("6 Styles d'animation Face ID")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FaceScanStyle.entries.chunked(2).forEach { pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        pair.forEach { style ->
                            val selected = faceSettings.style == style
                            QuickGlassSurface(
                                config = effectiveGlass,
                                accentColor = if (selected) faceSettings.accentColor else MikeColors.OutlineSubtle,
                                contentPadding = 12.dp,
                                onClick = { viewModel.updateFaceSettings { it.copy(style = style) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("face_style_${style.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StyledFaceScanCanvas(
                                        style = style,
                                        progress = 0.82f,
                                        success = false,
                                        runContinuousMotion = selected && loopPreview,
                                        accentColor = faceSettings.accentColor,
                                        successColor = faceSettings.successColor,
                                        sizeDp = 46.dp,
                                        strokeWidthDp = 2.4.dp,
                                        maxParticleCount = 6
                                    )
                                    MikeStatusBadge(
                                        text = style.gpuCostLabel,
                                        color = if (selected) MikeColors.Blue else MikeColors.Secondary
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = style.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) MikeColors.White else MikeColors.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = style.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MikeColors.Secondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Couleur & Réglages géométriques / temporels
        item {
            MikeSectionLabel("Couleur, dimensions & rythme")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                Text("Couleur du scanner", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FaceAccentPresets.all.forEach { preset ->
                        val isSelected = faceSettings.accentColorArgb == preset.argb
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MikeColors.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateFaceSettings { it.copy(accentColorArgb = preset.argb) }
                                }
                                .testTag("face_color_${preset.name.lowercase().replace(" ", "_")}")
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Sélectionné ${preset.name}",
                                    tint = MikeColors.Background,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                MikeSliderRow(
                    title = "Taille de l'anneau",
                    valueLabel = "${faceSettings.sizeDp.roundToInt()} dp",
                    value = faceSettings.sizeDp,
                    valueRange = 54f..116f,
                    onValueChange = { v -> viewModel.updateFaceSettings { it.copy(sizeDp = v) } },
                    accent = faceSettings.accentColor
                )

                MikeSliderRow(
                    title = "Épaisseur lumineuse",
                    valueLabel = String.format("%.1f dp", faceSettings.strokeWidthDp),
                    value = faceSettings.strokeWidthDp,
                    valueRange = 1.6f..5.5f,
                    onValueChange = { v -> viewModel.updateFaceSettings { it.copy(strokeWidthDp = v) } },
                    accent = MikeColors.Cyan
                )

                MikeSliderRow(
                    title = "Vitesse d'animation",
                    valueLabel = String.format("%.2fx", faceSettings.speedMultiplier),
                    value = faceSettings.speedMultiplier,
                    valueRange = 0.6f..2.0f,
                    onValueChange = { v -> viewModel.updateFaceSettings { it.copy(speedMultiplier = v) } },
                    accent = MikeColors.Blue
                )

                MikeSliderRow(
                    title = "Durée du balayage",
                    valueLabel = "${faceSettings.durationMs} ms",
                    value = faceSettings.durationMs.toFloat(),
                    valueRange = 600f..2000f,
                    onValueChange = { v -> viewModel.updateFaceSettings { it.copy(durationMs = v.roundToInt()) } },
                    accent = MikeColors.Green
                )

                MikeToggleRow(
                    title = "Retour haptique de validation",
                    subtitle = "Micro-vibration courte lors du déverrouillage",
                    checked = faceSettings.vibrateOnSuccess,
                    onCheckedChange = { v -> viewModel.updateFaceSettings { it.copy(vibrateOnSuccess = v) } },
                    icon = Icons.Filled.Vibration,
                    accent = MikeColors.Green
                )
            }
        }

        // Positionnement caméra & calibration manuelle (Module 01-D)
        item {
            MikeSectionLabel("Positionnement caméra & calibration")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                val cutout = profile.displayCutoutBounds
                Text(
                    text = if (cutout != null) {
                        "DisplayCutout détecté : X=${cutout.centerXPx}px, Y=${cutout.centerYPx}px (${cutout.widthPx}×${cutout.heightPx}px)"
                    } else {
                        "DisplayCutout non fourni par le système : profil Punch-Hole TECNO Camon 15 Air ou calibration manuelle disponible."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeColors.Secondary
                )

                Spacer(Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FacePositionMode.entries.forEach { mode ->
                        val selected = faceSettings.positionMode == mode
                        Surface(
                            onClick = { viewModel.updateFaceSettings { it.copy(positionMode = mode) } },
                            color = if (selected) MikeColors.SurfaceElevated else MikeColors.Surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .border(
                                    width = if (selected) 1.dp else 0.75.dp,
                                    color = if (selected) MikeColors.Blue else MikeColors.Hairline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("face_pos_${mode.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MikeColors.White
                                )
                                if (selected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MikeColors.Blue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                MikeSliderRow(
                    title = "Décalage horizontal (X)",
                    valueLabel = "${faceSettings.manualOffsetXDp.roundToInt()} dp",
                    value = faceSettings.manualOffsetXDp,
                    valueRange = -80f..80f,
                    onValueChange = { v -> viewModel.updateFaceSettings { it.copy(manualOffsetXDp = v) } },
                    accent = MikeColors.Blue
                )

                MikeSliderRow(
                    title = "Décalage vertical (Y)",
                    valueLabel = "${faceSettings.manualOffsetYDp.roundToInt()} dp",
                    value = faceSettings.manualOffsetYDp,
                    valueRange = -40f..80f,
                    onValueChange = { v -> viewModel.updateFaceSettings { it.copy(manualOffsetYDp = v) } },
                    accent = MikeColors.Blue
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
