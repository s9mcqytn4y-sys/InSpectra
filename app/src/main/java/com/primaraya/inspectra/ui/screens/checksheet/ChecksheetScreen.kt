package com.primaraya.inspectra.ui.screens.checksheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaraya.inspectra.domain.model.PartChecksheetSummary
import com.primaraya.inspectra.domain.model.ProcessType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksheetScreen(
    processType: ProcessType,
    viewModel: ChecksheetViewModel,
    onBackClick: () -> Unit
) {
    val parts by viewModel.uiState.collectAsState()
    var showSummaryDialog by remember { mutableStateOf(false) }

    val grandTotalSampling by remember { derivedStateOf { parts.sumOf { it.totalSampling } } }
    val grandTotalNg by remember { derivedStateOf { parts.sumOf { it.totalNg } } }
    val grandTotalOk by remember { derivedStateOf { parts.sumOf { it.totalOk } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checksheet: ${processType.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A365D), titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (grandTotalSampling > 0) showSummaryDialog = true },
                containerColor = Color(0xFFD97706),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Kirim")
                Spacer(modifier = Modifier.width(8.dp))
                Text("KIRIM LAPORAN", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Widget Akumulasi Data Atas
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL CEK", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$grandTotalSampling Pcs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text("TOTAL OK", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$grandTotalOk Pcs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                    }
                    Column {
                        Text("TOTAL NG", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$grandTotalNg Pcs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(parts, key = { it.uniqNo }) { part ->
                    PartChecksheetCard(
                        part = part,
                        onExpandClick = { viewModel.toggleExpand(part.uniqNo) },
                        onSamplingChange = { viewModel.updateSampling(part.uniqNo, it) },
                        onDefectChange = { defId, isInc -> viewModel.updateDefectQty(part.uniqNo, defId, isInc) },
                        onDefectManualInput = { defId, qty -> viewModel.setDefectQty(part.uniqNo, defId, qty) }
                    )
                }
            }
        }

        if (showSummaryDialog) {
            AlertDialog(
                onDismissRequest = { showSummaryDialog = false },
                title = { Text("Konfirmasi Laporan Akhir", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "⚠️ PERINGATAN: Data yang telah dikirim ke cloud server tidak dapat diubah atau dihapus kembali oleh operator.", 
                            color = Color(0xFFDC2626), 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider(color = Color.LightGray)
                        Text("Ringkasan Laporan:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        
                        parts.filter { it.totalSampling > 0 }.forEach { part ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("[${part.uniqNo}] ${part.partName}", fontWeight = FontWeight.Bold)
                                    Text("Pemeriksaan: ${part.totalSampling} Pcs | OK: ${part.totalOk} | Total NG: ${part.totalNg}", style = MaterialTheme.typography.bodySmall)
                                    val checkedDefects = part.defects.filter { it.ngQty > 0 }
                                    if (checkedDefects.isNotEmpty()) {
                                        Text("Detail Kerusakan:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                        checkedDefects.forEach { defect ->
                                            Text(" - ${defect.defectName}: ${defect.ngQty} Pcs", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSummaryDialog = false
                            val payload = viewModel.generateFinalPayload(processType)
                            println("=== INSPECTRA SECURE PAYLOAD VALIDATED ===")
                            println(payload.toString())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Kirim & Kunci Data", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showSummaryDialog = false }) { Text("Batal", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
fun PartChecksheetCard(
    part: PartChecksheetSummary,
    onExpandClick: () -> Unit,
    onSamplingChange: (Int) -> Unit,
    onDefectChange: (String, Boolean) -> Unit,
    onDefectManualInput: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpandClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color(0xFF1A365D))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(part.uniqNo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF1A365D))
                    Text("${part.partNumber} - ${part.partName}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        SuggestionChip(onClick = {}, label = { Text("Cek: ${part.totalSampling}") })
                        SuggestionChip(onClick = {}, label = { Text("NG: ${part.totalNg}", color = Color.Red) })
                    }
                }
                Icon(if (part.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color.Gray)
            }

            if (part.isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = if (part.totalSampling == 0) "" else part.totalSampling.toString(),
                    onValueChange = { onSamplingChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Input Total Sampling / Cek") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Daftar Item Kerusakan:", fontWeight = FontWeight.Bold, color = Color(0xFF1A365D), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))

                part.defects.forEach { defect ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(defect.defectName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Tombol Minus (48.dp Touch Target)
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                IconButton(onClick = { onDefectChange(defect.defectId, false) }, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang", tint = Color(0xFFDC2626), modifier = Modifier.size(26.dp))
                                }
                            }
                            
                            // TUNNING: Text Field untuk Input Kuantitas Manual Secara Instan
                            OutlinedTextField(
                                value = if (defect.ngQty == 0) "" else defect.ngQty.toString(),
                                onValueChange = { text ->
                                    val manualValue = text.toIntOrNull() ?: 0
                                    onDefectManualInput(defect.defectId, manualValue)
                                },
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center, 
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(64.dp).height(50.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1A365D),
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                singleLine = true
                            )

                            // Tombol Plus (48.dp Touch Target)
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                IconButton(onClick = { onDefectChange(defect.defectId, true) }, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = Color(0xFF16A34A), modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}
