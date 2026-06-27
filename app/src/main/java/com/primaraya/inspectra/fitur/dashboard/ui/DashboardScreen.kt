package com.primaraya.inspectra.fitur.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.ui.component.AppResponsiveContent
import com.primaraya.inspectra.core.ui.viewmodel.AppViewModel
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses
import com.primaraya.inspectra.fitur.checksheet.ui.labelIndonesia

@Composable
fun DashboardScreen(
    onChecksheetClick: () -> Unit,
    onMasterDataClick: () -> Unit,
    viewModel: AppViewModel = viewModel()
) {
    val isSchemaCompatible by viewModel.isSchemaCompatible.collectAsStateWithLifecycle()

    AppResponsiveContent { isTablet, contentModifier ->
        Column(
            modifier = contentModifier.padding(top = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HeaderDashboard()

            if (!isSchemaCompatible) {
                SchemaWarning()
            }

            Text(
                text = "Workspace Utama",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1E293B)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModuleCard(
                    title = "Checksheet",
                    subtitle = "Inspeksi Produksi",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    color = Color(0xFF1A365D),
                    onClick = onChecksheetClick,
                    modifier = Modifier.weight(1f)
                )
                ModuleCard(
                    title = "Master Data",
                    subtitle = "Kelola Referensi",
                    icon = Icons.Default.Dataset,
                    color = Color(0xFFD97706),
                    onClick = onMasterDataClick,
                    modifier = Modifier.weight(1f)
                )
            }
            
            QuickStatsSection()
        }
    }
}

@Composable
private fun HeaderDashboard() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(com.primaraya.inspectra.R.string.dashboard_workspace_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A365D),
                letterSpacing = (-0.5).sp
            )
            Text(
                text = stringResource(com.primaraya.inspectra.R.string.dashboard_workspace_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF475569))
            }
        }
    }
}

@Composable
private fun SchemaWarning() {
    Surface(
        color = Color(0xFFFEF2F2),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
            Column {
                Text("Database Outdated", fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                Text("Versi skema tidak sesuai. Harap update aplikasi.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF991B1B))
            }
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = color,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun QuickStatsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(com.primaraya.inspectra.R.string.dashboard_quick_summary),
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatItem("Checksheet", "12", Icons.Default.DoneAll, Color(0xFF16A34A), Modifier.weight(1f))
            StatItem("Defect", "4", Icons.Default.ErrorOutline, Color(0xFFDC2626), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Column {
                Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun MenuChecksheetScreen(
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text("Pilih Departemen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }

            val list = listOf(TipeProses.PRESS, TipeProses.SEWING, TipeProses.CUTTING)

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTablet) 3 else 1),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(list) { proses ->
                    DepartemenCard(proses, onClick = { onProcessSelect(proses) })
                }
            }
        }
    }
}

@Composable
private fun DepartemenCard(proses: TipeProses, onClick: () -> Unit) {
    val (icon, color) = when (proses) {
        TipeProses.PRESS -> Icons.Default.PrecisionManufacturing to Color(0xFF1E293B)
        TipeProses.SEWING -> Icons.Default.Layers to Color(0xFF334155)
        TipeProses.CUTTING -> Icons.Default.ContentCut to Color(0xFF475569)
        else -> Icons.Default.Inventory to Color.Gray
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = proses.labelIndonesia(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Akses form pemeriksaan harian",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCBD5E1))
        }
    }
}
