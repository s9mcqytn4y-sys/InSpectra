package com.primaraya.inspectra.fitur.cutting.ui

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.*
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.cutting.domain.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuttingScreen(
    onBackClick: () -> Unit,
    viewModel: CuttingViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application
        ) { CuttingViewModel(it) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onIntent(CuttingContract.Intent.Muat) }
    LaunchedEffect(state.pesan) {
        state.pesan?.let {
            snackbar.showSnackbar(it)
            viewModel.onIntent(CuttingContract.Intent.HapusPesan)
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(snackbar) { data ->
                InspectraEliteSnackbar(data)
            }
        },
        topBar = {
            if (!state.berhasil) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Input Cutting", fontWeight = FontWeight.Black)
                            Text("Satu batch untuk setiap lot atau roll", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        if (state.berhasil) {
            CuttingSuccessScreen(
                onDone = { viewModel.onIntent(CuttingContract.Intent.TutupBerhasil) }
            )
        } else {
            when (val statusMaterial = state.material) {
                is AsyncData.Loading -> AppListSkeleton()
                is AsyncData.Error -> AppEmptyState(
                    title = "Material Cutting gagal dimuat", 
                    message = statusMaterial.message,
                    onRetry = { viewModel.onIntent(CuttingContract.Intent.Muat) }
                )
                else -> {
                    AppResponsiveContent(modifier = Modifier.padding(padding)) { isTablet, contentModifier ->
                        FormBatchCutting(
                            input = state.input,
                            material = (state.material as? AsyncData.Success)?.data ?: persistentListOf(),
                            partUkuran = (state.partUkuran as? AsyncData.Success)?.data ?: persistentListOf(),
                            defect = (state.defect as? AsyncData.Success)?.data ?: persistentListOf(),
                            slotWaktu = state.slotWaktu,
                            ringkasan = state.ringkasan,
                            daftarPesanValidasi = state.daftarPesanValidasi,
                            menyimpan = state.menyimpan,
                            isTablet = isTablet,
                            onUbah = { viewModel.onIntent(CuttingContract.Intent.UbahInput(it)) },
                            onPilihMaterial = { viewModel.onIntent(CuttingContract.Intent.PilihMaterial(it)) },
                            onPilihPartUkuran = { viewModel.onIntent(CuttingContract.Intent.PilihPartAcuanUkuran(it)) },
                            onTambahDefect = { viewModel.onIntent(CuttingContract.Intent.TambahDefect(it)) },
                            onUbahJumlahDefect = { id, jumlah -> viewModel.onIntent(CuttingContract.Intent.UbahJumlahDefect(id, jumlah)) },
                            onUbahPanjangDefect = { id, panjang -> viewModel.onIntent(CuttingContract.Intent.UbahPanjangDefect(id, panjang)) },
                            onUbahSlotDefect = { id, slot -> viewModel.onIntent(CuttingContract.Intent.UbahSlotDefect(id, slot)) },
                            onHapusDefect = { viewModel.onIntent(CuttingContract.Intent.HapusDefect(it)) },
                            onSimpan = { viewModel.onIntent(CuttingContract.Intent.BukaPreview) },
                            modifier = contentModifier
                        )
                    }

                    if (state.menampilkanPreview) {
                        DialogPreviewCutting(
                            input = state.input,
                            menyimpan = state.menyimpan,
                            onBatal = { viewModel.onIntent(CuttingContract.Intent.TutupPreview) },
                            onSimpan = { viewModel.onIntent(CuttingContract.Intent.Simpan) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CuttingSuccessScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "Batch Berhasil Disimpan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            "Data pemotongan (Cutting) telah berhasil diunggah ke server.",
            style = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Siapkan Batch Baru", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FormBatchCutting(
    input: InputBatchCutting,
    material: ImmutableList<OpsiMaterialCutting>,
    partUkuran: ImmutableList<OpsiPartUkuranCutting>,
    defect: ImmutableList<com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto>,
    slotWaktu: ImmutableList<com.primaraya.inspectra.fitur.checksheet.domain.SlotNg>,
    ringkasan: AsyncData<ImmutableList<com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting>>,
    daftarPesanValidasi: ImmutableList<String>,
    menyimpan: Boolean,
    isTablet: Boolean,
    onUbah: (InputBatchCutting) -> Unit,
    onPilihMaterial: (OpsiMaterialCutting) -> Unit,
    onPilihPartUkuran: (OpsiPartUkuranCutting) -> Unit,
    onTambahDefect: (com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto) -> Unit,
    onUbahJumlahDefect: (String, String) -> Unit,
    onUbahPanjangDefect: (String, String) -> Unit,
    onUbahSlotDefect: (String, String?) -> Unit,
    onHapusDefect: (String) -> Unit,
    onSimpan: () -> Unit,
    modifier: Modifier
) {
    val materialTerpilih = material.firstOrNull { it.material_id == input.materialId }
    val showInlineErrors = daftarPesanValidasi.isNotEmpty()

    if (isTablet) {
        Row(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EliteFormSection(title = "Informasi Dasar", icon = Icons.Default.Dataset) {
                        OutlinedTextField(
                            value = input.tanggalPemeriksaan,
                            onValueChange = { onUbah(input.copy(tanggalPemeriksaan = it)) },
                            label = { Text("Tanggal Produksi") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        AppDropdownField(
                            label = "Material",
                            value = input.namaMaterial,
                            options = material.map { it.labelPilihan },
                            onSelected = { label -> material.firstOrNull { it.labelPilihan == label }?.let(onPilihMaterial) },
                            error = if (showInlineErrors) ValidatorBatchCutting.getErrorForField("material", input) else null
                        )
                        
                        if (input.spesifikasiMaterial.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Spesifikasi Otomatis:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(input.spesifikasiMaterial, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = input.lotRoll,
                            onValueChange = { onUbah(input.copy(lotRoll = it)) },
                            label = { Text("Nomor Lot / Roll") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contoh: LOT-2024-X") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                item {
                    EliteFormSection(title = "Dimensi & Hasil Output", icon = Icons.Default.Straighten) {
                        val opsiUkuran = materialTerpilih?.daftarUkuranValid?.map { "${it.ukuranEfektif} cm" }.orEmpty()
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (opsiUkuran.isNotEmpty()) {
                                AppDropdownField(
                                    label = "Pilih Ukuran Referensi",
                                    value = if (input.idReferensiUkuranMaterial != null) "${input.ukuranCuttingCm} cm" else "Gunakan Ukuran Manual",
                                    options = opsiUkuran + "Gunakan Ukuran Manual",
                                    onSelected = { label ->
                                        if (label == "Gunakan Ukuran Manual") {
                                            onUbah(input.copy(idReferensiUkuranMaterial = null))
                                        } else {
                                            materialTerpilih?.daftarUkuranValid
                                                ?.firstOrNull { "${it.ukuranEfektif} cm" == label }
                                                ?.let { ukuran ->
                                                    onUbah(input.copy(
                                                        ukuranCuttingCm = ukuran.ukuranEfektif.toString(),
                                                        idReferensiUkuranMaterial = ukuran.id
                                                    ))
                                                }
                                        }
                                    }
                                )
                            }
                            
                            OutlinedTextField(
                                value = input.ukuranCuttingCm,
                                onValueChange = { onUbah(input.copy(ukuranCuttingCm = it, idReferensiUkuranMaterial = null)) },
                                label = { Text("Ukuran Cutting (cm)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = showInlineErrors && ValidatorBatchCutting.getErrorForField("ukuran", input) != null,
                                supportingText = {
                                    ValidatorBatchCutting.getErrorForField("ukuran", input)?.let { Text(it) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = input.qtyLayerOk,
                                onValueChange = { onUbah(input.copy(qtyLayerOk = it.filter(Char::isDigit))) },
                                label = { Text("Layer OK") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = input.qtyLayerNg,
                                onValueChange = { onUbah(input.copy(qtyLayerNg = it.filter(Char::isDigit))) },
                                label = { Text("Layer NG") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = showInlineErrors && input.qtyLayerNgAngka > 0 && input.daftarDefect.isEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = input.wastePanjangCm,
                            onValueChange = { onUbah(input.copy(wastePanjangCm = it)) },
                            label = { Text("Waste Panjang (cm)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    EliteFormSection(title = "Temuan NG (Non-Good)", icon = Icons.Default.ReportProblem) {
                        AppDropdownField(
                            label = "Tambah Jenis Defect",
                            value = "Pilih defect dari list",
                            options = defect.filterNot { d -> input.daftarDefect.any { it.idDefect == d.id_defect } }.map { it.nama_defect },
                            onSelected = { nama -> defect.firstOrNull { it.nama_defect == nama }?.let(onTambahDefect) }
                        )
                    }
                }

                items(input.daftarDefect, key = { it.idDefect }) { item ->
                    BarisDefectCuttingElite(
                        defect = item,
                        slotWaktu = slotWaktu,
                        onUbahJumlah = { onUbahJumlahDefect(item.idDefect, it) },
                        onUbahPanjang = { onUbahPanjangDefect(item.idDefect, it) },
                        onUbahSlot = { onUbahSlotDefect(item.idDefect, it) },
                        onHapus = { onHapusDefect(item.idDefect) }
                    )
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                EliteCalculationCard(input = input)

                if (daftarPesanValidasi.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Validasi Gagal", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            }
                            daftarPesanValidasi.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) }
                        }
                    }
                }

                Button(
                    onClick = onSimpan,
                    enabled = !menyimpan,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (menyimpan) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("SIMPAN HASIL POTONG", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    }
                }
                
                val listRingkasan = (ringkasan as? AsyncData.Success)?.data.orEmpty()
                if (listRingkasan.isNotEmpty()) {
                    EliteRingkasanSection(listRingkasan)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { EliteCalculationCard(input = input) }
            item {
                Button(onClick = onSimpan, enabled = !menyimpan, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (menyimpan) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Lanjut Preview")
                }
            }
        }
    }
}

@Composable
private fun EliteFormSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun EliteCalculationCard(input: InputBatchCutting) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Kalkulasi Real-time", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            CalculationRow("Total Layer", input.totalLayer.toString())
            CalculationRow("Rasio NG", "${input.rasioNgLayer}%", isError = input.rasioNgLayer > 5.0)
            CalculationRow("Rasio Waste", "${input.rasioWastePanjang}%")
            
            Spacer(Modifier.height(8.dp))
            Text("Estimasi Panjang (cm)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Hasil OK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        Text("${input.estimasiPanjangOkCm}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Hasil NG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        Text("${input.estimasiPanjangNgCm}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculationRow(label: String, value: String, isError: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value, 
            fontWeight = FontWeight.Bold, 
            color = if(isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EliteRingkasanSection(list: List<com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ringkasan Batch Hari Ini", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        list.take(3).forEach { data ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${data.nama_line ?: "Line"}: ${data.total_batch} batch (OK ${data.total_layer_ok})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun BarisDefectCuttingElite(
    defect: InputDefectCutting,
    slotWaktu: ImmutableList<com.primaraya.inspectra.fitur.checksheet.domain.SlotNg>,
    onUbahJumlah: (String) -> Unit,
    onUbahPanjang: (String) -> Unit,
    onUbahSlot: (String?) -> Unit,
    onHapus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dangerous, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(defect.namaDefect, modifier = Modifier.weight(1f), fontWeight = FontWeight.Black)
                IconButton(onClick = onHapus, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = defect.jumlahLayerTerdampak.toString(),
                    onValueChange = onUbahJumlah,
                    label = { Text("Layer") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = defect.panjangDefectCm?.toString().orEmpty(),
                    onValueChange = onUbahPanjang,
                    label = { Text("Panjang (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
            AppDropdownField(
                label = "Slot Waktu",
                value = slotWaktu.firstOrNull { it.slotId == defect.idSlotWaktu }?.labelWaktu ?: "Pilih Slot",
                options = slotWaktu.map { it.labelWaktu },
                onSelected = { label -> onUbahSlot(slotWaktu.firstOrNull { it.labelWaktu == label }?.slotId) }
            )
        }
    }
}

@Composable
private fun DialogPreviewCutting(
    input: InputBatchCutting,
    menyimpan: Boolean,
    onBatal: () -> Unit,
    onSimpan: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!menyimpan) onBatal() },
        title = { Text("Preview Batch", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tanggal: ${input.tanggalPemeriksaan}")
                Text("Material: ${input.namaMaterial}")
                Text("Lot/Roll: ${input.lotRoll}")
                Text("OK: ${input.qtyLayerOk} | NG: ${input.qtyLayerNg}")
                Text("Waste: ${input.wastePanjangCm} cm")
                HorizontalDivider()
                Text("Defect:", fontWeight = FontWeight.Bold)
                if (input.daftarDefect.isEmpty()) {
                    Text("- Tidak ada")
                } else {
                    input.daftarDefect.forEach {
                        Text("- ${it.namaDefect} (${it.jumlahLayerTerdampak} layer, ${it.panjangDefectCm} cm)")
                    }
                }
                HorizontalDivider()
                Text("Rasio NG: ${input.rasioNgLayer}%")
                Text("Rasio Waste: ${input.rasioWastePanjang}%")
            }
        },
        confirmButton = {
            Button(onClick = onSimpan, enabled = !menyimpan) {
                if (menyimpan) CircularProgressIndicator(modifier = Modifier.width(16.dp), color = Color.White)
                else Text("Kirim Data")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onBatal, enabled = !menyimpan) {
                Text("Batal")
            }
        }
    )
}
