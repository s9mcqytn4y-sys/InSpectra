package com.primaraya.inspectra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.domain.model.ProcessType
import com.primaraya.inspectra.ui.screens.checksheet.ChecksheetScreen
import com.primaraya.inspectra.ui.screens.checksheet.ChecksheetViewModel
import com.primaraya.inspectra.ui.screens.splash.SplashScreen

enum class NavState { SPLASH, DASHBOARD, E_CHECKSHEETS, FORM_PRESS, FORM_SEWING }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Menggunakan rememberSaveable agar navigasi aman dari gangguan rotasi tablet
            var currentScreen by rememberSaveable { mutableStateOf(NavState.SPLASH) }
            val checksheetViewModel: ChecksheetViewModel = viewModel()

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8FAFC)) {
                    when (currentScreen) {
                        NavState.SPLASH -> SplashScreen(onHealthCheckPassed = {
                            currentScreen = NavState.DASHBOARD
                        })
                        NavState.DASHBOARD -> MainDashboard(onModuleClick = {
                            currentScreen = NavState.E_CHECKSHEETS
                        })
                        NavState.E_CHECKSHEETS -> EChecksheetsNavigationMenu(
                            onProcessSelect = { type ->
                                checksheetViewModel.loadChecksheetByProcess(type)
                                currentScreen = if (type == ProcessType.PRESS) NavState.FORM_PRESS else NavState.FORM_SEWING
                            },
                            onBack = { currentScreen = NavState.DASHBOARD }
                        )
                        NavState.FORM_PRESS -> ChecksheetScreen(
                            processType = ProcessType.PRESS,
                            viewModel = checksheetViewModel,
                            onBackClick = { currentScreen = NavState.E_CHECKSHEETS }
                        )
                        NavState.FORM_SEWING -> ChecksheetScreen(
                            processType = ProcessType.SEWING,
                            viewModel = checksheetViewModel,
                            onBackClick = { currentScreen = NavState.E_CHECKSHEETS }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainDashboard(onModuleClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("InSpectra Workspace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFF1A365D))
        
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onModuleClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A365D))
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White)
                Column {
                    Text("E-Checksheets Module", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Modul pelaporan digital terpadu Press & Sewing", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun EChecksheetsNavigationMenu(onProcessSelect: (ProcessType) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Pilih Jalur Proses", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Card(modifier = Modifier.fillMaxWidth().clickable { onProcessSelect(ProcessType.PRESS) }, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = Color(0xFF1A365D), modifier = Modifier.size(28.dp))
                Text("Proses PRESS (Inspeksi Carpet Box)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Card(modifier = Modifier.fillMaxWidth().clickable { onProcessSelect(ProcessType.SEWING) }, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF1A365D), modifier = Modifier.size(28.dp))
                Text("Proses SEWING (Inspeksi Felt Seat Back)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
