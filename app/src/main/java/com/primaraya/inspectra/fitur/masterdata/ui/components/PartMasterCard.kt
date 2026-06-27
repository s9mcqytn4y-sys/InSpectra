package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.AppStatusBadge
import com.primaraya.inspectra.core.ui.component.NadaStatusAplikasi
import com.primaraya.inspectra.fitur.masterdata.domain.MasterPartDto
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataContract
import com.primaraya.inspectra.fitur.masterdata.ui.components.AsyncRelationList

@Composable
fun PartMasterCard(
    part: MasterPartDto,
    detailState: MasterDataContract.PartRelationState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleDetail: () -> Unit,
    onAddDefect: () -> Unit,
    onRemoveDefect: (String) -> Unit,
    onAddMaterial: () -> Unit,
    onRemoveMaterial: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (part.lokasi_gambar != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(part.lokasi_gambar)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = part.uniq_no,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Text(
                        text = part.nama_part,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${part.part_no ?: "-"} · Model ${part.model ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppStatusBadge(
                    label = part.komoditas,
                    nada = NadaStatusAplikasi.PERINGATAN
                )
                PartKesiapanBadge(part)
            }

            Spacer(Modifier.height(16.dp))
            
            // Summary info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Material: ${part.jumlah_material ?: 0} jenis",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Defect Proses: ${part.jumlah_defect_proses ?: 0} jenis",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Defect Material: ${part.jumlah_defect_material ?: 0} jenis",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onToggleDetail,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (detailState.expanded) "Tutup Detail" else "Lihat Detail & Relasi")
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (detailState.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = detailState.expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Material Digunakan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AsyncRelationList(detailState.materials) { materials ->
                        if (materials.isEmpty()) {
                            Text("Belum ada material tertaut", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        materials.forEach { rel ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(rel.label_material, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { onRemoveMaterial(rel.id ?: "") }) {
                                    Icon(Icons.Default.LinkOff, contentDescription = "Hapus", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        TextButton(onClick = onAddMaterial) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah Material")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Defect Proses Part",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AsyncRelationList(detailState.defects) { defects ->
                        if (defects.isEmpty()) {
                            Text("Belum ada defect proses tertaut", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        defects.forEach { rel ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(rel.id_defect, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onRemoveDefect(rel.id ?: "") }) {
                                    Icon(Icons.Default.LinkOff, contentDescription = "Hapus", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        TextButton(onClick = onAddDefect) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah Defect Proses")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Defect dari Material",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AsyncRelationList(detailState.effectiveDefects) { effectiveDefects ->
                        val materialDefects = effectiveDefects.filter { it.sumber_defect == "MATERIAL" }
                        if (materialDefects.isEmpty()) {
                            if (part.jumlah_material == 0) {
                                Text(
                                    text = "Material belum terhubung. Defect material belum dapat digunakan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Text("Belum ada defect dari material", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                        materialDefects.forEach { rel ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(rel.nama_defect ?: rel.id_defect, style = MaterialTheme.typography.bodyMedium)
                                    Text("Sumber: ${rel.nama_material ?: "-"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartKesiapanBadge(part: MasterPartDto) {
    val (label, nada) = when {
        part.status_kelengkapan == "LENGKAP" -> "Siap Input" to NadaStatusAplikasi.SUKSES
        part.butuh_review -> "Butuh Review" to NadaStatusAplikasi.BAHAYA
        else -> "Belum Lengkap" to NadaStatusAplikasi.PERINGATAN
    }
    AppStatusBadge(label = label, nada = nada)
}
