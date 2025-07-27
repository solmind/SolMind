package com.solmind.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solmind.ui.screens.AddEntryScreen
import com.solmind.ui.screens.HomeScreen
import com.solmind.ui.screens.SettingsScreen
import com.solmind.ui.screens.WalletScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object AddEntry : Screen("add_entry", "Add Entry", Icons.Filled.Add)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object Wallet : Screen("wallet", "Wallet", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.AddEntry,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolMindNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.AddEntry.route) {
                AddEntryScreen(navController = navController)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(Screen.Wallet.route) {
                WalletScreen(navController = navController)
            }
        }
    }
}