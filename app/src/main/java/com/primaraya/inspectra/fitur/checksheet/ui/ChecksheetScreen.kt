package com.primaraya.inspectra.fitur.checksheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.AppEmptyState
import com.primaraya.inspectra.core.ui.component.AppFriendlyDialog
import com.primaraya.inspectra.core.ui.component.AppListSkeleton
import com.primaraya.inspectra.core.ui.component.PreviewChecksheetDialog
import com.primaraya.inspectra.core.ui.component.RingkasanAtas
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.checksheet.domain.InputDefect
import com.primaraya.inspectra.fitur.checksheet.domain.KategoriDefect
import com.primaraya.inspectra.fitur.checksheet.domain.RingkasanPartChecksheet
import com.primaraya.inspectra.fitur.checksheet.domain.TipeProses

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksheetScreen(
    tipeProses: TipeProses,
    onBackClick: () -> Unit,
    viewModel: ChecksheetMviViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application,
            pembuat = { ChecksheetMviViewModel(it) }
        )
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tipeProses) {
        viewModel.onIntent(ChecksheetContract.Intent.Muat(tipeProses))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChecksheetContract.Effect.PesanSukses -> {
                    snackbarHostState.showSnackbar(effect.pesan)
                }
                is ChecksheetContract.Effect.PesanError -> {
                    snackbarHostState.showSnackbar("${effect.judul}: ${effect.pesan}")
                }
                is ChecksheetContract.Effect.KirimBerhasil -> {
                    snackbarHostState.showSnackbar("Data tersimpan. ID: ${effect.idSesi.take(8)}")
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                    onClick = { viewModel.onIntent(ChecksheetContract.Intent.Tinjau) },
                    enabled = state.adaInput && !state.adaQtyTidakValid && !state.adaSlotTidakMatch && !state.mengirim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (state.mengirim) {
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
                    totalDiperiksa = state.totalDiperiksa,
                    totalOk = state.totalOk,
                    totalNg = state.totalNg,
                    rasioNg = state.rasioNg,
                    adaKuantitasTidakValid = state.adaQtyTidakValid
                )

                when (val data = state.dataChecksheet) {
                    is AsyncData.Loading -> AppListSkeleton()
                    is AsyncData.Empty -> AppEmptyState(title = data.title, message = data.message)
                    is AsyncData.Error -> AppEmptyState(title = data.title, message = data.message)
                    is AsyncData.Success -> {
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
                                items = data.data,
                                key = { it.uniqNo }
                            ) { part ->
                                KartuPartChecksheetRingkas(
                                    part = part,
                                    tipeProses = tipeProses,
                                    onBukaTutup = { viewModel.onIntent(ChecksheetContract.Intent.TogglePart(part.uniqNo)) },
                                    onJumlahDiperiksaUbah = {
                                        viewModel.onIntent(ChecksheetContract.Intent.UbahJumlahDiperiksa(part.uniqNo, it))
                                    },
                                    onDefectTambahKurang = { idDefect, tambah ->
                                        if (tambah) {
                                            viewModel.onIntent(ChecksheetContract.Intent.TambahDefect(part.uniqNo, idDefect))
                                        } else {
                                            viewModel.onIntent(ChecksheetContract.Intent.KurangiDefect(part.uniqNo, idDefect))
                                        }
                                    },
                                    onDefectInputManual = { idDefect, qty ->
                                        viewModel.onIntent(ChecksheetContract.Intent.UbahJumlahDefect(part.uniqNo, idDefect, qty))
                                    },
                                    onSlotDefectUbah = { idDefect, slotId, qty ->
                                        viewModel.onIntent(ChecksheetContract.Intent.UbahJumlahSlotDefect(part.uniqNo, idDefect, slotId, qty))
                                    },
                                    onDetailCuttingUbah = { lot, roll, size, waste, pic ->
                                        viewModel.onIntent(ChecksheetContract.Intent.UbahDetailCutting(part.uniqNo, lot, roll, size, waste, pic))
                                    }
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    state.preview?.let { payload ->
        PreviewChecksheetDialog(
            payload = payload,
            onDismiss = { viewModel.onIntent(ChecksheetContract.Intent.TutupPreview) },
            onConfirm = { viewModel.onIntent(ChecksheetContract.Intent.Kirim) },
            sending = state.mengirim
        )
    }
}

@Composable
fun KartuPartChecksheetRingkas(
    part: RingkasanPartChecksheet,
    tipeProses: TipeProses,
    onBukaTutup: () -> Unit,
    onJumlahDiperiksaUbah: (Int) -> Unit,
    onDefectTambahKurang: (String, Boolean) -> Unit,
    onDefectInputManual: (String, Int) -> Unit,
    onSlotDefectUbah: (String, String, Int) -> Unit,
    onDetailCuttingUbah: (String?, String?, String?, Double?, String?) -> Unit
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

                if (tipeProses == TipeProses.CUTTING) {
                    Spacer(Modifier.height(16.dp))
                    Text("Detail Cutting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = part.detailCutting?.noLot ?: "",
                                onValueChange = { onDetailCuttingUbah(it, null, null, null, null) },
                                label = { Text("No. Lot") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = part.detailCutting?.noRoll ?: "",
                                onValueChange = { onDetailCuttingUbah(null, it, null, null, null) },
                                label = { Text("No. Roll") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = part.detailCutting?.sizeCuttingCm ?: "",
                                onValueChange = { onDetailCuttingUbah(null, null, it, null, null) },
                                label = { Text("Ukuran (cm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = part.detailCutting?.waste?.toString() ?: "",
                                onValueChange = { onDetailCuttingUbah(null, null, null, it.toDoubleOrNull(), null) },
                                label = { Text("Sisa Material") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = part.detailCutting?.pic ?: "",
                            onValueChange = { onDetailCuttingUbah(null, null, null, null, it) },
                            label = { Text("Operator") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

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
                                onManual = { qty -> onDefectInputManual(defect.idDefect, qty) },
                                onSlotUbah = { slotId, qty -> onSlotDefectUbah(defect.idDefect, slotId, qty) }
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
    onManual: (Int) -> Unit,
    onSlotUbah: (String, Int) -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }
    var showSlotDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = defect.namaDefect,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1F2937)
                )
                if (defect.detailSlot.isNotEmpty()) {
                    Text(
                        text = "Detail Slot: ${defect.totalNgDariSlot} / ${defect.jumlahNg}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (defect.slotMatch) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.clickable { showSlotDialog = true }
                    )
                }
            }

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

        if (showSlotDialog) {
            DialogInputSlotNg(
                defect = defect,
                onDismiss = { showSlotDialog = false },
                onSlotUbah = onSlotUbah
            )
        }
    }
}

@Composable
fun DialogInputSlotNg(
    defect: InputDefect,
    onDismiss: () -> Unit,
    onSlotUbah: (String, Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail NG per Slot: ${defect.namaDefect}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Total NG: ${defect.jumlahNg}", fontWeight = FontWeight.Bold)
                HorizontalDivider()
                
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyColumn {
                        items(defect.detailSlot) { slot ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(slot.labelWaktu, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onSlotUbah(slot.slotId, slot.jumlah - 1) }) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Text(slot.jumlah.toString(), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { onSlotUbah(slot.slotId, slot.jumlah + 1) }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                Text(
                    text = "Total Slot: ${defect.totalNgDariSlot}",
                    color = if (defect.slotMatch) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Selesai") }
        }
    )
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
