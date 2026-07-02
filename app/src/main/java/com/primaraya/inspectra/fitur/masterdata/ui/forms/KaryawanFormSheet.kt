package com.primaraya.inspectra.fitur.masterdata.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.common.LineProcess
import com.primaraya.inspectra.core.common.TipePekerja
import com.primaraya.inspectra.core.ui.component.AppDropdownField
import com.primaraya.inspectra.fitur.masterdata.ui.KaryawanFormState

@Composable
fun KaryawanFormSheet(
    state: KaryawanFormState,
    onDismiss: () -> Unit,
    onUpdate: (KaryawanFormState) -> Unit,
    onSave: (KaryawanFormState) -> Unit,
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
            text = if (state.id == null) "Tambah Pekerja Baru" else "Ubah Data Pekerja",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.namaLengkap,
            onValueChange = { onUpdate(state.copy(namaLengkap = it)) },
            label = { Text("Nama Lengkap") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        AppDropdownField(
            label = "Tipe Pekerja",
            value = state.tipePekerja.name,
            options = TipePekerja.entries.map { it.name },
            onSelected = { 
                val newTipe = TipePekerja.valueOf(it)
                onUpdate(state.copy(
                    tipePekerja = newTipe,
                    // Domain Rule: PKL doesn't have No Reg
                    noReg = if (newTipe == TipePekerja.PKL) "" else state.noReg
                )) 
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Reactive Validation: Only show No Reg if KARYAWAN
        if (state.tipePekerja == TipePekerja.KARYAWAN) {
            OutlinedTextField(
                value = state.noReg,
                onValueChange = { onUpdate(state.copy(noReg = it)) },
                label = { Text("Nomor Registrasi (No Reg)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("MANDATORY for Karyawan") },
                singleLine = true,
                isError = state.noReg.isBlank()
            )
            if (state.noReg.isBlank()) {
                Text(
                    "No Reg wajib diisi untuk karyawan internal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        AppDropdownField(
            label = "Line / Proses Utama",
            value = state.lineProcess.name,
            options = LineProcess.entries.map { it.name },
            onSelected = { onUpdate(state.copy(lineProcess = LineProcess.valueOf(it))) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = state.aktif,
                onCheckedChange = { onUpdate(state.copy(aktif = it)) }
            )
            Text("Pekerja Aktif")
        }

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
                enabled = !isSaving && state.namaLengkap.isNotBlank() && (state.tipePekerja == TipePekerja.PKL || state.noReg.isNotBlank()),
                shape = MaterialTheme.shapes.small
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Simpan Data")
            }
        }
    }
}
