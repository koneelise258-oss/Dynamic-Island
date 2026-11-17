package com.mike.dynamicisland.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Identifiants uniques de navigation pour les 12 écrans du Module 09.
 */
object MikeRoutes {
    const val HOME = "home"
    const val FACE_ID = "face_id"
    const val ISLAND = "island"
    const val QUICK_GLASS = "quick_glass"
    const val SETTINGS = "settings"

    // Sous-écrans dédiés (inclut Niveau 3 MIKE Control Center V7)
    const val CONTROL_CENTER = "control_center"
    const val ACTIVITIES = "activities"
    const val NOTIFICATIONS = "notifications"
    const val MUSIC = "music"
    const val APPEARANCE = "appearance"
    const val CUSTOMIZATION = "customization"
    const val PRIVACY = "privacy"
    const val PERMISSIONS = "permissions"
    const val DIAGNOSTIC = "diagnostic"
}

data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val MikeTopDestinations = listOf(
    TopLevelDestination(
        route = MikeRoutes.HOME,
        label = "Accueil",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    TopLevelDestination(
        route = MikeRoutes.FACE_ID,
        label = "Face ID",
        selectedIcon = Icons.Filled.Face,
        unselectedIcon = Icons.Outlined.Face,
        testTag = "nav_tab_face_id"
    ),
    TopLevelDestination(
        route = MikeRoutes.ISLAND,
        label = "Island",
        selectedIcon = Icons.Filled.ViewAgenda,
        unselectedIcon = Icons.Outlined.ViewAgenda,
        testTag = "nav_tab_island"
    ),
    TopLevelDestination(
        route = MikeRoutes.QUICK_GLASS,
        label = "Glass",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_glass"
    ),
    TopLevelDestination(
        route = MikeRoutes.SETTINGS,
        label = "Réglages",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)
