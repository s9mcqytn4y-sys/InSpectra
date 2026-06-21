package com.primaraya.inspectra.fitur.cutting.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.AppDropdownField
import com.primaraya.inspectra.core.ui.component.AppEmptyState
import com.primaraya.inspectra.core.ui.component.AppListSkeleton
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.cutting.domain.InputBatchCutting
import com.primaraya.inspectra.fitur.cutting.domain.InputDefectCutting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuttingScreen(
    onBackClick: () -> Unit,
    viewModel: CuttingViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application,
            pembuat = { CuttingViewModel(it) }
        )
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
            TopAppBar(
                title = {
                    Column {
                        Text("Input Cutting", fontWeight = FontWeight.Black)
                        Text("Satu batch untuk setiap lot atau roll", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBackClick) {
                        androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A365D),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        val statusMaterial = state.material
        when (statusMaterial) {
            is AsyncData.Loading -> AppListSkeleton()
            is AsyncData.Error -> AppEmptyState("Material Cutting gagal dimuat", statusMaterial.message)
            else -> FormBatchCutting(
                input = state.input,
                material = (state.material as? AsyncData.Success)?.data.orEmpty(),
                defect = (state.defect as? AsyncData.Success)?.data.orEmpty(),
                slotWaktu = state.slotWaktu,
                ringkasan = state.ringkasan,
                daftarPesanValidasi = state.daftarPesanValidasi,
                menyimpan = state.menyimpan,
                onUbah = { viewModel.onIntent(CuttingContract.Intent.UbahInput(it)) },
                onPilihMaterial = { viewModel.onIntent(CuttingContract.Intent.PilihMaterial(it)) },
                onTambahDefect = { viewModel.onIntent(CuttingContract.Intent.TambahDefect(it)) },
                onUbahJumlahDefect = { id, jumlah -> viewModel.onIntent(CuttingContract.Intent.UbahJumlahDefect(id, jumlah)) },
                onUbahPanjangDefect = { id, panjang -> viewModel.onIntent(CuttingContract.Intent.UbahPanjangDefect(id, panjang)) },
                onUbahSlotDefect = { id, slot -> viewModel.onIntent(CuttingContract.Intent.UbahSlotDefect(id, slot)) },
                onHapusDefect = { viewModel.onIntent(CuttingContract.Intent.HapusDefect(it)) },
                onSimpan = { viewModel.onIntent(CuttingContract.Intent.Simpan) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun FormBatchCutting(
    input: InputBatchCutting,
    material: List<com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting>,
    defect: List<com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto>,
    slotWaktu: List<com.primaraya.inspectra.fitur.checksheet.domain.SlotNg>,
    ringkasan: AsyncData<List<com.primaraya.inspectra.fitur.cutting.domain.RingkasanHarianCutting>>,
    daftarPesanValidasi: List<String>,
    menyimpan: Boolean,
    onUbah: (InputBatchCutting) -> Unit,
    onPilihMaterial: (com.primaraya.inspectra.fitur.cutting.domain.OpsiMaterialCutting) -> Unit,
    onTambahDefect: (com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto) -> Unit,
    onUbahJumlahDefect: (String, String) -> Unit,
    onUbahPanjangDefect: (String, String) -> Unit,
    onUbahSlotDefect: (String, String?) -> Unit,
    onHapusDefect: (String) -> Unit,
    onSimpan: () -> Unit,
    modifier: Modifier
) {
    val materialTerpilih = material.firstOrNull { it.material_id == input.materialId }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Setup Sesi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            OutlinedTextField(
                value = input.tanggalPemeriksaan,
                onValueChange = { onUbah(input.copy(tanggalPemeriksaan = it)) },
                label = { Text("Tanggal pemeriksaan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input.namaLine,
                    onValueChange = { onUbah(input.copy(namaLine = it)) },
                    label = { Text("Line") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = input.namaOperator,
                    onValueChange = { onUbah(input.copy(namaOperator = it)) },
                    label = { Text("Operator") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        item { HorizontalDivider() }
        item { Text("Batch Pemotongan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            AppDropdownField(
                label = "Material",
                value = input.namaMaterial,
                options = material.map { it.nama_material },
                onSelected = { nama -> material.firstOrNull { it.nama_material == nama }?.let(onPilihMaterial) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input.nomorLotRoll,
                    onValueChange = { onUbah(input.copy(nomorLotRoll = it)) },
                    label = { Text("Nomor lot / roll") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = input.nomorRoll,
                    onValueChange = { onUbah(input.copy(nomorRoll = it)) },
                    label = { Text("Nomor roll") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        item {
            AppDropdownField(
                label = "Ukuran cutting (cm)",
                value = input.ukuranCuttingCm,
                options = materialTerpilih?.daftar_ukuran_cutting?.map { it.ukuran_cutting_cm.toString() }.orEmpty(),
                onSelected = { onUbah(input.copy(ukuranCuttingCm = it)) }
            )
            OutlinedTextField(
                value = input.ukuranCuttingCm,
                onValueChange = { onUbah(input.copy(ukuranCuttingCm = it)) },
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
                OutlinedTextField(
                    value = input.wastePanjangCm,
                    onValueChange = { onUbah(input.copy(wastePanjangCm = it)) },
                    label = { Text("Waste (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }
        item { HorizontalDivider() }
        item {
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
        item {
            Text("Rasio NG: ${input.rasioNgLayer}% | Rasio waste: ${input.rasioWastePanjang}%", style = MaterialTheme.typography.labelLarge)
            Text("Estimasi panjang OK: ${input.estimasiPanjangOkCm} cm | NG: ${input.estimasiPanjangNgCm} cm", style = MaterialTheme.typography.labelMedium)
        }
        val ringkasanHariIni = (ringkasan as? AsyncData.Success)?.data.orEmpty()
        if (ringkasanHariIni.isNotEmpty()) {
            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Ringkasan Harian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ringkasanHariIni.forEach { data ->
                    Text(
                        text = "${data.nama_line ?: "Tanpa line"}: ${data.total_batch} batch | " +
                            "OK ${data.total_layer_ok} | NG ${data.total_layer_ng} | " +
                            "Waste ${data.total_waste_cm} cm",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        if (daftarPesanValidasi.isNotEmpty()) {
            item {
                Column {
                    daftarPesanValidasi.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            Button(onClick = onSimpan, enabled = !menyimpan, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (menyimpan) CircularProgressIndicator(modifier = Modifier.width(20.dp), color = Color.White)
                else Text("Simpan Batch")
            }
        }
    }
}

@Composable
private fun BarisDefectCutting(
    defect: InputDefectCutting,
    slotWaktu: List<com.primaraya.inspectra.fitur.checksheet.domain.SlotNg>,
    onUbahJumlah: (String) -> Unit,
    onUbahPanjang: (String) -> Unit,
    onUbahSlot: (String?) -> Unit,
    onHapus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(defect.namaDefect, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onHapus) {
                androidx.compose.material3.Icon(Icons.Filled.DeleteOutline, "Hapus defect")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = defect.jumlahLayerTerdampak.toString(),
                onValueChange = onUbahJumlah,
                label = { Text("Layer terdampak") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = defect.panjangDefectCm?.toString().orEmpty(),
                onValueChange = onUbahPanjang,
                label = { Text("Panjang defect (cm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
        AppDropdownField(
            label = "Slot waktu",
            value = slotWaktu.firstOrNull { it.slotId == defect.idSlotWaktu }?.labelWaktu.orEmpty(),
            options = slotWaktu.map { it.labelWaktu },
            onSelected = { label -> onUbahSlot(slotWaktu.firstOrNull { it.labelWaktu == label }?.slotId) }
        )
        HorizontalDivider()
    }
}
