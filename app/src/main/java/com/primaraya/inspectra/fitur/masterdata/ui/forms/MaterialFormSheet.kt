package com.primaraya.inspectra.fitur.masterdata.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.ui.component.AppDropdownField
import com.primaraya.inspectra.fitur.masterdata.ui.MaterialFormState

@Composable
fun MaterialFormSheet(
    state: MaterialFormState,
    onDismiss: () -> Unit,
    onUpdate: (MaterialFormState) -> Unit,
    onSave: (MaterialFormState) -> Unit,
    onOpenSupplierPicker: () -> Unit,
    isSaving: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (state.id == null) "Tambah Material Baru" else "Ubah Material",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.namaMaterial,
            onValueChange = { onUpdate(state.copy(namaMaterial = it)) },
            label = { Text("Nama Material") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Box(modifier = Modifier.fillMaxWidth().clickable { onOpenSupplierPicker() }) {
            OutlinedTextField(
                value = state.supplierNama,
                onValueChange = { },
                readOnly = true,
                enabled = false, // Allow clicks to pass to Box
                label = { Text("Supplier") },
                placeholder = { Text("Pilih Supplier...") },
                trailingIcon = {
                    Icon(Icons.Default.Store, contentDescription = "Pilih Supplier")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = state.spec,
            onValueChange = { onUpdate(state.copy(spec = it)) },
            label = { Text("Spesifikasi / Deskripsi Ringkas") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Text("Spesifikasi Fisik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.lebarCm,
                onValueChange = { onUpdate(state.copy(lebarCm = it)) },
                label = { Text("Lebar (cm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = state.panjangRollCm,
                onValueChange = { onUpdate(state.copy(panjangRollCm = it)) },
                label = { Text("Panjang Roll (cm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.tebalMm,
                onValueChange = { onUpdate(state.copy(tebalMm = it)) },
                label = { Text("Tebal (mm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = state.beratGsm,
                onValueChange = { onUpdate(state.copy(beratGsm = it)) },
                label = { Text("Berat (gsm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.gramasiGsm,
                onValueChange = { onUpdate(state.copy(gramasiGsm = it)) },
                label = { Text("Gramasi (gsm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = state.warna,
                onValueChange = { onUpdate(state.copy(warna = it)) },
                label = { Text("Warna") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = state.catatanSpesifikasi,
            onValueChange = { onUpdate(state.copy(catatanSpesifikasi = it)) },
            label = { Text("Catatan Spesifikasi") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        AppDropdownField(
            label = "Satuan Utama",
            value = state.satuan,
            options = listOf("PCS", "ROLL", "MTR", "KG", "GRAM", "CONES", "CAN", "SET", "LEMBAR", "UNKNOWN"),
            onSelected = { onUpdate(state.copy(satuan = it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Batal")
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { onSave(state) },
                enabled = !isSaving && state.namaMaterial.isNotBlank(),
                shape = MaterialTheme.shapes.small
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Simpan Material")
            }
        }
    }
}
