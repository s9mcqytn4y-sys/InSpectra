package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.InspectraEliteModal
import com.primaraya.inspectra.fitur.masterdata.domain.*
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataContract
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PartDetailModal(
    part: MasterPartDto,
    relationState: MasterDataContract.PartRelationState,
    onDismiss: () -> Unit,
    onAddDefect: () -> Unit,
    onRemoveDefect: (String) -> Unit,
    onAddMaterial: () -> Unit,
    onRemoveMaterial: (String) -> Unit
) {
    InspectraEliteModal(
        title = "Detail Part: ${part.uniq_no}",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Basic Info Section
            DetailSectionElite(title = "Informasi Dasar", icon = Icons.Default.Info) {
                DetailRowElite("Nama Part", part.nama_part)
                DetailRowElite("Nomor Part", part.part_no ?: "-")
                DetailRowElite("Model", part.model ?: "-")
                DetailRowElite("Customer", part.customer ?: "-")
                DetailRowElite("Komoditas", part.komoditas)
            }

            // Quality Rules Section
            DetailSectionElite(title = "Aturan Kualitas", icon = Icons.Default.Gavel) {
                DetailRowElite("Total / Kanban", part.total_item_per_kanban?.toString() ?: "0 Pcs")
                DetailRowElite("Sample / Kanban", part.sample_item_per_kanban?.toString() ?: "0 Pcs")
                DetailRowElite("Catatan Sample", part.sample_cycle_note ?: "-")
            }

            // Material Relations
            RelationSectionElite(
                title = "Material Terkait",
                icon = Icons.Default.Inventory2,
                data = relationState.materials,
                onAdd = onAddMaterial,
                emptyMsg = "Belum ada material tertaut."
            ) { relations: ImmutableList<MasterPartMaterialDto> ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    relations.forEach { rel ->
                        RelationItemElite(
                            title = rel.label_material,
                            subtitle = "Uniq No: ${rel.uniq_no}",
                            onRemove = { rel.id?.let { onRemoveMaterial(it) } }
                        )
                    }
                }
            }

            // Defect Relations
            RelationSectionElite(
                title = "Defect Proses",
                icon = Icons.Default.BugReport,
                data = relationState.defects,
                onAdd = onAddDefect,
                emptyMsg = "Belum ada defect proses tertaut."
            ) { relations: ImmutableList<MasterPartDefectDto> ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    relations.forEach { rel ->
                        RelationItemElite(
                            title = rel.id_defect,
                            onRemove = { rel.id?.let { onRemoveDefect(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialDetailModal(
    material: MasterMaterialDto,
    relationState: MasterDataContract.MaterialRelationState,
    onDismiss: () -> Unit,
    onAddDefect: () -> Unit,
    onRemoveDefect: (String) -> Unit
) {
    InspectraEliteModal(
        title = "Detail Material",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DetailSectionElite(title = "Informasi Dasar", icon = Icons.Default.Info) {
                DetailRowElite("Nama Material", material.nama_material)
                DetailRowElite("Supplier", material.supplier ?: "-")
                DetailRowElite("Satuan", material.satuan ?: "-")
                DetailRowElite("Spesifikasi", material.spec ?: "-")
            }

            DetailSectionElite(title = "Spesifikasi Fisik", icon = Icons.Default.SettingsInputComponent) {
                DetailRowElite("Lebar Roll", "${material.lebar_roll_cm ?: 0} cm")
                DetailRowElite("Panjang Roll", "${material.panjang_roll_cm ?: 0} cm")
                DetailRowElite("Tebal", "${material.tebal_mm ?: 0} mm")
                DetailRowElite("Gramasi", "${material.gramasi_gsm ?: material.berat_gsm ?: 0} gsm")
                DetailRowElite("Warna", material.warna ?: "-")
            }

            RelationSectionElite(
                title = "Defect Bawaan Material",
                icon = Icons.Default.Warning,
                data = relationState.defects,
                onAdd = onAddDefect,
                emptyMsg = "Belum ada defect material."
            ) { relations: ImmutableList<MasterMaterialDefectDto> ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    relations.forEach { rel ->
                        RelationItemElite(
                            title = rel.id_defect,
                            onRemove = { rel.id?.let { onRemoveDefect(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierDetailModal(
    supplier: MasterSupplierDto,
    onDismiss: () -> Unit
) {
    InspectraEliteModal(
        title = "Detail Supplier",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DetailSectionElite(title = "Informasi Bisnis", icon = Icons.Default.Business) {
                DetailRowElite("Nama Supplier", supplier.nama_supplier)
                DetailRowElite("Kode", supplier.kode_supplier ?: "-")
                DetailRowElite("Kategori", supplier.kategori ?: "UMUM")
                DetailRowElite("Status", if (supplier.aktif) "AKTIF" else "NONAKTIF")
            }
        }
    }
}

@Composable
fun DefectDetailModal(
    defect: MasterDefectDto,
    onDismiss: () -> Unit
) {
    InspectraEliteModal(
        title = "Detail Defect",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DetailSectionElite(title = "Katalog Defect", icon = Icons.Default.BugReport) {
                DetailRowElite("Nama Defect", defect.nama_defect)
                DetailRowElite("ID Defect", defect.id_defect)
                DetailRowElite("Kategori", defect.kategori)
                DetailRowElite("Status", if (defect.aktif) "AKTIF" else "NONAKTIF")
            }
        }
    }
}

@Composable
private fun DetailSectionElite(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun DetailRowElite(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun <T> RelationSectionElite(
    title: String,
    icon: ImageVector,
    data: AsyncData<ImmutableList<T>>,
    onAdd: () -> Unit,
    emptyMsg: String,
    content: @Composable (ImmutableList<T>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambah")
            }
        }
        
        when (data) {
            is AsyncData.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            is AsyncData.Success -> {
                if (data.data.isEmpty()) {
                    Text(emptyMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    content(data.data)
                }
            }
            is AsyncData.Error -> Text("Gagal memuat data", color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
    }
}

@Composable
private fun RelationItemElite(
    title: String,
    subtitle: String? = null,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}
