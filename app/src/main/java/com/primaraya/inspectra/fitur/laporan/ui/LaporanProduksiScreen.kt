package com.primaraya.inspectra.fitur.laporan.ui

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.primaraya.inspectra.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(tipeProses) {
        viewModel.onIntent(LaporanContract.Intent.Muat(tipeProses))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LaporanContract.Effect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        HeaderSection(state = state, onIntent = viewModel::onIntent)
                    }
                    
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.laporan_tenaga_kerja), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputTeksUtama(
                    value = state.otProd,
                    onValueChange = { onIntent(LaporanContract.Intent.UpdateOtProd(it)) },
                    label = stringResource(R.string.laporan_ot_prod),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                InputTeksUtama(
                    value = state.otNon,
                    onValueChange = { onIntent(LaporanContract.Intent.UpdateOtNon(it)) },
                    label = stringResource(R.string.laporan_ot_non),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun SummarySection(state: LaporanContract.State) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.laporan_total_planning), style = MaterialTheme.typography.labelMedium)
                Text("${state.totalPlanning}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.laporan_total_actual), style = MaterialTheme.typography.labelMedium)
                Text("${state.totalActual}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.laporan_total_ng), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                Text("${state.totalNg}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(item.namaPart, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text(
                    text = "NG tidak boleh lebih besar dari Actual",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
