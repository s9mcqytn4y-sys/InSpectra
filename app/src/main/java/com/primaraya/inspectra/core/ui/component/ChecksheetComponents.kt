package com.primaraya.inspectra.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadChecksheet

@Composable
fun RingkasanAtas(
    totalDiperiksa: Int,
    totalOk: Int,
    totalNg: Int,
    rasioNg: Float,
    adaKuantitasTidakValid: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Diperiksa", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("$totalDiperiksa Pcs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("OK", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("$totalOk Pcs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NG", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("$totalNg Pcs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rasio NG", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    String.format("%.1f%%", rasioNg),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (adaKuantitasTidakValid) Color(0xFFDC2626) else Color(0xFFD97706)
                )
            }
        }
    }
}

@Composable
fun KonfirmasiKirimChecksheetDialog(
    payload: PayloadChecksheet,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
        },
        title = {
            Text(
                text = "Kirim Checksheet?",
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pastikan data pemeriksaan sudah sesuai sebelum dikirim.")

                HorizontalDivider()

                Text("Proses: ${payload.tipeProses}")
                Text("Part diisi: ${payload.daftarPart.size}")
                Text("Diperiksa: ${payload.totalDiperiksa} pcs")
                Text("OK: ${payload.totalOk} pcs")
                Text("NG: ${payload.totalNg} pcs")
                Text("Rasio NG: ${payload.rasioNgGlobal}%")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Kirim")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tinjau Lagi")
            }
        }
    )
}
