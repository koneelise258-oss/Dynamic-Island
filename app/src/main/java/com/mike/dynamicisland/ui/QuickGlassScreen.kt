package com.mike.dynamicisland.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassPreset
import com.mike.dynamicisland.quickglass.QuickGlassSurface
import com.mike.dynamicisland.quickglass.mikeAmbientHalo
import kotlin.math.roundToInt

/**
 * Moteur visuel Quick Glass (Module 03 & Module 09).
 */
@Composable
fun QuickGlassScreen(
    viewModel: MikeViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val rawGlass = settings.quickGlass
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(rawGlass, profile)
    val blurSupported = QuickGlassEngine.isHardwareBlurSupported(profile)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Quick Glass",
                subtitle = "Verre stratifié discret, filet supérieur fin et rendu fluide 60 Hz",
                badgeText = if (blurSupported) "API ${profile.androidVersion} · Flou dispo" else "Verre stratifié 60 Hz",
                badgeColor = MikeColors.Blue
            )
        }

        // Aperçu en direct sur fond lumineux Quick Glass
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .mikeAmbientHalo(MikeColors.Blue, effectiveGlass.accentTint, effectiveGlass.haloIntensity)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_quick_glass_backdrop),
                    contentDescription = "Fond de prévisualisation Quick Glass",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.75f)
                )
                Box(modifier = Modifier.padding(18.dp)) {
                    QuickGlassSurface(
                        config = effectiveGlass,
                        contentPadding = 18.dp,
                        modifier = Modifier.fillMaxWidth()
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
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = effectiveGlass.accentTint
                                )
                                Text(
                                    text = rawGlass.preset.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MikeColors.White
                                )
                            }
                            MikeStatusBadge(
                                text = "${(effectiveGlass.glassAlpha * 100).roundToInt()}%",
                                color = effectiveGlass.accentTint
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Effet verre translucide obtenu sans RenderEffect : dégradés multicouches, reflet spéculaire supérieur et halo contrôlé pour 60 FPS stables sur PowerVR GE8320.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
            }
        }

        // Préréglages Quick Glass
        item {
            MikeSectionLabel("Préréglages Quick Glass")
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickGlassPreset.entries.forEach { preset ->
                    val selected = rawGlass.preset == preset
                    QuickGlassSurface(
                        config = effectiveGlass,
                        accentColor = if (selected) MikeColors.Blue else MikeColors.OutlineSubtle,
                        contentPadding = 14.dp,
                        onClick = { viewModel.applyQuickGlassPreset(preset) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("glass_preset_${preset.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MikeColors.White
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MikeColors.Secondary
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Actif",
                                    tint = MikeColors.Blue
                                )
                            }
                        }
                    }
                }
            }
        }

        // Réglages fins de translucidité et lumière
        item {
            MikeSectionLabel("Optique & profondeur")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                MikeSliderRow(
                    title = "Opacité du verre",
                    valueLabel = "${(rawGlass.glassAlpha * 100).roundToInt()}%",
                    value = rawGlass.glassAlpha,
                    valueRange = 0.48f..0.94f,
                    onValueChange = { v -> viewModel.updateQuickGlass { it.copy(glassAlpha = v) } },
                    accent = MikeColors.Cyan
                )
                MikeSliderRow(
                    title = "Éclat des bordures",
                    valueLabel = "${(rawGlass.borderGlowIntensity * 100).roundToInt()}%",
                    value = rawGlass.borderGlowIntensity,
                    valueRange = 0.10f..0.70f,
                    onValueChange = { v -> viewModel.updateQuickGlass { it.copy(borderGlowIntensity = v) } },
                    accent = MikeColors.Blue
                )
                MikeSliderRow(
                    title = "Intensité du halo ambiant",
                    valueLabel = "${(rawGlass.haloIntensity * 100).roundToInt()}%",
                    value = rawGlass.haloIntensity,
                    valueRange = 0f..0.70f,
                    onValueChange = { v -> viewModel.updateQuickGlass { it.copy(haloIntensity = v) } },
                    accent = MikeColors.Purple
                )
                MikeSliderRow(
                    title = "Arrondi des surfaces",
                    valueLabel = "${rawGlass.cornerRadiusDp} dp",
                    value = rawGlass.cornerRadiusDp.toFloat(),
                    valueRange = 14f..32f,
                    onValueChange = { v -> viewModel.updateQuickGlass { it.copy(cornerRadiusDp = v.roundToInt()) } },
                    accent = MikeColors.Cyan
                )

                Spacer(Modifier.height(6.dp))
                Text("Teinte des reflets", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                Spacer(Modifier.height(8.dp))
                val tints = listOf(
                    0xFF0A84FFL to "Bleu",
                    0xFF30D158L to "Émeraude",
                    0xFFBF5AF2L to "Violet",
                    0xFF8E8E93L to "Neutre"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    tints.forEach { (argb, label) ->
                        val isSelected = rawGlass.accentTintArgb == argb
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MikeColors.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.updateQuickGlass { it.copy(accentTintArgb = argb) } }
                                .testTag("glass_tint_${label.lowercase()}")
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = label, tint = MikeColors.Background, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                MikeDivider()

                MikeToggleRow(
                    title = "Reflet spéculaire supérieur",
                    subtitle = "Filet lumineux 0.5 dp discret sur le bord haut des cartes",
                    checked = rawGlass.specularHighlightEnabled,
                    onCheckedChange = { v -> viewModel.updateQuickGlass { it.copy(specularHighlightEnabled = v) } },
                    icon = Icons.Filled.LightMode,
                    accent = MikeColors.Blue
                )
                MikeDivider()
                MikeToggleRow(
                    title = "Flou matériel optionnel (Android 12+)",
                    subtitle = if (blurSupported) {
                        "Compatible sur cet appareil (désactivé par défaut sur profil éco)"
                    } else {
                        "Indisponible sur Android ${profile.androidVersion} / profil 3 Go RAM — verre stratifié actif"
                    },
                    checked = rawGlass.optionalHardwareBlurEnabled && blurSupported,
                    onCheckedChange = { v ->
                        if (blurSupported) {
                            viewModel.updateQuickGlass { it.copy(optionalHardwareBlurEnabled = v) }
                        }
                    },
                    icon = Icons.Filled.BlurOn,
                    accent = MikeColors.Blue,
                    enabled = blurSupported
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
