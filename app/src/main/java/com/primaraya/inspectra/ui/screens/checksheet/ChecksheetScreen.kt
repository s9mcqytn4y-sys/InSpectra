package com.primaraya.inspectra.ui.screens.checksheet

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaraya.inspectra.domain.model.InputDefect
import com.primaraya.inspectra.domain.model.KategoriDefect
import com.primaraya.inspectra.domain.model.MaterialPartAcuan
import com.primaraya.inspectra.domain.model.RingkasanPartChecksheet
import com.primaraya.inspectra.domain.model.TipeProses

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksheetScreen(
    tipeProses: TipeProses,
    viewModel: ChecksheetViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(tipeProses) {
        viewModel.muatChecksheet(tipeProses)
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChecksheetEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is ChecksheetEvent.SubmitSuccess -> {
                    viewModel.muatChecksheet(tipeProses) // Reset data
                }
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val daftarPart = uiState.daftarPart
    val pesanValidasi = uiState.pesanValidasi
    val payloadJson = uiState.payloadJson

    val totalDiperiksa by remember(daftarPart) {
        derivedStateOf { daftarPart.sumOf { it.jumlahDiperiksa } }
    }

    val totalNg by remember(daftarPart) {
        derivedStateOf { daftarPart.sumOf { it.jumlahNg } }
    }

    val totalOk by remember(daftarPart) {
        derivedStateOf { totalDiperiksa - totalNg }
    }

    val rasioNg by remember(daftarPart) {
        derivedStateOf {
            if (totalDiperiksa > 0) {
                (totalNg.toFloat() / totalDiperiksa.toFloat()) * 100f
            } else {
                0f
            }
        }
    }

    val adaKuantitasTidakValid by remember(daftarPart) {
        derivedStateOf { daftarPart.any { it.kuantitasTidakValid } }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Inspeksi ${tipeProses.labelIndonesia()}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supabase Connected",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A365D),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (payloadJson == null && daftarPart.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.buatPayloadValidasi(tipeProses) },
                    containerColor = Color(0xFFD97706),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Payload")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buat Payload", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                RingkasanAtas(
                    totalDiperiksa = totalDiperiksa,
                    totalOk = totalOk,
                    totalNg = totalNg,
                    rasioNg = rasioNg,
                    adaKuantitasTidakValid = adaKuantitasTidakValid
                )

                if (pesanValidasi != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pesanValidasi,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (payloadJson != null) {
                    PanelPayloadValidasi(
                        json = payloadJson,
                        onTutup = { viewModel.hapusPayload() },
                        onSubmit = { viewModel.submitKeSupabase() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        if (daftarPart.isEmpty() && !uiState.isLoading) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Belum ada data acuan untuk proses ini",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A365D)
                                        )
                                    }
                                }
                            }
                        }

                        items(daftarPart, key = { it.uniqNo }) { part ->
                            KartuPartChecksheet(
                                part = part,
                                onBukaTutup = { viewModel.ubahBukaTutup(part.uniqNo) },
                                onJumlahDiperiksaUbah = { viewModel.ubahJumlahDiperiksa(part.uniqNo, it) },
                                onDefectTambahKurang = { idDefect, isTambah ->
                                    viewModel.tambahKurangiDefect(part.uniqNo, idDefect, isTambah)
                                },
                                onDefectInputManual = { idDefect, qty ->
                                    viewModel.isiManualDefect(part.uniqNo, idDefect, qty)
                                }
                            )
                        }
                    }
                }
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

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
fun KartuPartChecksheet(
    part: RingkasanPartChecksheet,
    onBukaTutup: () -> Unit,
    onJumlahDiperiksaUbah: (Int) -> Unit,
    onDefectTambahKurang: (String, Boolean) -> Unit,
    onDefectInputManual: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (part.kuantitasTidakValid) Color(0xFFFEF2F2) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBukaTutup() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color(0xFF1A365D))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(part.uniqNo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF1A365D))
                    Text("${part.nomorPart ?: "-"} | ${part.namaPart}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        SuggestionChip(onClick = {}, label = { Text("Cek: ${part.jumlahDiperiksa}") })
                        SuggestionChip(
                            onClick = {}, 
                            label = { Text("NG: ${part.jumlahNg}", color = if (part.jumlahNg > 0) Color.Red else Color.DarkGray) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (part.kuantitasTidakValid) Color(0xFFFEE2E2) else Color.Transparent
                            )
                        )
                    }
                }
                Icon(if (part.terbuka) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color.Gray)
            }

            if (part.terbuka) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = if (part.jumlahDiperiksa == 0) "" else part.jumlahDiperiksa.toString(),
                    onValueChange = { onJumlahDiperiksaUbah(it.toIntOrNull() ?: 0) },
                    label = { Text("Jumlah Diperiksa") },
                    isError = part.kuantitasTidakValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                
                if (part.kuantitasTidakValid) {
                    Text(
                        text = "Jumlah NG melebihi jumlah diperiksa",
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                if (part.daftarMaterial.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Material Digunakan:", fontWeight = FontWeight.Bold, color = Color(0xFF1A365D), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        part.daftarMaterial.forEach { material ->
                            BarisMaterial(material)
                        }
                    }
                }

                if (part.daftarDefect.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Jenis Temuan NG:", fontWeight = FontWeight.Bold, color = Color(0xFF1A365D), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    val groupedDefects = part.daftarDefect.groupBy { it.kategori }
                    
                    groupedDefects.forEach { (kategori, defects) ->
                        Text(
                            text = "Kategori ${kategori.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        defects.forEach { defect ->
                            BarisInputDefect(
                                defect = defect,
                                onUbahQty = { isTambah -> onDefectTambahKurang(defect.idDefect, isTambah) },
                                onInputManual = { qty -> onDefectInputManual(defect.idDefect, qty) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarisMaterial(material: MaterialPartAcuan) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(material.materialDigunakan, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Supplier: ${material.namaSupplier ?: "-"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Spec: ${material.specAsli ?: "-"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = Color(0xFFE2E8F0))
    }
}

@Composable
fun BarisInputDefect(
    defect: InputDefect,
    onUbahQty: (Boolean) -> Unit,
    onInputManual: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(defect.namaDefect, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = { onUbahQty(false) }, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang", tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                }
            }
            
            OutlinedTextField(
                value = if (defect.jumlahNg == 0) "" else defect.jumlahNg.toString(),
                onValueChange = { text ->
                    val manualValue = text.toIntOrNull() ?: 0
                    onInputManual(manualValue)
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.Center, 
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(56.dp)
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1A365D),
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )

            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = { onUbahQty(true) }, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun PanelPayloadValidasi(json: String, onTutup: () -> Unit, onSubmit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A365D))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payload JSON",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onTutup) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
            
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kirim ke Supabase", color = Color.White, fontWeight = FontWeight.Bold)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = json,
                        style = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

fun TipeProses.labelIndonesia(): String {
    return when (this) {
        TipeProses.PRESS -> "Press"
        TipeProses.SEWING -> "Sewing"
        TipeProses.CUTTING -> "Cutting"
        TipeProses.MATERIAL -> "Material"
        TipeProses.PASS_THROUGH -> "Pass Through"
        TipeProses.CONSUMABLE -> "Consumable"
    }
}
