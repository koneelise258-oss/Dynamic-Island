package com.mike.dynamicisland.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface
import com.mike.dynamicisland.utils.PermissionManager

/**
 * Hub Réglages donnant accès aux sous-pages Apparence, Personnalisation, Confidentialité,
 * Permissions, Activités, Notifications, Musique et Diagnostic (Module 09).
 */
@Composable
fun SettingsScreen(
    viewModel: MikeViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Réglages",
                subtitle = "Organisation One UI · Accès rapide à chaque module"
            )
        }

        item {
            MikeSectionLabel("Expérience & Modules")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass, contentPadding = 10.dp) {
                MikeNavigationTile(
                    title = "MIKE Control Center",
                    subtitle = "Niveau 3 · Volume radial, Genres, Statistiques & Accessoires",
                    icon = Icons.Filled.Equalizer,
                    accent = MikeColors.Green,
                    trailingBadge = "Niveau 3",
                    testTag = "settings_nav_control_center",
                    onClick = { onNavigate(MikeRoutes.CONTROL_CENTER) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Activités Now Bar",
                    subtitle = "Minuteur, Chronomètre, Alarme, Batterie",
                    icon = Icons.Filled.Timer,
                    accent = MikeColors.Amber,
                    testTag = "settings_nav_activities",
                    onClick = { onNavigate(MikeRoutes.ACTIVITIES) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Notifications",
                    subtitle = "Applications autorisées & regroupement local",
                    icon = Icons.Filled.Notifications,
                    accent = MikeColors.Blue,
                    testTag = "settings_nav_notifications",
                    onClick = { onNavigate(MikeRoutes.NOTIFICATIONS) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Musique & Médias",
                    subtitle = "MediaSession natif & commandes de lecture",
                    icon = Icons.Filled.MusicNote,
                    accent = MikeColors.Purple,
                    testTag = "settings_nav_music",
                    onClick = { onNavigate(MikeRoutes.MUSIC) }
                )
            }
        }

        item {
            MikeSectionLabel("Interface & Système")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass, contentPadding = 10.dp) {
                MikeNavigationTile(
                    title = "Apparence",
                    subtitle = "One UI, QuickStar et thème adaptatif",
                    icon = Icons.Filled.Palette,
                    accent = MikeColors.White,
                    testTag = "settings_nav_appearance",
                    onClick = { onNavigate(MikeRoutes.APPEARANCE) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Personnalisation",
                    subtitle = "Démarrage automatique & réinitialisation",
                    icon = Icons.Filled.Tune,
                    accent = MikeColors.Blue,
                    testTag = "settings_nav_customization",
                    onClick = { onNavigate(MikeRoutes.CUSTOMIZATION) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Confidentialité",
                    subtitle = "100% local · Masquage sur écran verrouillé",
                    icon = Icons.Filled.Lock,
                    accent = MikeColors.Green,
                    testTag = "settings_nav_privacy",
                    onClick = { onNavigate(MikeRoutes.PRIVACY) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Permissions",
                    subtitle = "Superposition, Notifications, Batterie & HiOS",
                    icon = Icons.Filled.Security,
                    accent = if (permissions.allCoreGranted) MikeColors.Green else MikeColors.Amber,
                    trailingBadge = "${permissions.grantedCount}/${permissions.totalCount}",
                    testTag = "settings_nav_permissions",
                    onClick = { onNavigate(MikeRoutes.PERMISSIONS) }
                )
                MikeDivider()
                MikeNavigationTile(
                    title = "Diagnostic & Tests",
                    subtitle = "Profil matériel, RAM réelle & journal d'événements",
                    icon = Icons.Filled.BugReport,
                    accent = MikeColors.Secondary,
                    testTag = "settings_nav_diagnostic",
                    onClick = { onNavigate(MikeRoutes.DIAGNOSTIC) }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun AppearanceScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)
    val appearance = settings.appearance

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Apparence",
                subtitle = "Identité visuelle MIKE · Inspiration One UI & QuickStar",
                onBack = onBack
            )
        }
        item {
            QuickGlassSurface(config = effectiveGlass) {
                MikeToggleRow(
                    title = "Couleurs dynamiques Material You (Android 12+)",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "Adapte la teinte d'accent au fond d'écran système"
                    } else {
                        "Palette Quick Glass Mike native utilisée sur Android ${profile.androidVersion}"
                    },
                    checked = appearance.useDynamicMaterialColors,
                    onCheckedChange = { v -> viewModel.updateAppearance { it.copy(useDynamicMaterialColors = v) } },
                    accent = MikeColors.Blue,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                )
                MikeDivider()
                MikeToggleRow(
                    title = "En-têtes compacts One UI",
                    subtitle = "Optimise l'espace vertical sur écran 720×1600",
                    checked = appearance.compactOneUiHeader,
                    onCheckedChange = { v -> viewModel.updateAppearance { it.copy(compactOneUiHeader = v) } },
                    accent = MikeColors.Blue
                )
                MikeDivider()
                MikeToggleRow(
                    title = "Style QuickStar : indicateurs discrets",
                    subtitle = "Masque toute décoration superflue quand l'îlot est inactif",
                    checked = appearance.quickStarHideIdleIndicators,
                    onCheckedChange = { v -> viewModel.updateAppearance { it.copy(quickStarHideIdleIndicators = v) } },
                    accent = MikeColors.Purple
                )
                MikeDivider()
                MikeToggleRow(
                    title = "Retours haptiques UI",
                    subtitle = "Légère vibration lors des validations importantes",
                    checked = appearance.hapticFeedbackEnabled,
                    onCheckedChange = { v -> viewModel.updateAppearance { it.copy(hapticFeedbackEnabled = v) } },
                    icon = Icons.Filled.Vibration,
                    accent = MikeColors.Green
                )
            }
        }
    }
}

@Composable
fun CustomizationScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Personnalisation",
                subtitle = "Comportement système, démarrage et profils",
                onBack = onBack
            )
        }
        item {
            QuickGlassSurface(config = effectiveGlass) {
                MikeToggleRow(
                    title = "Relancer après redémarrage",
                    subtitle = "Utilise BootReceiver si le service et la superposition sont autorisés",
                    checked = settings.island.autoStartOnBoot,
                    onCheckedChange = { v -> viewModel.updateIslandSettings { it.copy(autoStartOnBoot = v) } },
                    icon = Icons.Filled.PowerSettingsNew,
                    accent = MikeColors.Blue,
                    testTag = "customization_boot_switch"
                )
                MikeDivider()
                MikeToggleRow(
                    title = "Badge horloge QuickStar compact",
                    subtitle = "Allège la zone supérieure près de la caméra frontale",
                    checked = settings.appearance.quickStarCompactClockBadge,
                    onCheckedChange = { v -> viewModel.updateAppearance { it.copy(quickStarCompactClockBadge = v) } },
                    accent = MikeColors.Blue
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.resetAllDefaults() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("customization_reset_defaults_btn")
                ) {
                    Text("Réinitialiser tous les réglages par défaut", color = MikeColors.Coral)
                }
            }
        }
    }
}

