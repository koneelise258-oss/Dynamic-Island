package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MikeColors
import com.example.ui.theme.MikeDynamicIslandTheme
import com.mike.dynamicisland.MikeApplication
import com.mike.dynamicisland.quickglass.LightingEffects
import com.mike.dynamicisland.ui.ActivitiesScreen
import com.mike.dynamicisland.ui.AppearanceScreen
import com.mike.dynamicisland.ui.ControlCenterScreen
import com.mike.dynamicisland.ui.CustomizationScreen
import com.mike.dynamicisland.ui.DiagnosticScreen
import com.mike.dynamicisland.ui.FaceIdScreen
import com.mike.dynamicisland.ui.HomeScreen
import com.mike.dynamicisland.ui.IslandScreen
import com.mike.dynamicisland.ui.MikeRoutes
import com.mike.dynamicisland.ui.MikeTopDestinations
import com.mike.dynamicisland.ui.MikeViewModel
import com.mike.dynamicisland.ui.MusicScreen
import com.mike.dynamicisland.ui.NotificationsScreen
import com.mike.dynamicisland.ui.PermissionsScreen
import com.mike.dynamicisland.ui.PrivacyScreen
import com.mike.dynamicisland.ui.QuickGlassScreen
import com.mike.dynamicisland.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val engine = MikeApplication.getEngine(applicationContext)
        window.decorView.post {
            engine.refineCutoutFromActivity(this)
        }

        setContent {
            val viewModel: MikeViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()

            MikeDynamicIslandTheme(
                useDynamicColor = settings.appearance.useDynamicMaterialColors
            ) {
                MikeDynamicIslandApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val engine = MikeApplication.getEngine(applicationContext)
        engine.refreshPermissions()
        engine.refineCutoutFromActivity(this)
    }
}

@Composable
fun MikeDynamicIslandApp(viewModel: MikeViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MikeRoutes.HOME

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnosticsAndPermissions()
    }

    val navigateTo: (String) -> Unit = { route ->
        if (route != currentRoute) {
            navController.navigate(route) {
                launchSingleTop = true
                if (route in MikeTopDestinations.map { it.route }) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    restoreState = true
                }
            }
        }
    }

    val topLevelRoutes = MikeTopDestinations.map { it.route }.toSet()
    val selectedTabRoute = if (currentRoute in topLevelRoutes) currentRoute else MikeRoutes.SETTINGS

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(LightingEffects.ambientScreenBackdrop())
    ) {
        val useNavigationRail = maxWidth >= 680.dp

        if (useNavigationRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = Color(0xFF0C0C0E),
                    contentColor = MikeColors.White,
                    modifier = Modifier
                        .fillMaxHeight()
                        .navigationBarsPadding()
                ) {
                    MikeTopDestinations.forEach { dest ->
                        val selected = selectedTabRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MikeColors.White,
                                selectedTextColor = MikeColors.White,
                                indicatorColor = MikeColors.SurfaceElevated,
                                unselectedIconColor = MikeColors.Secondary,
                                unselectedTextColor = MikeColors.Secondary
                            ),
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 0.5.dp, max = 0.5.dp)
                        .background(MikeColors.DividerSubtle)
                )
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MikeNavHost(
                            navController = navController,
                            viewModel = viewModel,
                            onNavigate = navigateTo,
                            modifier = Modifier.widthIn(max = 760.dp)
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MikeColors.DividerSubtle
                        )
                        NavigationBar(
                            containerColor = Color(0xFF0C0C0E),
                            contentColor = MikeColors.White,
                            tonalElevation = 0.dp
                        ) {
                            MikeTopDestinations.forEach { dest ->
                                val selected = selectedTabRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MikeColors.White,
                                        selectedTextColor = MikeColors.White,
                                        indicatorColor = MikeColors.SurfaceElevated,
                                        unselectedIconColor = MikeColors.Secondary,
                                        unselectedTextColor = MikeColors.Secondary
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                MikeNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    onNavigate = navigateTo,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun MikeNavHost(
    navController: androidx.navigation.NavHostController,
    viewModel: MikeViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MikeRoutes.HOME,
        modifier = modifier
    ) {
        composable(MikeRoutes.HOME) {
            HomeScreen(viewModel = viewModel, onNavigate = onNavigate)
        }
        composable(MikeRoutes.FACE_ID) {
            FaceIdScreen(viewModel = viewModel)
        }
        composable(MikeRoutes.ISLAND) {
            IslandScreen(viewModel = viewModel, onNavigate = onNavigate)
        }
        composable(MikeRoutes.QUICK_GLASS) {
            QuickGlassScreen(viewModel = viewModel)
        }
        composable(MikeRoutes.SETTINGS) {
            SettingsScreen(viewModel = viewModel, onNavigate = onNavigate)
        }
        composable(MikeRoutes.CONTROL_CENTER) {
            ControlCenterScreen(
                viewModel = viewModel,
                onNavigate = onNavigate,
                onBack = { navController.popBackStack() }
            )
        }
        composable(MikeRoutes.ACTIVITIES) {
            ActivitiesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(MikeRoutes.NOTIFICATIONS) {
            NotificationsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(MikeRoutes.MUSIC) {
            MusicScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(MikeRoutes.APPEARANCE) {
            AppearanceScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(MikeRoutes.CUSTOMIZATION) {
            CustomizationScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(MikeRoutes.PRIVACY) {
            PrivacyScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(MikeRoutes.PERMISSIONS) {
            PermissionsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(MikeRoutes.DIAGNOSTIC) {
            DiagnosticScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
