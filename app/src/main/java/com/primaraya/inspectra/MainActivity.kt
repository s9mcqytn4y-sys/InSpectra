package com.primaraya.inspectra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
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

enum class NavState { 
    SPLASH, 
    DASHBOARD, 
    MENU_CHECKSHEET, 
    FORM_CHECKSHEET, 
    MASTER_DATA 
}

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
                        NavState.DASHBOARD -> MainDashboard(
                            onChecksheetClick = {
                                currentScreen = NavState.MENU_CHECKSHEET
                            },
                            onMasterDataClick = {
                                currentScreen = NavState.MASTER_DATA
                            }
                        )
                        NavState.MENU_CHECKSHEET -> MenuChecksheet(
                            onProcessSelect = { type ->
                                prosesTerpilih = type
                                currentScreen = NavState.FORM_CHECKSHEET
                            },
                            onBack = { currentScreen = NavState.DASHBOARD }
                        )
                        NavState.FORM_CHECKSHEET -> ChecksheetScreen(
                            tipeProses = prosesTerpilih,
                            viewModel = checksheetViewModel,
                            onBackClick = { currentScreen = NavState.MENU_CHECKSHEET }
                        )
                        NavState.MASTER_DATA -> {
                            // TODO: Master Data Screen
                            Text("Master Data Module (Coming Soon)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainDashboard(
    onChecksheetClick: () -> Unit,
    onMasterDataClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "InSpectra Workspace",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1A365D)
        )

        DashboardModuleCard(
            title = "E-Checksheet",
            subtitle = "Inspeksi harian Press, Sewing, dan Cutting",
            icon = Icons.AutoMirrored.Filled.Assignment,
            onClick = onChecksheetClick
        )

        DashboardModuleCard(
            title = "Master Data",
            subtitle = "Kelola part, material, supplier, spec, dan defect",
            icon = Icons.Default.Dataset,
            onClick = onMasterDataClick
        )
    }
}

@Composable
private fun DashboardModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF1A365D)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

private val prosesChecksheetAktif = listOf(
    TipeProses.PRESS,
    TipeProses.SEWING,
    TipeProses.CUTTING
)

@Composable
fun MenuChecksheet(
    onProcessSelect: (TipeProses) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }

            Text(
                text = "Pilih Proses Inspeksi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A365D)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(prosesChecksheetAktif) { proses ->
                ProcessCard(
                    proses = proses,
                    onClick = { onProcessSelect(proses) }
                )
            }
        }
    }
}

@Composable
private fun ProcessCard(
    proses: TipeProses,
    onClick: () -> Unit
) {
    val icon = when (proses) {
        TipeProses.PRESS -> Icons.Default.PrecisionManufacturing
        TipeProses.SEWING -> Icons.Default.Layers
        TipeProses.CUTTING -> Icons.Default.ContentCut
        else -> Icons.Default.Inventory
    }

    val subtitle = when (proses) {
        TipeProses.PRESS -> "Part press dan carpet"
        TipeProses.SEWING -> "Felt, protector, seat cover"
        TipeProses.CUTTING -> "Material check dan hasil cutting"
        else -> ""
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1A365D),
                modifier = Modifier.size(42.dp)
            )

            Column {
                Text(
                    text = proses.labelIndonesia(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A365D)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
