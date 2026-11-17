package com.mike.dynamicisland.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MikeColors
import com.mike.dynamicisland.notifications.NotificationController
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.quickglass.QuickGlassSurface
import com.mike.dynamicisland.utils.PermissionManager

/**
 * Page Notifications (Module 06 & Module 09).
 */
@Composable
fun NotificationsScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)
    val connected by viewModel.isNotificationListenerConnected.collectAsState()
    val entries by viewModel.activeNotifications.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Notifications",
                subtitle = "Regroupement local intelligent · Aucune synchronisation externe",
                badgeText = if (connected || permissions.notificationListenerGranted) "Connecté" else "Accès requis",
                badgeColor = if (connected || permissions.notificationListenerGranted) MikeColors.Green else MikeColors.Amber,
                onBack = onBack
            )
        }

        if (!permissions.notificationListenerGranted) {
            item {
                QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Amber) {
                    Text(
                        text = "Accès aux notifications non accordé",
                        style = MaterialTheme.typography.titleMedium,
                        color = MikeColors.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Autorisez MikeNotificationListenerService pour afficher WhatsApp, Messages, Gmail, Telegram et les appels entrants dans la Dynamic Island.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MikeColors.Secondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { PermissionManager.openNotificationListenerSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Blue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("notifications_open_settings_btn")
                    ) {
                        Text("Ouvrir l'accès aux notifications")
                    }
                }
            }
        }

        item {
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Green) {
                MikeToggleRow(
                    title = "Mode confidentialité global",
                    subtitle = "Masque le texte des messages dans l'îlot flottant",
                    checked = settings.privacy.privacyModeMaskPreview,
                    onCheckedChange = { v -> viewModel.updatePrivacy { it.copy(privacyModeMaskPreview = v) } },
                    icon = Icons.Filled.Lock,
                    accent = MikeColors.Green
                )
            }
        }

        // Filtrage des applications prioritaires
        item {
            MikeSectionLabel("Applications prioritaires autorisées")
            Spacer(Modifier.height(6.dp))
            QuickGlassSurface(config = effectiveGlass) {
                NotificationController.PRIORITY_APPS.forEachIndexed { idx, app ->
                    val allowed = app.packageName in settings.island.allowedNotificationPackages
                    MikeToggleRow(
                        title = app.displayName,
                        subtitle = "${app.category} · ${app.packageName}",
                        checked = allowed,
                        onCheckedChange = { viewModel.toggleAllowedNotificationPackage(app.packageName, it) },
                        accent = MikeColors.Blue
                    )
                    if (idx < NotificationController.PRIORITY_APPS.lastIndex) {
                        MikeDivider()
                    }
                }
            }
        }

        // Flux local temps réel
        item {
            MikeSectionLabel("Notifications locales interceptées (${entries.size})")
            Spacer(Modifier.height(6.dp))
            if (entries.isEmpty()) {
                QuickGlassSurface(config = effectiveGlass) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = MikeColors.Secondary)
                        Text(
                            text = "Aucune notification active en mémoire. Les notifications réelles reçues apparaîtront ici en direct (zéro donnée fictive).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    entries.values.sortedByDescending { it.postedAtMs }.forEach { entry ->
                        QuickGlassSurface(config = effectiveGlass) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(entry.appName, style = MaterialTheme.typography.labelMedium, color = MikeColors.Blue)
                                OutlinedButton(
                                    onClick = { viewModel.engine.notificationController.dismissNotification(entry.key) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Ignorer", style = MaterialTheme.typography.labelSmall, color = MikeColors.Secondary)
                                }
                            }
                            Text(entry.title, style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                            Text(
                                text = if (settings.privacy.privacyModeMaskPreview) "Contenu masqué" else entry.text,
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

        item { Spacer(Modifier.height(16.dp)) }
    }
}

/**
 * Page Musique & Médias basée exclusivement sur MediaSession natif (Module 07 & Module 09).
 */
@Composable
fun MusicScreen(
    viewModel: MikeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(settings.quickGlass, profile)
    val media by viewModel.mediaState.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            MikeScreenHeader(
                title = "Musique & Médias",
                subtitle = "Contrôle MediaSession natif · Aucune donnée multimédia simulée",
                badgeText = if (media != null) "Session active" else "En attente",
                badgeColor = if (media != null) MikeColors.Purple else MikeColors.Secondary,
                onBack = onBack
            )
        }

        item {
            val activeMedia = media
            QuickGlassSurface(config = effectiveGlass, accentColor = MikeColors.Purple) {
                if (activeMedia == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MikeColors.Purple.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MikeColors.Purple,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "Aucune session MediaSession détectée",
                            style = MaterialTheme.typography.titleMedium,
                            color = MikeColors.White
                        )
                        Text(
                            text = if (permissions.notificationListenerGranted) {
                                "Lancez Spotify, YouTube Music ou votre lecteur local : la pochette, le titre et les commandes réelles apparaîtront automatiquement ici et dans la Dynamic Island."
                            } else {
                                "Android exige l'accès aux notifications pour connecter MediaSessionManager aux lecteurs audio actifs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MikeColors.Secondary
                        )
                        if (!permissions.notificationListenerGranted) {
                            Button(
                                onClick = { PermissionManager.openNotificationListenerSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = MikeColors.Purple),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Autoriser l'accès MediaSession")
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val bmp = activeMedia.artwork
                        if (bmp != null && !bmp.isRecycled) {
                            val img = remember(bmp) { bmp.asImageBitmap() }
                            Image(
                                bitmap = img,
                                contentDescription = "Pochette ${activeMedia.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MikeColors.Purple.copy(alpha = 0.22f))
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MikeColors.Purple, modifier = Modifier.size(32.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeMedia.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MikeColors.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeMedia.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MikeColors.Secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeMedia.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MikeColors.Purple
                            )
                        }
                    }

                    if (activeMedia.durationMs > 0L) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { activeMedia.progress },
                            color = MikeColors.Purple,
                            trackColor = MikeColors.SurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { viewModel.engine.mediaSessionController.skipToPrevious() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Piste précédente", tint = MikeColors.White)
                        }
                        Surface(
                            shape = CircleShape,
                            color = MikeColors.White,
                            onClick = { viewModel.engine.mediaSessionController.togglePlayPause() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (activeMedia.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (activeMedia.isPlaying) "Pause" else "Lecture",
                                    tint = MikeColors.Background
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.engine.mediaSessionController.skipToNext() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Piste suivante", tint = MikeColors.White)
                        }
                    }
                }
            }
        }

        item {
            QuickGlassSurface(config = effectiveGlass) {
                Text("Optimisation mémoire pochette", style = MaterialTheme.typography.titleSmall, color = MikeColors.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Les pochettes MediaMetadata sont automatiquement réduites à ${profile.maxAlbumArtSizePx}×${profile.maxAlbumArtSizePx} px afin d'éviter toute saturation RAM sur les appareils 3 Go.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MikeColors.Secondary
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
