package com.primaraya.inspectra.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadChecksheet

@Composable
fun RingkasanAtas(
    totalDiperiksa: Int,
    totalOk: Int,
    totalNg: Int,
    rasioNg: Float,
    adaKuantitasTidakValid: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoColumn("Diperiksa", "$totalDiperiksa Pcs", Color(0xFF475569))
            VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = Color(0xFFF1F5F9))
            InfoColumn("OK", "$totalOk Pcs", Color(0xFF16A34A))
            VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = Color(0xFFF1F5F9))
            InfoColumn("NG", "$totalNg Pcs", Color(0xFFDC2626))
            VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = Color(0xFFF1F5F9))
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
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
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
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 24.dp),
        title = {
            Column {
                Text(
                    text = "Tinjau & Kirim",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Periksa kembali data sebelum disimpan ke server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailSummaryItem("Diperiksa", "${payload.totalDiperiksa}", Color(0xFF475569))
                        DetailSummaryItem("OK", "${payload.totalOk}", Color(0xFF16A34A))
                        DetailSummaryItem("NG", "${payload.totalNg}", Color(0xFFDC2626))
                        DetailSummaryItem("Rasio", String.format("%.1f%%", payload.rasioNgGlobal), Color(0xFFD97706))
                    }
                }

                Text(
                    "Rincian Temuan per Part:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(payload.daftarPart) { part ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(part.uniqNo, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
                                        Text("${part.jumlahNg} NG", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (part.daftarDefectNg.isNotEmpty()) {
                                        Text(
                                            text = part.daftarDefectNg.joinToString { "${it.namaDefect} (${it.jumlahNg})" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (payload.daftarPart.any { it.jumlahNg > it.jumlahDiperiksa }) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Peringatan: Ada jumlah NG yang tidak valid.",
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !sending,
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A365D))
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kirim Sekarang", fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Batal", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun DetailSummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = color)
    }
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
