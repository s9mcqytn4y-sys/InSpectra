package com.primaraya.inspectra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.primaraya.inspectra.domain.model.TipeProses
import com.primaraya.inspectra.ui.screens.checksheet.ChecksheetScreen
import com.primaraya.inspectra.ui.screens.checksheet.ChecksheetViewModel
import com.primaraya.inspectra.ui.screens.checksheet.labelIndonesia
import com.primaraya.inspectra.ui.screens.splash.SplashScreen

enum class NavState { SPLASH, DASHBOARD, E_CHECKSHEETS, FORM_INSPEKSI }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by rememberSaveable { mutableStateOf(NavState.SPLASH) }
            var prosesTerpilih by rememberSaveable { mutableStateOf(TipeProses.PRESS) }
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
                                prosesTerpilih = type
                                currentScreen = NavState.FORM_INSPEKSI
                            },
                            onBack = { currentScreen = NavState.DASHBOARD }
                        )
                        NavState.FORM_INSPEKSI -> ChecksheetScreen(
                            tipeProses = prosesTerpilih,
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
                    Text("Modul E-Checksheets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Inspeksi QC lokal (Fase 1: Validasi Referensi)", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun EChecksheetsNavigationMenu(onProcessSelect: (TipeProses) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Pilih Komoditas Inspeksi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1A365D))
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(TipeProses.values()) { proses ->
                val icon = when(proses) {
                    TipeProses.PRESS -> Icons.Default.PrecisionManufacturing
                    TipeProses.SEWING -> Icons.Default.Layers
                    TipeProses.CUTTING -> Icons.Default.ContentCut
                    TipeProses.MATERIAL -> Icons.Default.Inventory
                    TipeProses.PASS_THROUGH -> Icons.Default.DoubleArrow
                    TipeProses.CONSUMABLE -> Icons.Default.FormatPaint
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onProcessSelect(proses) }, 
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color(0xFF1A365D), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(proses.labelIndonesia(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A365D))
                    }
                }
            }
        }
    }
}
