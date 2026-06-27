package com.primaraya.inspectra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.ui.component.AppResponsiveContent
import com.primaraya.inspectra.core.ui.theme.InSpectraTheme
import com.primaraya.inspectra.core.ui.viewmodel.AppViewModel
import com.primaraya.inspectra.core.ui.viewmodel.CURRENT_SCHEMA_REVISION
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses
import com.primaraya.inspectra.fitur.checksheet.ui.ChecksheetScreen
import com.primaraya.inspectra.fitur.checksheet.ui.labelIndonesia
import com.primaraya.inspectra.fitur.cutting.ui.CuttingScreen
import com.primaraya.inspectra.fitur.dashboard.ui.DashboardScreen
import com.primaraya.inspectra.fitur.dashboard.ui.MenuChecksheetScreen
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataScreen
import com.primaraya.inspectra.fitur.splash.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

enum class NavState { 
    SPLASH, 
    DASHBOARD, 
    MENU_CHECKSHEET, 
    FORM_CHECKSHEET, 
    MASTER_DATA 
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by rememberSaveable { mutableStateOf(NavState.SPLASH) }
            var prosesTerpilih by rememberSaveable { mutableStateOf(TipeProses.PRESS) }

            InSpectraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        NavState.SPLASH -> SplashScreen(
                            onSelesai = {
                                currentScreen = NavState.DASHBOARD
                            }
                        )
                        NavState.DASHBOARD -> DashboardScreen(
                            onChecksheetClick = {
                                currentScreen = NavState.MENU_CHECKSHEET
                            },
                            onMasterDataClick = {
                                currentScreen = NavState.MASTER_DATA
                            }
                        )
                        NavState.MENU_CHECKSHEET -> MenuChecksheetScreen(
                            onProcessSelect = { type ->
                                prosesTerpilih = type
                                currentScreen = NavState.FORM_CHECKSHEET
                            },
                            onBack = { currentScreen = NavState.DASHBOARD }
                        )
                        NavState.FORM_CHECKSHEET -> if (prosesTerpilih == TipeProses.CUTTING) {
                            CuttingScreen(onBackClick = { currentScreen = NavState.MENU_CHECKSHEET })
                        } else {
                            ChecksheetScreen(
                                tipeProses = prosesTerpilih,
                                onBackClick = { currentScreen = NavState.MENU_CHECKSHEET }
                            )
                        }
                        NavState.MASTER_DATA -> MasterDataScreen(
                            onBackClick = { currentScreen = NavState.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}
