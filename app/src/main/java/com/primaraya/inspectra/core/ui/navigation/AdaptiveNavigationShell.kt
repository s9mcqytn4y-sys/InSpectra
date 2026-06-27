package com.primaraya.inspectra.core.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState


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
        title = "Master Data",
        icon = Icons.Default.Dataset,
        screen = Screen.MasterData,
        checkActive = { it?.contains("MasterData") == true }
    )
)

@Composable
fun AdaptiveNavigationShell(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show navigation UI if we are not on Splash or deeply nested screens (like FormChecksheet)
    // Actually, for SaaS, it's good to keep the nav shell visible except on Splash or full-screen processes.
    val isFullScreen = currentDestination?.route?.contains("Splash") == true || 
                       currentDestination?.route?.contains("FormChecksheet") == true
    
    val showNavUI = !isFullScreen
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current

    Scaffold(
        bottomBar = {
            if (showNavUI) {
                NavigationBar {
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
                            label = { Text(dest.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                content(Modifier)
            }
        }
    }
}
