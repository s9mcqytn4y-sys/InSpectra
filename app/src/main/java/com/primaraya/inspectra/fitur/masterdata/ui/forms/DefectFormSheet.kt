package com.primaraya.inspectra.fitur.masterdata.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.ui.component.AppDropdownField
import com.primaraya.inspectra.fitur.masterdata.ui.DefectFormState

@Composable
fun DefectFormSheet(
    state: DefectFormState,
    onDismiss: () -> Unit,
    onUpdate: (DefectFormState) -> Unit,
    onSave: (DefectFormState) -> Unit,
    isSaving: Boolean,
    isEdit: Boolean
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
            text = if (!isEdit) "Tambah Defect Baru" else "Ubah Defect",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.idDefect,
            onValueChange = { onUpdate(state.copy(idDefect = it)) },
            label = { Text("ID Defect / Kode") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isEdit,
            singleLine = true
        )

        OutlinedTextField(
            value = state.namaDefect,
            onValueChange = { onUpdate(state.copy(namaDefect = it)) },
            label = { Text("Nama Defect") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        AppDropdownField(
            label = "Kategori",
            value = state.kategori,
            options = listOf("PROSES", "MATERIAL"),
            onSelected = { onUpdate(state.copy(kategori = it)) },
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
                enabled = !isSaving && state.idDefect.isNotBlank() && state.namaDefect.isNotBlank(),
                shape = MaterialTheme.shapes.small
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Simpan Defect")
            }
        }
    }
}
