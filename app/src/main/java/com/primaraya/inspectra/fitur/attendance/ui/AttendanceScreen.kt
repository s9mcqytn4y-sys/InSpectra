package com.primaraya.inspectra.fitur.attendance.ui

import android.app.Application
import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.common.KeteranganHadir
import com.primaraya.inspectra.core.common.LemburNonMainJob
import com.primaraya.inspectra.core.common.LineProcess
import com.primaraya.inspectra.core.common.TipePekerja
import com.primaraya.inspectra.core.ui.component.*
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBackClick: () -> Unit,
    viewModel: AttendanceViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application,
            pembuat = { AttendanceViewModel(it) }
        )
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(AttendanceContract.Intent.ClearMessage)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(AttendanceContract.Intent.ClearMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rasio Kehadiran", fontWeight = FontWeight.Black)
                        Text("Pencatatan Man Power Harian", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text("MP Actual: ${state.totalMp}") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.padding(end = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onIntent(AttendanceContract.Intent.Simpan) },
                icon = { Icon(Icons.Default.Save, null) },
                text = { Text("Simpan Kehadiran") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Bar
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tanggal Picker (Simulated as Button for now)
                    OutlinedButton(
                        onClick = { /* Open Date Picker */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.tanggal)
                    }

                    // Line Dropdown
                    AppDropdownField(
                        label = "Line / Proses",
                        value = state.selectedLine.name,
                        options = LineProcess.entries.map { it.name },
                        onSelected = { viewModel.onIntent(AttendanceContract.Intent.PilihLine(LineProcess.valueOf(it))) },
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }

            if (state.isLoading && state.attendanceRows.isEmpty()) {
                AppLoading(label = "Memuat data pekerja...")
            } else if (state.attendanceRows.isEmpty()) {
                AppEmptyState(
                    title = "Data Kosong",
                    message = "Belum ada pekerja terdaftar untuk proses ini."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.attendanceRows, key = { it.employeeId }) { row ->
                        AttendanceRowElite(
                            row = row,
                            onIntent = viewModel::onIntent
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun AttendanceRowElite(
    row: AttendanceContract.AttendanceRowState,
    onIntent: (AttendanceContract.Intent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kolom Kiri: Nama & Tipe
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.namaLengkap,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(row.tipePekerja.name, fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    if (row.noReg != null) {
                        Text(
                            text = row.noReg,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Kolom Tengah: Keterangan
            Box(modifier = Modifier.width(140.dp)) {
                AppDropdownField(
                    label = "Hadir?",
                    value = row.keterangan.name,
                    options = KeteranganHadir.entries.map { it.name },
                    onSelected = { onIntent(AttendanceContract.Intent.UpdateKeterangan(row.employeeId, KeteranganHadir.valueOf(it))) }
                )
            }

            // Kolom Kanan: Lembur
            Row(
                modifier = Modifier.width(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = row.jamLembur,
                    onValueChange = { onIntent(AttendanceContract.Intent.UpdateJamLembur(row.employeeId, it)) },
                    label = { Text("Jam") },
                    modifier = Modifier.width(70.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    AppDropdownField(
                        label = "Tipe Lembur",
                        value = row.lemburNonMainJob.name,
                        options = LemburNonMainJob.entries.map { it.name },
                        onSelected = { onIntent(AttendanceContract.Intent.UpdateLemburNonMainJob(row.employeeId, LemburNonMainJob.valueOf(it))) }
                    )
                }
            }
        }
    }
}
