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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoColumn("Diperiksa", "$totalDiperiksa Pcs", MaterialTheme.colorScheme.onSurfaceVariant)
            VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            InfoColumn("OK", "$totalOk Pcs", MaterialTheme.colorScheme.tertiary)
            VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            InfoColumn("NG", "$totalNg Pcs", MaterialTheme.colorScheme.error)
            VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            InfoColumn(
                "Rasio NG",
                String.format("%.1f%%", rasioNg),
                if (adaKuantitasTidakValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
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
            color = MaterialTheme.colorScheme.outline,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailSummaryItem("Diperiksa", "${payload.totalDiperiksa}", MaterialTheme.colorScheme.onSurfaceVariant)
                        DetailSummaryItem("OK", "${payload.totalOk}", MaterialTheme.colorScheme.tertiary)
                        DetailSummaryItem("NG", "${payload.totalNg}", MaterialTheme.colorScheme.error)
                        DetailSummaryItem("Rasio", String.format("%.1f%%", payload.rasioNgGlobal), MaterialTheme.colorScheme.secondary)
                    }
                }

                Text(
                    "Rincian Temuan per Part:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(payload.daftarPart) { part ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(part.uniqNo, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
                                        Text("${part.jumlahNg} NG", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (part.daftarDefectNg.isNotEmpty()) {
                                        Text(
                                            text = part.daftarDefectNg.joinToString { "${it.namaDefect} (${it.jumlahNg})" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Peringatan: Ada jumlah NG yang tidak valid.",
                                color = MaterialTheme.colorScheme.error,
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun DetailSummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun DetailRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