@Composable
fun PrivacyScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Confidentialité",
                subtitle = "Traitement strictement local sur l'appareil",
                badgeText = "Zéro Cloud",
                badgeColor = MikeColors.Green,
                onBack = onBack
            )
        }
        item {
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Green) {
                Text("Garantie d'isolation locale", style = MaterialTheme.typography.titleMedium, color = MikeColors.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Aucune permission INTERNET n'est requise : vos notifications, vos préférences et vos événements système ne quittent jamais votre téléphone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeColors.Secondary
                )
                Spacer(Modifier.height(10.dp))
                MikeDivider()
                MikeToggleRow(
                    title = "Masquer sur écran verrouillé",
                    subtitle = "Cache l'expéditeur et le texte tant que Keyguard est verrouillé",
                    checked = settings.privacy.hideNotificationContentWhenLocked,
                    onCheckedChange = { v -> viewModel.updatePrivacy { it.copy(hideNotificationContentWhenLocked = v) } },
                    icon = Icons.Filled.Lock,
                    accent = MikeColors.Green,
                    testTag = "privacy_hide_locked_switch"
                )
                MikeDivider()
                MikeToggleRow(
                    title = "Masquage permanent des aperçus",
                    subtitle = "Affiche uniquement l'application dans la Dynamic Island",
                    checked = settings.privacy.privacyModeMaskPreview,
                    onCheckedChange = { v -> viewModel.updatePrivacy { it.copy(privacyModeMaskPreview = v) } },
                    accent = MikeColors.Blue,
                    testTag = "privacy_mask_all_switch"
                )
            }
        }
    }
}

