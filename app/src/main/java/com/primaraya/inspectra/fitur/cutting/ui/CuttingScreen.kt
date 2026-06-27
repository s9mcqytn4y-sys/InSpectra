package com.primaraya.inspectra.fitur.cutting.ui

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.AppDropdownField
import com.primaraya.inspectra.core.ui.component.AppEmptyState
import com.primaraya.inspectra.core.ui.component.AppListSkeleton
import com.primaraya.inspectra.core.ui.component.AppResponsiveContent
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.InputDefectCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting
import com.primaraya.inspectra.fitur.cutting.domain.OpsiPartUkuranCutting

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
        snackbarHost = { SnackbarHost(snackbar) },
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
                        containerColor = Color(0xFF1A365D),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
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
            color = Color(0xFFDCFCE7)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color(0xFF16A34A)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "Batch Berhasil Disimpan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1E293B)
        )
        
        Text(
            "Data pemotongan (Cutting) telah berhasil diunggah ke server.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
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

    if (isTablet) {
        Row(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Informasi Pemotongan
                item {
                    Text("Informasi Pemotongan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input.tanggalPemeriksaan,
                        onValueChange = { onUbah(input.copy(tanggalPemeriksaan = it)) },
                        label = { Text("Tanggal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item { HorizontalDivider() }

                // Section 2: Material dan Ukuran
                item {
                    Text("Material dan Ukuran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AppDropdownField(
                        label = "Material",
                        value = input.namaMaterial,
                        options = material.map { it.labelPilihan },
                        onSelected = { label -> material.firstOrNull { it.labelPilihan == label }?.let(onPilihMaterial) }
                    )
                }
                item {
                    OutlinedTextField(
                        value = input.spesifikasiMaterial,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Spesifikasi material") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = input.lotRoll,
                        onValueChange = { onUbah(input.copy(lotRoll = it)) },
                        label = { Text("Lot / Roll") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    AppDropdownField(
                        label = "Ukuran cutting (cm)",
                        value = input.ukuranCuttingCm,
                        options = materialTerpilih?.daftarUkuranValid?.map { "${it.ukuranEfektif} cm" }.orEmpty(),
                        onSelected = { label ->
                            materialTerpilih?.daftarUkuranValid
                                ?.firstOrNull { "${it.ukuranEfektif} cm" == label }
                                ?.let { ukuran ->
                                    onUbah(
                                        input.copy(
                                            ukuranCuttingCm = ukuran.ukuranEfektif.toString(),
                                            idReferensiUkuranMaterial = ukuran.id
                                        )
                                    )
                                }
                        }
                    )
                    OutlinedTextField(
                        value = input.ukuranCuttingCm,
                        onValueChange = { onUbah(input.copy(ukuranCuttingCm = it, idReferensiUkuranMaterial = null)) },
                        label = { Text("Atau isi ukuran manual (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = input.qtyLayerOk,
                            onValueChange = { onUbah(input.copy(qtyLayerOk = it.filter(Char::isDigit))) },
                            label = { Text("Layer OK") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = input.qtyLayerNg,
                            onValueChange = { onUbah(input.copy(qtyLayerNg = it.filter(Char::isDigit))) },
                            label = { Text("Layer NG") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = input.wastePanjangCm,
                        onValueChange = { onUbah(input.copy(wastePanjangCm = it)) },
                        label = { Text("Waste (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                item { HorizontalDivider() }

                // Section 3: Defects
                item {
                    Text("Temuan NG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AppDropdownField(
                        label = "Tambah defect bila ada NG",
                        value = "Pilih defect",
                        options = defect.filterNot { kandidat -> input.daftarDefect.any { it.idDefect == kandidat.id_defect } }.map { it.nama_defect },
                        onSelected = { nama -> defect.firstOrNull { it.nama_defect == nama }?.let(onTambahDefect) }
                    )
                }
                items(input.daftarDefect, key = { it.idDefect }) { item ->
                    BarisDefectCutting(
                        defect = item,
                        slotWaktu = slotWaktu,
                        onUbahJumlah = { onUbahJumlahDefect(item.idDefect, it) },
                        onUbahPanjang = { onUbahPanjangDefect(item.idDefect, it) },
                        onUbahSlot = { onUbahSlotDefect(item.idDefect, it) },
                        onHapus = { onHapusDefect(item.idDefect) }
                    )
                }
            }

            // Right Pane: Summary and Submission
            Column(
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Kalkulasi Real-time", fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                        HorizontalDivider(color = Color.White)
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Layer:")
                            Text(input.totalLayer.toString(), fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rasio NG:")
                            Text("${input.rasioNgLayer}%", fontWeight = FontWeight.Bold, color = if(input.rasioNgLayer > 5) Color.Red else Color.Unspecified)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rasio Waste:")
                            Text("${input.rasioWastePanjang}%", fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text("Estimasi Panjang (cm)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Hasil OK:")
                            Text("${input.estimasiPanjangOkCm} cm", fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Hasil NG:")
                            Text("${input.estimasiPanjangNgCm} cm", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (daftarPesanValidasi.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Mohon perbaiki:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            daftarPesanValidasi.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onSimpan,
                    enabled = !menyimpan,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (menyimpan) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("SIMPAN HASIL POTONG", fontWeight = FontWeight.Black)
                    }
                }

                // Show daily summary mini list
                val ringkasanHariIni = (ringkasan as? AsyncData.Success)?.data.orEmpty()
                if (ringkasanHariIni.isNotEmpty()) {
                    Text("Ringkasan Harian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(ringkasanHariIni) { data ->
                            Text(
                                text = "• ${data.nama_line ?: "Line"}: ${data.total_batch} batch | OK ${data.total_layer_ok} | NG ${data.total_layer_ng}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Phone Layout
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Informasi Pemotongan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                OutlinedTextField(
                    value = input.tanggalPemeriksaan,
                    onValueChange = { onUbah(input.copy(tanggalPemeriksaan = it)) },
                    label = { Text("Tanggal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item { HorizontalDivider() }
            item { Text("Material dan Ukuran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                AppDropdownField(
                    label = "Material",
                    value = input.namaMaterial,
                    options = material.map { it.labelPilihan },
                    onSelected = { label -> material.firstOrNull { it.labelPilihan == label }?.let(onPilihMaterial) }
                )
            }
            item {
                OutlinedTextField(
                    value = input.spesifikasiMaterial,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Spesifikasi material") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = input.lotRoll,
                    onValueChange = { onUbah(input.copy(lotRoll = it)) },
                    label = { Text("Lot / Roll") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                AppDropdownField(
                    label = "Ukuran cutting (cm)",
                    value = input.ukuranCuttingCm,
                    options = materialTerpilih?.daftarUkuranValid?.map { "${it.ukuranEfektif} cm" }.orEmpty(),
                    onSelected = { label ->
                        materialTerpilih?.daftarUkuranValid
                            ?.firstOrNull { "${it.ukuranEfektif} cm" == label }
                            ?.let { ukuran ->
                                onUbah(
                                    input.copy(
                                        ukuranCuttingCm = ukuran.ukuranEfektif.toString(),
                                        idReferensiUkuranMaterial = ukuran.id
                                    )
                                )
                            }
                    }
                )
                OutlinedTextField(
                    value = input.ukuranCuttingCm,
                    onValueChange = { onUbah(input.copy(ukuranCuttingCm = it, idReferensiUkuranMaterial = null)) },
                    label = { Text("Atau isi ukuran manual (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = input.qtyLayerOk,
                        onValueChange = { onUbah(input.copy(qtyLayerOk = it.filter(Char::isDigit))) },
                        label = { Text("OK") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = input.qtyLayerNg,
                        onValueChange = { onUbah(input.copy(qtyLayerNg = it.filter(Char::isDigit))) },
                        label = { Text("NG") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = input.wastePanjangCm,
                    onValueChange = { onUbah(input.copy(wastePanjangCm = it)) },
                    label = { Text("Waste (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            item { HorizontalDivider() }
            item {
                Text("Temuan NG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AppDropdownField(
                    label = "Tambah defect",
                    value = "Pilih defect",
                    options = defect.filterNot { kandidat -> input.daftarDefect.any { it.idDefect == kandidat.id_defect } }.map { it.nama_defect },
                    onSelected = { nama -> defect.firstOrNull { it.nama_defect == nama }?.let(onTambahDefect) }
                )
            }
            items(input.daftarDefect, key = { it.idDefect }) { item ->
                BarisDefectCutting(
                    defect = item,
                    slotWaktu = slotWaktu,
                    onUbahJumlah = { onUbahJumlahDefect(item.idDefect, it) },
                    onUbahPanjang = { onUbahPanjangDefect(item.idDefect, it) },
                    onUbahSlot = { onUbahSlotDefect(item.idDefect, it) },
                    onHapus = { onHapusDefect(item.idDefect) }
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ringkasan: OK ${input.estimasiPanjangOkCm} cm | NG ${input.estimasiPanjangNgCm} cm", style = MaterialTheme.typography.labelMedium)
                        Text("Rasio NG: ${input.rasioNgLayer}% | Waste: ${input.rasioWastePanjang}%", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (daftarPesanValidasi.isNotEmpty()) {
                item {
                    daftarPesanValidasi.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
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

@Composable
private fun BarisDefectCutting(
    defect: InputDefectCutting,
    slotWaktu: ImmutableList<com.primaraya.inspectra.fitur.checksheet.domain.SlotNg>,
    onUbahJumlah: (String) -> Unit,
    onUbahPanjang: (String) -> Unit,
    onUbahSlot: (String?) -> Unit,
    onHapus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(defect.namaDefect, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            IconButton(onClick = onHapus) {
                Icon(Icons.Filled.DeleteOutline, "Hapus defect", tint = Color(0xFFDC2626))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = defect.jumlahLayerTerdampak.toString(),
                onValueChange = onUbahJumlah,
                label = { Text("Layer terdampak") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = defect.panjangDefectCm?.toString().orEmpty(),
                onValueChange = onUbahPanjang,
                label = { Text("Panjang (cm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
        AppDropdownField(
            label = "Slot waktu",
            value = slotWaktu.firstOrNull { it.slotId == defect.idSlotWaktu }?.labelWaktu.orEmpty(),
            options = slotWaktu.map { it.labelWaktu },
            onSelected = { label -> onUbahSlot(slotWaktu.firstOrNull { it.labelWaktu == label }?.slotId) }
        )
    }
}
