package com.primaraya.inspectra.fitur.laporan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.ui.component.AppResponsiveContent
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses
import com.primaraya.inspectra.fitur.checksheet.ui.labelIndonesia

@Composable
fun MenuLaporanScreen(
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
                Text("Pilih Modul Laporan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }

            // List 3 proses untuk pelaporan
            val list = listOf(TipeProses.PRESS, TipeProses.SEWING, TipeProses.CUTTING)

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTablet) 3 else 1),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(list) { proses ->
                    DepartemenLaporanCard(
                        proses = proses,
                        desc = if (proses == TipeProses.CUTTING) "Laporan detail material potong" else "Laporan produksi harian",
                        onClick = { onProcessSelect(proses) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DepartemenLaporanCard(proses: TipeProses, desc: String, onClick: () -> Unit) {
    val (icon, color) = when (proses) {
        TipeProses.PRESS -> Icons.Default.PrecisionManufacturing to MaterialTheme.colorScheme.onSurface
        TipeProses.SEWING -> Icons.Default.Layers to Color(0xFF334155)
        TipeProses.CUTTING -> Icons.Default.ContentCut to MaterialTheme.colorScheme.onSurfaceVariant
        else -> Icons.Default.Inventory to Color.Gray
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
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
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCBD5E1))
        }
    }
}
