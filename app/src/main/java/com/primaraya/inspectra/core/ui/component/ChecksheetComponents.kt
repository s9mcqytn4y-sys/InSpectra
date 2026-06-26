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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoColumn("Diperiksa", "$totalDiperiksa Pcs", Color(0xFF475569))
            InfoColumn("OK", "$totalOk Pcs", Color(0xFF16A34A))
            InfoColumn("NG", "$totalNg Pcs", Color(0xFFDC2626))
            InfoColumn(
                "Rasio NG",
                String.format("%.1f%%", rasioNg),
                if (adaKuantitasTidakValid) Color(0xFFDC2626) else Color(0xFFD97706)
            )
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun PreviewChecksheetDialog(
    payload: PayloadChecksheet,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    sending: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tinjau & Kirim", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Periksa kembali data sebelum disimpan ke server.")
                HorizontalDivider()
                
                DetailRow("Total Diperiksa", "${payload.totalDiperiksa} Pcs")
                DetailRow("Total OK", "${payload.totalOk} Pcs", Color(0xFF16A34A))
                DetailRow("Total NG", "${payload.totalNg} Pcs", Color(0xFFDC2626))
                DetailRow("Rasio NG", String.format("%.1f%%", payload.rasioNgGlobal), Color(0xFFD97706))

                if (payload.daftarPart.any { it.jumlahNg > it.jumlahDiperiksa }) {
                    Text(
                        "Peringatan: Ada jumlah NG yang tidak valid.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !sending,
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kirim Sekarang")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, color: Color = Color(0xFF1E293B)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