@Composable
fun PermissionsScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)

    val postNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshDiagnosticsAndPermissions()
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Permissions",
                subtitle = "Permissions minimales requises · Fonctionnement sans root",
                badgeText = "${permissions.grantedCount}/${permissions.totalCount}",
                badgeColor = if (permissions.allCoreGranted) MikeColors.Green else MikeColors.Amber,
                onBack = onBack
            )
        }

        item {
            PermissionRowCard(
                title = "Superposition d'écran (SYSTEM_ALERT_WINDOW)",
                description = "Indispensable pour afficher Face ID autour de la caméra et la Dynamic Island par-dessus les autres applications.",
                granted = permissions.overlayGranted,
                buttonLabel = "Ouvrir réglages",
                testTag = "perm_overlay_btn",
                glassConfig = effectiveGlass,
                onAction = { PermissionManager.openOverlaySettings(context) }
            )
        }

        item {
            PermissionRowCard(
                title = "Accès aux notifications & MediaSession",
                description = "Permet de lire localement WhatsApp, Messages, Gmail, Telegram, les appels et les sessions musicales natives.",
                granted = permissions.notificationListenerGranted,
                buttonLabel = "Autoriser l'écouteur",
                testTag = "perm_notif_listener_btn",
                glassConfig = effectiveGlass,
                onAction = { PermissionManager.openNotificationListenerSettings(context) }
            )
        }

        item {
            PermissionRowCard(
                title = "Notifications du service (Android 13+)",
                description = "Requis sur Android 13+ pour afficher la notification discrète du service de premier plan.",
                granted = permissions.postNotificationsGranted,
                buttonLabel = "Demander",
                testTag = "perm_post_notif_btn",
                glassConfig = effectiveGlass,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.refreshDiagnosticsAndPermissions()
                    }
                }
            )
        }

        item {
            PermissionRowCard(
                title = "Exclusion économiseur de batterie",
                description = "Recommandé sur TECNO HiOS 6.0 (Phone Master) pour éviter que le système ferme le service après mise en veille.",
                granted = permissions.batteryOptimizationIgnored,
                buttonLabel = "Configurer batterie",
                testTag = "perm_battery_btn",
                glassConfig = effectiveGlass,
                onAction = { PermissionManager.openBatteryOptimizationSettings(context) }
            )
        }

        item {
            QuickGlassSurface(config = effectiveGlass) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.BatterySaver, contentDescription = null, tint = MikeColors.Amber)
                    Text("Guide TECNO Camon 15 Air (HiOS 6.0)", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "1. Ouvrez Phone Master → Démarrage automatique → activez Mike Dynamic Island.\n2. Dans Gestionnaire de batterie → Applications protégées, verrouillez Mike Dynamic Island.\n3. Revenez ici puis appuyez sur Actualiser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeColors.Secondary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.refreshDiagnosticsAndPermissions() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("perm_refresh_btn")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = MikeColors.White)
                    Text("  Actualiser l'état des permissions", color = MikeColors.White)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonLabel: String,
    testTag: String,
    glassConfig: com.mike.dynamicisland.quickglass.QuickGlassConfig,
    onAction: () -> Unit
) {
    QuickGlassSurface(
        config = glassConfig,
        accentColor = if (granted) MikeColors.Green else MikeColors.Amber
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MikeColors.White,
                modifier = Modifier.weight(1f)
            )
            MikeStatusBadge(
                text = if (granted) "AUTORISÉ" else "REQUIS",
                color = if (granted) MikeColors.Green else MikeColors.Amber
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MikeColors.Secondary
        )
        if (!granted) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Blue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(testTag)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
