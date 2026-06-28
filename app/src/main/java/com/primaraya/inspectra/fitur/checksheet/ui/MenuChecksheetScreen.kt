package com.primaraya.inspectra.fitur.checksheet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.ui.component.AppResponsiveContent
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses

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
            Text(
                "Pilih Departemen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val list = listOf(TipeProses.PRESS, TipeProses.SEWING)

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTablet) 3 else 1),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(list, key = { it.name }) { proses ->
                    DepartemenCard(proses, onClick = { onProcessSelect(proses) })
                }
            }
        }
    }
}

@Composable
private fun DepartemenCard(proses: TipeProses, onClick: () -> Unit) {
    val icon = when (proses) {
        TipeProses.PRESS -> Icons.Default.PrecisionManufacturing
        TipeProses.SEWING -> Icons.Default.Layers
        TipeProses.CUTTING -> Icons.Default.ContentCut
        else -> Icons.Default.Inventory
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = proses.labelIndonesia(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Akses form pemeriksaan harian",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
