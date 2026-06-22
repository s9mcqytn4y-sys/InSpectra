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
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataScreen
import com.primaraya.inspectra.fitur.splash.SplashScreen

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

@Composable
fun MainDashboard(
    onChecksheetClick: () -> Unit,
    onMasterDataClick: () -> Unit,
    appViewModel: AppViewModel = viewModel()
) {
    val isSchemaCompatible by appViewModel.isSchemaCompatible.collectAsStateWithLifecycle()
    val bootstrap by appViewModel.bootstrap.collectAsStateWithLifecycle()

    AppResponsiveContent { isTablet, contentModifier ->
        Column(
            modifier = contentModifier.padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (!isSchemaCompatible) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Versi Database Tidak Cocok", fontWeight = FontWeight.Bold)
                            Text("Aplikasi mungkin tidak berfungsi dengan benar. Harap hubungi Admin IT.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            Text(
                text = "Workspace QC InSpectra",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Input diperiksa terhadap data acuan server sebelum dikirim.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isTablet) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    DashboardModuleCard(
                        title = "Lembar Periksa",
                        subtitle = "Inspeksi harian Press, Sewing, dan Cutting",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        onClick = onChecksheetClick,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleCard(
                        title = "Data Induk",
                        subtitle = "Part, material, supplier, spesifikasi, dan defect",
                        icon = Icons.Default.Dataset,
                        onClick = onMasterDataClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                DashboardModuleCard(
                    title = "Lembar Periksa",
                    subtitle = "Inspeksi harian Press, Sewing, dan Cutting",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    onClick = onChecksheetClick
                )
                DashboardModuleCard(
                    title = "Data Induk",
                    subtitle = "Part, material, supplier, spesifikasi, dan defect",
                    icon = Icons.Default.Dataset,
                    onClick = onMasterDataClick
                )
            }
        }
    }
}

@Composable
private fun DashboardModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary
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
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp).size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
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
    AppResponsiveContent { isTablet, contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .padding(top = 24.dp)
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
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isTablet) 3 else 2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(prosesChecksheetAktif, key = { it.name }) { proses ->
                ProcessCard(
                    proses = proses,
                    onClick = { onProcessSelect(proses) }
                )
            }
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
        TipeProses.CUTTING -> "Pemeriksaan material dan hasil cutting"
        else -> ""
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )

            Column {
                Text(
                    text = proses.labelIndonesia(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
