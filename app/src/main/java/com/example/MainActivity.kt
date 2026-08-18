package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.model.GameItem
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

sealed class AppDestination(val route: String, val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector) {
    object Library : AppDestination("library", "Library", Icons.Filled.Games, Icons.Outlined.Games)
    object Controllers : AppDestination("controllers", "Gamepad", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports)
    object Settings : AppDestination("settings", "Graphics", Icons.Filled.Tune, Icons.Outlined.Tune)
    object Diagnostics : AppDestination("diagnostics", "Hardware", Icons.Filled.Memory, Icons.Outlined.Memory)
    object Logs : AppDestination("logs", "Logs", Icons.Filled.Terminal, Icons.Outlined.Terminal)
    object Emulation : AppDestination("emulation", "Emulation", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            XeniaAndroidTheme {
                XeniaApp(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && viewModel.controllerManager.handleKeyEvent(event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && viewModel.controllerManager.handleKeyEvent(event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null && viewModel.controllerManager.handleMotionEvent(event)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}

@Composable
fun XeniaApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var activeGameForPlay by remember { mutableStateOf<GameItem?>(null) }
    val isEmulating = currentRoute == AppDestination.Emulation.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            if (!isEmulating) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TechTextPrimary,
                    tonalElevation = 8.dp
                ) {
                    val destinations = listOf(
                        AppDestination.Library,
                        AppDestination.Controllers,
                        AppDestination.Settings,
                        AppDestination.Diagnostics,
                        AppDestination.Logs
                    )

                    destinations.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (isSelected) screen.iconFilled else screen.iconOutlined,
                                    contentDescription = screen.label,
                                    tint = if (isSelected) XeniaGreen else TechTextSecondary
                                )
                            },
                            label = {
                                Text(
                                    screen.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) XeniaGreen else TechTextSecondary
                                )
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = XeniaGreen,
                                unselectedIconColor = TechTextSecondary,
                                indicatorColor = XeniaGreenContainer
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Library.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isEmulating) PaddingValues(0.dp) else innerPadding)
        ) {
            composable(AppDestination.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onGameSelectedForPlay = { game ->
                        activeGameForPlay = game
                        viewModel.launchGame(game)
                        navController.navigate(AppDestination.Emulation.route)
                    }
                )
            }

            composable(AppDestination.Controllers.route) {
                ControllerScreen(viewModel = viewModel)
            }

            composable(AppDestination.Settings.route) {
                GraphicsSettingsScreen(viewModel = viewModel)
            }

            composable(AppDestination.Diagnostics.route) {
                DiagnosticsScreen(viewModel = viewModel)
            }

            composable(AppDestination.Logs.route) {
                LogsScreen(viewModel = viewModel)
            }

            composable(AppDestination.Emulation.route) {
                val game = activeGameForPlay
                if (game != null) {
                    EmulationScreen(
                        game = game,
                        viewModel = viewModel,
                        onExitEmulation = {
                            viewModel.stopEmulation()
                            navController.popBackStack(AppDestination.Library.route, false)
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
