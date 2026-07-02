package com.primaraya.inspectra.fitur.laporan.ui

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.primaraya.inspectra.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.ui.component.InputTeksUtama
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanProduksiScreen(
    tipeProses: String,
    onBackClick: () -> Unit,
    viewModel: LaporanViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application,
            pembuat = { LaporanViewModel(it) }
        )
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    @Suppress("DEPRECATION")
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }

    LaunchedEffect(tipeProses) {
        viewModel.onIntent(LaporanContract.Intent.Muat(tipeProses))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LaporanContract.Effect.ShowSnackbar -> {
                    val msg = effect.message
                    if (msg.contains("Gagal", ignoreCase = true) || 
                        msg.contains("Pilih minimal", ignoreCase = true) || 
                        msg.contains("Data belum lengkap", ignoreCase = true) ||
                        msg.contains("Tidak valid", ignoreCase = true)) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(200)
                        }
                    }
                    snackbarHostState.showSnackbar(msg)
                }
                is LaporanContract.Effect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.laporan_produksi_title, tipeProses)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (state.step == LaporanContract.Step.ISI_FORM) {
                            viewModel.onIntent(LaporanContract.Intent.KembaliKePilihPart)
                        } else {
                            onBackClick() 
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.zIndex(1f) // Ensure it's on top
            ) 
        },
        floatingActionButton = {
            if (state.step == LaporanContract.Step.ISI_FORM) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onIntent(LaporanContract.Intent.Submit) },
                    icon = { /* You can add an icon here if desired */ },
                    text = { Text(stringResource(R.string.laporan_kirim)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading && state.details.isEmpty() && state.masterParts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AnimatedVisibility(visible = state.step == LaporanContract.Step.PILIH_PART) {
                com.primaraya.inspectra.core.ui.component.InspectraMultiPickerSheet(
                    title = stringResource(R.string.laporan_pilih_part),
                    items = state.masterParts.map { it.uniqNo to it.namaPart },
                    selectedIds = state.selectedPartIds,
                    onToggle = { viewModel.onIntent(LaporanContract.Intent.TogglePilihPart(it)) },
                    onConfirm = { viewModel.onIntent(LaporanContract.Intent.LanjutKeForm) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            AnimatedVisibility(visible = state.step == LaporanContract.Step.ISI_FORM) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SummarySection(state = state)
                    }

                    itemsIndexed(
                        items = state.details,
                        key = { _, item -> item.idPart }
                    ) { index, item ->
                        PartDetailItem(
                            index = index,
                            item = item,
                            onIntent = viewModel::onIntent
                        )
                    }

                    item {
                        HeaderSection(state = state, onIntent = viewModel::onIntent)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    state: LaporanContract.State,
    onIntent: (LaporanContract.Intent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.laporan_tenaga_kerja),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputTeksUtama(
                        value = state.mpDirect,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateMpDirect(it)) },
                        label = stringResource(R.string.laporan_mp_direct),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    InputTeksUtama(
                        value = state.mpIndirect,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateMpIndirect(it)) },
                        label = stringResource(R.string.laporan_mp_indirect),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputTeksUtama(
                        value = state.jknHour,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateJknHour(it)) },
                        label = stringResource(R.string.laporan_jkn_jam),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    InputTeksUtama(
                        value = state.jknMenit,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateJknMenit(it)) },
                        label = stringResource(R.string.laporan_jkn_menit),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.laporan_overtime_bantuan),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputTeksUtama(
                        value = state.lemburProd,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateLemburProd(it)) },
                        label = stringResource(R.string.laporan_lembur_prod),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    InputTeksUtama(
                        value = state.lemburNon,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateLemburNon(it)) },
                        label = stringResource(R.string.laporan_lembur_non),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputTeksUtama(
                        value = state.bantuanKeluar,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateBantuanKeluar(it)) },
                        label = stringResource(R.string.laporan_bantuan_keluar),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    InputTeksUtama(
                        value = state.bantuanMasuk,
                        onValueChange = { onIntent(LaporanContract.Intent.UpdateBantuanMasuk(it)) },
                        label = stringResource(R.string.laporan_bantuan_masuk),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    }
}

@Composable
fun SummarySection(state: LaporanContract.State) {
    val totalPlanning by remember(state.details) { derivedStateOf { state.totalPlanning } }
    val totalActual by remember(state.details) { derivedStateOf { state.totalActual } }
    val totalNg by remember(state.details) { derivedStateOf { state.totalNg } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.laporan_total_planning), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$totalPlanning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.laporan_total_actual), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                Text("$totalActual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.laporan_total_ng), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                Text("$totalNg", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PartDetailItem(
    index: Int,
    item: LaporanContract.DetailLaporanState,
    onIntent: (LaporanContract.Intent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.namaPart,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = item.idPart,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InputTeksUtama(
                    value = item.planning,
                    onValueChange = { onIntent(LaporanContract.Intent.UpdateDetailPlanning(index, it)) },
                    label = stringResource(R.string.laporan_planning),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                InputTeksUtama(
                    value = item.actual,
                    onValueChange = { onIntent(LaporanContract.Intent.UpdateDetailActual(index, it)) },
                    label = stringResource(R.string.laporan_actual),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                InputTeksUtama(
                    value = item.ng,
                    onValueChange = { onIntent(LaporanContract.Intent.UpdateDetailNg(index, it)) },
                    label = stringResource(R.string.laporan_ng),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = (item.ng.toIntOrNull() ?: 0) > (item.actual.toIntOrNull() ?: 0)
                )
            }
            
            AnimatedVisibility(visible = (item.ng.toIntOrNull() ?: 0) > (item.actual.toIntOrNull() ?: 0)) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "NG tidak boleh lebih besar dari Actual",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
