package com.primaraya.inspectra.fitur.masterdata.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.primaraya.inspectra.core.ui.component.AppDropdownField
import com.primaraya.inspectra.fitur.masterdata.ui.PartFormState
import java.io.File
import java.io.FileOutputStream

@Composable
fun PartFormSheet(
    state: PartFormState,
    onDismiss: () -> Unit,
    onUpdate: (PartFormState) -> Unit,
    onSave: (PartFormState) -> Unit,
    onImageSelect: (File) -> Unit,
    isSaving: Boolean
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = File(context.cacheDir, "part_upload_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            onImageSelect(file)
        }
    }

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
            text = if (state.id == null) "Tambah Part Baru" else "Ubah Part",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val imageSource = state.lokasiGambarLokal ?: state.lokasiGambarRemote
                
                if (imageSource != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageSource)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { launcher.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Ganti Foto", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.clickable { launcher.launch("image/*") }
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                        Text("Tambah Foto Part", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.uniqNo,
            onValueChange = { onUpdate(state.copy(uniqNo = it)) },
            label = { Text("Unique No (Uniq No)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.id == null, // Uniq No usually immutable
            singleLine = true
        )

        OutlinedTextField(
            value = state.partNo,
            onValueChange = { onUpdate(state.copy(partNo = it)) },
            label = { Text("Part No") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.namaPart,
            onValueChange = { onUpdate(state.copy(namaPart = it)) },
            label = { Text("Nama Part") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.model,
            onValueChange = { onUpdate(state.copy(model = it)) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.customer,
            onValueChange = { onUpdate(state.copy(customer = it)) },
            label = { Text("Customer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        AppDropdownField(
            label = "Komoditas",
            value = state.komoditas,
            options = listOf("PRESS", "SEWING", "CUTTING", "MATERIAL", "PASS_THROUGH", "CONSUMABLE"),
            onSelected = { onUpdate(state.copy(komoditas = it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.totalItemPerKanban,
                onValueChange = { onUpdate(state.copy(totalItemPerKanban = it)) },
                label = { Text("Total / Kanban") },
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = state.sampleItemPerKanban,
                onValueChange = { onUpdate(state.copy(sampleItemPerKanban = it)) },
                label = { Text("Sample / Kanban") },
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = state.sampleCycleNote,
            onValueChange = { onUpdate(state.copy(sampleCycleNote = it)) },
            label = { Text("Catatan Siklus Sample") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
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
                enabled = !isSaving && state.uniqNo.isNotBlank() && state.namaPart.isNotBlank(),
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
