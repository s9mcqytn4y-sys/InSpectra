package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.ui.component.AppStatusBadge
import com.primaraya.inspectra.core.ui.component.NadaStatusAplikasi
import com.primaraya.inspectra.fitur.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataContract
import com.primaraya.inspectra.fitur.masterdata.ui.components.AsyncRelationList

@Composable
fun MaterialMasterCard(
    material: MasterMaterialDto,
    detailState: MasterDataContract.MaterialRelationState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleDetail: () -> Unit,
    onAddDefect: () -> Unit,
    onRemoveDefect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.nama_material,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Supplier: ${material.supplier ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppStatusBadge(
                    label = material.satuan ?: "UNKNOWN",
                    nada = NadaStatusAplikasi.INFO
                )
                if (material.supplier == "UNKNOWN") {
                    AppStatusBadge(
                        label = "Perlu Verifikasi Supplier",
                        nada = NadaStatusAplikasi.PERINGATAN
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Spesifikasi Detail:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (material.lebar_roll_cm != null) Text("Lebar: ${material.lebar_roll_cm} cm", style = MaterialTheme.typography.bodySmall)
                if (material.panjang_roll_cm != null) Text("Panjang: ${material.panjang_roll_cm} cm", style = MaterialTheme.typography.bodySmall)
                if (material.tebal_mm != null) Text("Tebal: ${material.tebal_mm} mm", style = MaterialTheme.typography.bodySmall)
                if (material.berat_gsm != null) Text("Berat: ${material.berat_gsm} gsm", style = MaterialTheme.typography.bodySmall)
                if (material.gramasi_gsm != null) Text("Gramasi: ${material.gramasi_gsm} gsm", style = MaterialTheme.typography.bodySmall)
                if (!material.warna.isNullOrBlank()) Text("Warna: ${material.warna}", style = MaterialTheme.typography.bodySmall)
                if (!material.catatan_spesifikasi.isNullOrBlank()) {
                    Text("Catatan: ${material.catatan_spesifikasi}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onToggleDetail,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (detailState.expanded) "Tutup Detail" else "Defect Bawaan Material")
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (detailState.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = detailState.expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = "Daftar Defect Bawaan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AsyncRelationList(detailState.defects) { relations ->
                        if (relations.isEmpty()) {
                            Text("Belum ada defect material", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        relations.forEach { relation ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(relation.id_defect, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onRemoveDefect(relation.id.orEmpty()) }) {
                                    Icon(
                                        Icons.Default.LinkOff,
                                        contentDescription = "Hapus",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        TextButton(onClick = onAddDefect) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah Defect Material")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Digunakan oleh Part",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AsyncRelationList(detailState.usageParts) { usages ->
                        if (usages.isEmpty()) {
                            Text("Material ini belum digunakan oleh part manapun", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        usages.forEach { usage ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(usage.uniq_no, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text("(${usage.label_material})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
