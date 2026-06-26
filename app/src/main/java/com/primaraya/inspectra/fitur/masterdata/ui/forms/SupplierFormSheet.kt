package com.primaraya.inspectra.fitur.masterdata.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.fitur.masterdata.ui.SupplierFormState

@Composable
fun SupplierFormSheet(
    state: SupplierFormState,
    onDismiss: () -> Unit,
    onUpdate: (SupplierFormState) -> Unit,
    onSave: (SupplierFormState) -> Unit,
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
            text = if (state.id == null) "Tambah Supplier Baru" else "Ubah Supplier",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.kodeSupplier,
            onValueChange = { onUpdate(state.copy(kodeSupplier = it)) },
            label = { Text("Kode Supplier") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.namaSupplier,
            onValueChange = { onUpdate(state.copy(namaSupplier = it)) },
            label = { Text("Nama Supplier") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.kategori,
            onValueChange = { onUpdate(state.copy(kategori = it)) },
            label = { Text("Kategori") },
            placeholder = { Text("Contoh: Material, Benang, Jasa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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
                enabled = !isSaving && state.namaSupplier.isNotBlank(),
                shape = MaterialTheme.shapes.small
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Simpan Supplier")
            }
        }
    }
}
