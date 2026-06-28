package com.primaraya.inspectra.core.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Representasi destinasi top-level pada Navigation Bar.
 */
data class TopLevelDestination(
    val title: String,
    val icon: ImageVector,
    val screen: Screen,
    val checkActive: (String?) -> Boolean
)

val topLevelDestinations = listOf(
    TopLevelDestination(
        title = "Dashboard",
        icon = Icons.Default.Dashboard,
        screen = Screen.Dashboard,
        checkActive = { it?.contains("Dashboard") == true }
    ),
    TopLevelDestination(
        title = "Checksheet",
        icon = Icons.AutoMirrored.Filled.Assignment,
        screen = Screen.MenuChecksheet,
        checkActive = { it?.contains("MenuChecksheet") == true }
    ),
    TopLevelDestination(
        title = "Laporan",
        icon = Icons.Default.Assessment,
        screen = Screen.MenuLaporan,
        checkActive = { it?.contains("MenuLaporan") == true }
    ),
    TopLevelDestination(
        title = "Data Induk",
        icon = Icons.Default.Dataset,
        screen = Screen.MasterData,
        checkActive = { it?.contains("MasterData") == true }
    )
)

/**
 * Shell navigasi adaptif InSpectra.
 * - Phone: Bottom Navigation Bar (4 tab).
 * - Tablet (>=840dp): Navigation Rail di sisi kiri.
 * - Full-screen (Form, Splash): Nav tersembunyi.
 */
@Composable
fun AdaptiveNavigationShell(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isFullScreen = currentDestination?.route?.contains("Splash") == true ||
                       currentDestination?.route?.contains("FormChecksheet") == true ||
                       currentDestination?.route?.contains("FormLaporan") == true

    val showNavUI = !isFullScreen
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    if (isTablet && showNavUI) {
        // Tablet: NavigationRail + content
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(Modifier.height(16.dp))
                topLevelDestinations.forEach { dest ->
                    val isSelected = dest.checkActive(currentDestination?.route)
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(dest.screen) {
                                    popUpTo(Screen.Dashboard) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.title) },
                        label = { Text(dest.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                content(Modifier)
            }
        }
    } else {
        // Phone: Bottom Navigation Bar
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showNavUI) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val isSelected = dest.checkActive(currentDestination?.route)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        navController.navigate(dest.screen) {
                                            popUpTo(Screen.Dashboard) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content(Modifier)
            }
        }
    }
}
