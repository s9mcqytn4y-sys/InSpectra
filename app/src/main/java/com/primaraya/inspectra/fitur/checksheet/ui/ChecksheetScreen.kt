package com.primaraya.inspectra.fitur.checksheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.primaraya.inspectra.R
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.*
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.checksheet.domain.*
import com.primaraya.inspectra.fitur.checksheet.ui.ChecksheetContract.State.Step
import kotlinx.collections.immutable.ImmutableList

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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    @Suppress("DEPRECATION")
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }

    LaunchedEffect(tipeProses) {
        viewModel.onIntent(ChecksheetContract.Intent.Muat(tipeProses))
    }

    val successDesc = stringResource(R.string.checksheet_success_desc)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChecksheetContract.Effect.PesanSukses -> {
                    snackbarHostState.showSnackbar(effect.pesan)
                }
                is ChecksheetContract.Effect.PesanError -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200)
                    }
                    snackbarHostState.showSnackbar("${effect.judul}: ${effect.pesan}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.zIndex(1f)
            ) 
        },
        topBar = {
            if (!state.berhasil) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (state.step == Step.PILIH_PART) stringResource(R.string.checksheet_title_pilih_part, tipeProses.labelIndonesia()) else stringResource(R.string.checksheet_title_inspeksi, tipeProses.labelIndonesia()),
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (state.step == Step.PILIH_PART) stringResource(R.string.checksheet_desc_pilih_part) else stringResource(R.string.checksheet_desc_inspeksi),
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (!state.berhasil) {
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
                            Text(stringResource(R.string.checksheet_mulai_cek, state.partTerpilih.size), fontWeight = FontWeight.Black)
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
        }
    ) { padding ->
        if (state.berhasil) {
            ChecksheetSuccessScreen(
                onDone = { viewModel.onIntent(ChecksheetContract.Intent.TutupBerhasil) }
            )
        } else {
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
fun ChecksheetSuccessScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer
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
            stringResource(R.string.checksheet_success_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            stringResource(R.string.checksheet_success_desc),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.checksheet_back_to_list), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DialogTambahDefectLain(
    defectsMaster: AsyncData<ImmutableList<com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto>>,
    onDismiss: () -> Unit,
    onConfirm: (com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.checksheet_add_temuan), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.checksheet_add_temuan_desc))
                
                SearchBarElite(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.checksheet_search_defect)
                )

                Surface(
                    modifier = Modifier.heightIn(max = 350.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    when (defectsMaster) {
                        is AsyncData.Loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is AsyncData.Error -> Text(defectsMaster.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                        is AsyncData.Success -> {
                            val allDefects = defectsMaster.data
                            val filtered = remember(query, allDefects) {
                                allDefects.filter { 
                                    it.nama_defect.contains(query, ignoreCase = true) || it.id_defect.contains(query, ignoreCase = true)
                                }
                            }
                            
                            if (filtered.isEmpty()) {
                                AppEmptyState(stringResource(R.string.checksheet_empty_state_title), stringResource(R.string.checksheet_empty_state_desc), modifier = Modifier.height(200.dp))
                            } else {
                                LazyColumn(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filtered, key = { it.id_defect }) { defect ->
                                        ListItem(
                                            headlineContent = { Text(defect.nama_defect, fontWeight = FontWeight.Bold) },
                                            supportingContent = { Text(defect.id_defect, style = MaterialTheme.typography.labelSmall) },
                                            trailingContent = {
                                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            modifier = Modifier.clickable { onConfirm(defect) }
                                        )
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.checksheet_btn_selesai)) }
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
        MaterialTheme.colorScheme.errorContainer
    } else if (part.jumlahNg > 0) {
        MaterialTheme.colorScheme.surface
    } else {
        Color.White
    }

    val borderColor = if (part.kuantitasTidakValid) {
        MaterialTheme.colorScheme.error
    } else if (part.jumlahNg > 0) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (part.terbuka) 4.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBukaTutup),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.uniqNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = part.namaPart,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallBadgeElite(label = stringResource(R.string.checksheet_badge_cek, part.jumlahDiperiksa.toString()), color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        SmallBadgeElite(label = stringResource(R.string.checksheet_badge_ok, part.jumlahOk.toString()), color = MaterialTheme.colorScheme.tertiaryContainer, textColor = MaterialTheme.colorScheme.onTertiaryContainer)
                        if (part.jumlahNg > 0) {
                            SmallBadgeElite(label = stringResource(R.string.checksheet_badge_ng, part.jumlahNg.toString()), color = MaterialTheme.colorScheme.errorContainer, textColor = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                IconButton(
                    onClick = onBukaTutup,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (part.terbuka) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (part.terbuka) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = if (part.jumlahDiperiksa == 0) "" else part.jumlahDiperiksa.toString(),
                    onValueChange = { onJumlahDiperiksaUbah(it.toIntOrNull() ?: 0) },
                    label = { Text(stringResource(R.string.checksheet_label_jumlah_diperiksa), style = MaterialTheme.typography.labelMedium) },
                    placeholder = { Text("0") },
                    isError = part.kuantitasTidakValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                if (part.kuantitasTidakValid) {
                    Text(
                        stringResource(R.string.checksheet_error_ng_exceeds),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Daftar Temuan NG",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val defectsByKategori = remember(part.daftarDefect, part.defectTersembunyi) {
                    part.daftarDefect.filter { it.idDefect !in part.defectTersembunyi }
                        .groupBy { it.kategori }
                }

                defectsByKategori.forEach { (kategori, defects) ->
                    val isMaterial = kategori == KategoriDefect.MATERIAL
                    
                    Surface(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        color = if (isMaterial) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isMaterial) "DEFECT MATERIAL" else "DEFECT PROSES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isMaterial) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                
                OutlinedButton(
                    onClick = onBukaTambahDefect,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Jenis Lain", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SmallBadgeElite(label: String, color: Color, textColor: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
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
                    color = if (defect.jumlahNg > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (defect.jumlahNg > 0) FontWeight.Bold else FontWeight.Medium
                )
                if (defect.detailSlot.isNotEmpty()) {
                    Surface(
                        onClick = { showSlotDialog = true },
                        color = if (defect.slotMatch) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (defect.slotMatch) "✓ Slot OK: ${defect.totalNgDariSlot}" else "⚠ Slot: ${defect.totalNgDariSlot}/${defect.jumlahNg}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (defect.slotMatch) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
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
                        tint = if (defect.jumlahNg > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .width(60.dp)
                        .height(48.dp)
                        .clickable { showManualDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    color = if (defect.jumlahNg > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = defect.jumlahNg.toString(),
                            fontWeight = FontWeight.Black,
                            color = if (defect.jumlahNg > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                IconButton(onClick = onPlus) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Tambah",
                        tint = MaterialTheme.colorScheme.tertiary,
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
                        items(defect.detailSlot, key = { it.slotId }) { slot ->
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
                    color = if (defect.slotMatch) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
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
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun BadgeElite(label: String, color: Color, textColor: Color = Color.Unspecified) {
    Surface(
        color = color,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
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
        SearchBarElite(
            query = state.pencarian,
            onQueryChange = onCari,
            placeholder = "Cari UNIQ NO atau Nama Part...",
            modifier = Modifier.padding(16.dp)
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
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
