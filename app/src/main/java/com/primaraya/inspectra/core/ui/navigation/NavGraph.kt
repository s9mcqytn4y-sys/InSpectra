package com.primaraya.inspectra.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses
import com.primaraya.inspectra.fitur.checksheet.ui.ChecksheetScreen
import com.primaraya.inspectra.fitur.cutting.ui.CuttingScreen
import com.primaraya.inspectra.fitur.checksheet.ui.MenuChecksheetScreen
import com.primaraya.inspectra.fitur.dashboard.ui.DashboardScreen
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataScreen
import com.primaraya.inspectra.fitur.splash.SplashScreen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Screen = Screen.Splash
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable<Screen.Splash> {
            SplashScreen(
                onSelesai = {
                    navController.navigate(Screen.Dashboard) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Dashboard> {
            DashboardScreen(
                onChecksheetClick = { navController.navigate(Screen.MenuChecksheet) },
                onLaporanClick = { navController.navigate(Screen.MenuLaporan) },
                onMasterDataClick = { navController.navigate(Screen.MasterData) }
            )
        }

        composable<Screen.MenuChecksheet> {
            MenuChecksheetScreen(
                onProcessSelect = { type ->
                    navController.navigate(Screen.FormChecksheet(type.name))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.FormChecksheet>(
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.FormChecksheet>()
            val tipeProses = TipeProses.valueOf(route.tipeProses)

            ChecksheetScreen(
                tipeProses = tipeProses,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Screen.MenuLaporan> {
            com.primaraya.inspectra.fitur.laporan.ui.MenuLaporanScreen(
                onProcessSelect = { type ->
                    navController.navigate(Screen.FormLaporan(type.name))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.FormLaporan>(
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.FormLaporan>()
            val tipeProses = TipeProses.valueOf(route.tipeProses)

            if (tipeProses == TipeProses.CUTTING) {
                CuttingScreen(
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                com.primaraya.inspectra.fitur.laporan.ui.LaporanProduksiScreen(
                    tipeProses = tipeProses.name,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable<Screen.MasterData> {
            MasterDataScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
