package com.primaraya.inspectra.fitur.checksheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.*
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.checksheet.domain.*
import com.primaraya.inspectra.fitur.checksheet.ui.ChecksheetContract.State.Step

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
                            text = if (state.step == Step.PILIH_PART) "Pilih Part ${tipeProses.labelIndonesia()}" else "Inspeksi ${tipeProses.labelIndonesia()}",
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (state.step == Step.PILIH_PART) "Cari dan pilih part yang akan dicek" else "Form pemeriksaan harian",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.step == Step.ISI_FORM) {
                            viewModel.onIntent(ChecksheetContract.Intent.KembaliKePicker)
                        } else {
                            onBackClick()
                        }
                    }) {
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
                if (state.step == Step.PILIH_PART) {
                    Button(
                        onClick = { viewModel.onIntent(ChecksheetContract.Intent.LanjutKeForm) },
                        enabled = state.partTerpilih.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Mulai Cek (${state.partTerpilih.size} Part)", fontWeight = FontWeight.Black)
                    }
                } else {
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
                            Text(stringResource(com.primaraya.inspectra.R.string.checksheet_review_submit), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    ) { padding ->
        AppResponsiveContent(modifier = Modifier.padding(padding)) { isTablet, contentModifier ->
            if (state.step == Step.PILIH_PART) {
                StepPilihPart(
                    state = state,
                    isTablet = isTablet,
                    onCari = { viewModel.onIntent(ChecksheetContract.Intent.CariPart(it)) },
                    onPilih = { uniqNo, pilih -> viewModel.onIntent(ChecksheetContract.Intent.PilihPart(uniqNo, pilih)) },
                    onRetry = { viewModel.onIntent(ChecksheetContract.Intent.Retry) },
                    modifier = contentModifier
                )
            } else {
                StepIsiForm(
                    state = state,
                    tipeProses = tipeProses,
                    onRetry = { viewModel.onIntent(ChecksheetContract.Intent.Retry) },
                    viewModel = viewModel,
                    isTablet = isTablet,
                    modifier = contentModifier
                )
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

    state.dialogTambahDefect?.let { uniqNo ->
        DialogTambahDefectLain(
            defectsMaster = state.daftarDefectMaster,
            onDismiss = { viewModel.onIntent(ChecksheetContract.Intent.TutupDialogTambahDefect) },
            onConfirm = { defect -> viewModel.onIntent(ChecksheetContract.Intent.TambahDefectLain(uniqNo, defect)) }
        )
    }
}

@Composable
fun DialogTambahDefectLain(
    defectsMaster: AsyncData<List<com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto>>,
    onDismiss: () -> Unit,
    onConfirm: (com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Temuan Baru", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Cari jenis defect yang belum muncul di daftar checksheet.", style = MaterialTheme.typography.bodyMedium)
                
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cetik nama defect...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                when (defectsMaster) {
                    is AsyncData.Loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is AsyncData.Error -> Text(defectsMaster.message, color = MaterialTheme.colorScheme.error)
                    is AsyncData.Success -> {
                        val filtered = defectsMaster.data.filter { 
                            it.nama_defect.contains(query, ignoreCase = true) || it.id_defect.contains(query, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            AppEmptyState("Tidak ditemukan", "Gunakan kata kunci lain.", modifier = Modifier.height(150.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 350.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filtered, key = { it.id_defect }) { defect ->
                                    ListItem(
                                        headlineContent = { Text(defect.nama_defect, fontWeight = FontWeight.Bold) },
                                        supportingContent = { 
                                            Text(defect.kategori, style = MaterialTheme.typography.labelSmall)
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { onConfirm(defect) }) {
                                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        modifier = Modifier.clickable { onConfirm(defect) }
                                    )
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
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
    onDefectSembunyikan: (String) -> Unit,
    onBukaTambahDefect: () -> Unit
) {
    val containerColor = if (part.kuantitasTidakValid) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    } else if (part.jumlahNg > 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBukaTutup),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceDim
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (part.lokasiGambar != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(part.lokasiGambar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.uniqNo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "${part.nomorPart ?: "-"} • ${part.namaPart}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Cek: ${part.jumlahDiperiksa}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (part.jumlahNg > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "NG: ${part.jumlahNg}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onBukaTutup) {
                    Icon(
                        imageVector = if (part.terbuka) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (part.terbuka) {
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = if (part.jumlahDiperiksa == 0) "" else part.jumlahDiperiksa.toString(),
                    onValueChange = { onJumlahDiperiksaUbah(it.toIntOrNull() ?: 0) },
                    label = { Text(stringResource(com.primaraya.inspectra.R.string.checksheet_input_checked_qty)) },
                    placeholder = { Text("0 Pcs") },
                    supportingText = {
                        if (part.kuantitasTidakValid) {
                            Text("Peringatan: Jumlah NG melebihi jumlah diperiksa.", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Total item fisik yang sudah diperiksa")
                        }
                    },
                    isError = part.kuantitasTidakValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(com.primaraya.inspectra.R.string.checksheet_ng_findings_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (part.daftarDefectAktif.isNotEmpty()) {
                        Text(
                            "${part.daftarDefectAktif.size} Jenis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                part.daftarDefectAktif
                    .groupBy { it.kategori }
                    .forEach { (kategori, defects) ->
                        Text(
                            text = if (kategori == KategoriDefect.MATERIAL) "SUMBER: MATERIAL" else "SUMBER: PROSES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = if (kategori == KategoriDefect.MATERIAL) Color(0xFF0284C7) else Color(0xFF475569),
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            defects.forEach { defect ->
                                BarisDefectStepper(
                                    defect = defect,
                                    onMinus = { onDefectTambahKurang(defect.idDefect, false) },
                                    onPlus = { onDefectTambahKurang(defect.idDefect, true) },
                                    onManual = { qty -> onDefectInputManual(defect.idDefect, qty) },
                                    onSlotUbah = { slotId, qty -> onSlotDefectUbah(defect.idDefect, slotId, qty) },
                                    onSembunyikan = { onDefectSembunyikan(defect.idDefect) }
                                )
                            }
                        }
                    }
                
                Spacer(Modifier.height(16.dp))
                
                FilledTonalButton(
                    onClick = onBukaTambahDefect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(com.primaraya.inspectra.R.string.checksheet_add_other_defect), fontWeight = FontWeight.Bold)
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
    onSlotUbah: (String, Int) -> Unit,
    onSembunyikan: () -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }
    var showSlotDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded }
            ) {
                Text(
                    text = defect.namaDefect,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (defect.jumlahNg > 0) Color(0xFF1E293B) else Color(0xFF64748B),
                    fontWeight = if (defect.jumlahNg > 0) FontWeight.Bold else FontWeight.Medium
                )
                if (defect.detailSlot.isNotEmpty()) {
                    Surface(
                        onClick = { showSlotDialog = true },
                        color = if (defect.slotMatch) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (defect.slotMatch) "✓ Slot OK: ${defect.totalNgDariSlot}" else "⚠ Slot: ${defect.totalNgDariSlot}/${defect.jumlahNg}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (defect.slotMatch) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (expanded) {
                IconButton(onClick = onSembunyikan) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMinus) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = "Kurangi",
                        tint = if (defect.jumlahNg > 0) Color(0xFFDC2626) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .width(60.dp)
                        .height(44.dp)
                        .clickable { showManualDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    color = if (defect.jumlahNg > 0) Color(0xFF1A365D) else Color(0xFFF1F5F9)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = defect.jumlahNg.toString(),
                            fontWeight = FontWeight.Black,
                            color = if (defect.jumlahNg > 0) Color.White else Color(0xFF64748B),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                IconButton(onClick = onPlus) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Tambah",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(28.dp)
                    )
                }
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

@Composable
private fun StepPilihPart(
    state: ChecksheetContract.State,
    isTablet: Boolean,
    onCari: (String) -> Unit,
    onPilih: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.pencarian,
            onValueChange = onCari,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Cari UNIQ NO atau Nama Part...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.pencarian.isNotEmpty()) {
                    IconButton(onClick = { onCari("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        when (val data = state.dataPicker) {
            is AsyncData.Loading -> AppListSkeleton()
            is AsyncData.Error -> AppEmptyState(
                title = data.title,
                message = data.message,
                onRetry = onRetry
            )
            is AsyncData.Success -> {
                val list = state.pickerFiltered
                if (list.isEmpty()) {
                    AppEmptyState("Tidak ada hasil", "Coba kata kunci lain.")
                } else {
                    if (isTablet) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(list, key = { it.uniq_no }) { item ->
                                ItemKartuPicker(
                                    item = item,
                                    terpilih = state.partTerpilih.contains(item.uniq_no),
                                    onPilih = onPilih
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(list, key = { it.uniq_no }) { item ->
                                ItemKartuPicker(
                                    item = item,
                                    terpilih = state.partTerpilih.contains(item.uniq_no),
                                    onPilih = onPilih
                                )
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun ItemKartuPicker(
    item: PartPickerItem,
    terpilih: Boolean,
    onPilih: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPilih(item.uniq_no, !terpilih) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (terpilih) MaterialTheme.colorScheme.primaryContainer else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = terpilih,
                onCheckedChange = { onPilih(item.uniq_no, it) }
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.image_url != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.image_url)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.uniq_no, fontWeight = FontWeight.Bold)
                Text(
                    "${item.part_no ?: "-"} | ${item.nama_part}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.status_input != "SIAP_INPUT") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.status_input,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIsiForm(
    state: ChecksheetContract.State,
    tipeProses: TipeProses,
    onRetry: () -> Unit,
    viewModel: ChecksheetMviViewModel,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RingkasanAtas(
            totalDiperiksa = state.totalDiperiksa,
            totalOk = state.totalOk,
            totalNg = state.totalNg,
            rasioNg = state.rasioNg,
            adaKuantitasTidakValid = state.adaQtyTidakValid
        )

        when (val data = state.dataChecksheet) {
            is AsyncData.Loading -> AppListSkeleton()
            is AsyncData.Error -> AppEmptyState(
                title = data.title,
                message = data.message,
                onRetry = onRetry
            )
            is AsyncData.Success -> {
                if (isTablet) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                onDefectSembunyikan = { idDefect ->
                                    viewModel.onIntent(ChecksheetContract.Intent.SembunyikanDefect(part.uniqNo, idDefect))
                                },
                                onBukaTambahDefect = {
                                    viewModel.onIntent(ChecksheetContract.Intent.BukaTambahDefectLain(part.uniqNo))
                                }
                            )
                        }
                    }
                } else {
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
                                onDefectSembunyikan = { idDefect ->
                                    viewModel.onIntent(ChecksheetContract.Intent.SembunyikanDefect(part.uniqNo, idDefect))
                                },
                                onBukaTambahDefect = {
                                    viewModel.onIntent(ChecksheetContract.Intent.BukaTambahDefectLain(part.uniqNo))
                                }
                            )
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}
