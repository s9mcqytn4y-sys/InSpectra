package com.primaraya.inspectra.ui.screens.checksheet

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primaraya.inspectra.domain.model.InputDefect
import com.primaraya.inspectra.domain.model.KategoriDefect
import com.primaraya.inspectra.domain.model.PayloadChecksheet
import com.primaraya.inspectra.domain.model.RingkasanPartChecksheet
import com.primaraya.inspectra.domain.model.TipeProses

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksheetScreen(
    tipeProses: TipeProses,
    viewModel: ChecksheetViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tipeProses) {
        viewModel.muatChecksheet(tipeProses)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChecksheetEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is ChecksheetEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is ChecksheetEvent.SubmitSuccess -> {
                    snackbarHostState.showSnackbar("Data tersimpan. ID sesi: ${event.sesiId.take(8)}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Inspeksi ${tipeProses.labelIndonesia()}",
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Form pemeriksaan harian",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f)
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
        bottomBar = {
            Surface(shadowElevation = 10.dp) {
                Button(
                    onClick = { viewModel.buatPreviewPayload(tipeProses) },
                    enabled = uiState.adaInput &&
                        !uiState.adaQtyTidakValid &&
                        !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.TaskAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tinjau & Kirim", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            Column {
                RingkasanAtas(
                    totalDiperiksa = uiState.totalDiperiksa,
                    totalOk = uiState.totalOk,
                    totalNg = uiState.totalNg,
                    rasioNg = uiState.rasioNg,
                    adaKuantitasTidakValid = uiState.adaQtyTidakValid
                )

                when {
                    uiState.isLoading -> {
                        ChecksheetSkeleton()
                    }

                    uiState.daftarPart.isEmpty() -> {
                        EmptyStateCard(
                            title = "Belum ada data part",
                            message = "Data acuan untuk proses ini belum tersedia."
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.daftarPart,
                                key = { it.uniqNo }
                            ) { part ->
                                KartuPartChecksheetRingkas(
                                    part = part,
                                    onBukaTutup = { viewModel.ubahBukaTutup(part.uniqNo) },
                                    onJumlahDiperiksaUbah = {
                                        viewModel.ubahJumlahDiperiksa(part.uniqNo, it)
                                    },
                                    onDefectTambahKurang = { idDefect, tambah ->
                                        viewModel.tambahKurangiDefect(part.uniqNo, idDefect, tambah)
                                    },
                                    onDefectInputManual = { idDefect, qty ->
                                        viewModel.isiManualDefect(part.uniqNo, idDefect, qty)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.previewPayload?.let { payload ->
        KonfirmasiKirimChecksheetDialog(
            payload = payload,
            onDismiss = { viewModel.hapusPayload() },
            onConfirm = { viewModel.submitKeSupabase() }
        )
    }

    uiState.userMessage?.let { msg ->
        FriendlyInfoDialog(
            title = msg.title,
            message = msg.body,
            buttonText = msg.actionLabel ?: "Mengerti",
            onDismiss = { viewModel.clearUserMessage() }
        )
    }
}

@Composable
fun ChecksheetSkeleton() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF1A365D))
    }
}

@Composable
fun EmptyStateCard(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, textAlign = TextAlign.Center, color = Color.Gray)
    }
}

@Composable
fun FriendlyInfoDialog(
    title: String,
    message: String,
    buttonText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(buttonText)
            }
        }
    )
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
fun KartuPartChecksheetRingkas(
    part: RingkasanPartChecksheet,
    onBukaTutup: () -> Unit,
    onJumlahDiperiksaUbah: (Int) -> Unit,
    onDefectTambahKurang: (String, Boolean) -> Unit,
    onDefectInputManual: (String, Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (part.kuantitasTidakValid) {
                Color(0xFFFFF1F2)
            } else {
                Color.White
            }
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBukaTutup),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFF1A365D),
                    modifier = Modifier.size(38.dp)
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.uniqNo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A365D)
                    )

                    Text(
                        text = "${part.nomorPart ?: "-"} | ${part.namaPart}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Cek: ${part.jumlahDiperiksa}") }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("NG: ${part.jumlahNg}") }
                        )
                    }
                }

                Icon(
                    imageVector = if (part.terbuka) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            if (part.terbuka) {
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = if (part.jumlahDiperiksa == 0) "" else part.jumlahDiperiksa.toString(),
                    onValueChange = { onJumlahDiperiksaUbah(it.toIntOrNull() ?: 0) },
                    label = { Text("Jumlah Diperiksa") },
                    supportingText = {
                        if (part.kuantitasTidakValid) {
                            Text("Jumlah NG tidak boleh melebihi jumlah diperiksa.")
                        }
                    },
                    isError = part.kuantitasTidakValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Jenis Temuan NG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A365D)
                )

                Spacer(Modifier.height(8.dp))

                part.daftarDefect
                    .groupBy { it.kategori }
                    .forEach { (kategori, defects) ->
                        Text(
                            text = if (kategori == KategoriDefect.MATERIAL) {
                                "Material"
                            } else {
                                "Proses"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        defects.forEach { defect ->
                            BarisDefectStepper(
                                defect = defect,
                                onMinus = { onDefectTambahKurang(defect.idDefect, false) },
                                onPlus = { onDefectTambahKurang(defect.idDefect, true) },
                                onManual = { qty -> onDefectInputManual(defect.idDefect, qty) }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
fun BarisDefectStepper(
    defect: InputDefect,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onManual: (Int) -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = defect.namaDefect,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = Color(0xFF1F2937)
        )

        IconButton(onClick = onMinus) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                contentDescription = "Kurangi",
                tint = Color(0xFFDC2626)
            )
        }

        Surface(
            modifier = Modifier
                .width(56.dp)
                .height(42.dp)
                .clickable { showManualDialog = true },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            color = Color(0xFFF1F5F9)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = defect.jumlahNg.toString(),
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A365D)
                )
            }
        }

        IconButton(onClick = onPlus) {
            Icon(
                Icons.Default.AddCircleOutline,
                contentDescription = "Tambah",
                tint = Color(0xFF16A34A)
            )
        }
    }

    if (showManualDialog) {
        DialogInputJumlahNg(
            title = defect.namaDefect,
            initialValue = defect.jumlahNg,
            onDismiss = { showManualDialog = false },
            onConfirm = {
                onManual(it)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun DialogInputJumlahNg(
    title: String,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var value by remember { mutableStateOf(if (initialValue == 0) "" else initialValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ubah Jumlah NG",
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column {
                Text(title, color = Color(0xFF64748B))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit) },
                    label = { Text("Jumlah NG") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(value.toIntOrNull() ?: 0) }) {
                Text("Simpan")
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
