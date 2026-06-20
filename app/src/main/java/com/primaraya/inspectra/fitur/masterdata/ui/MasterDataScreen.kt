package com.primaraya.inspectra.fitur.masterdata.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.ui.component.AppEmptyState
import com.primaraya.inspectra.core.ui.component.AppFriendlyDialog
import com.primaraya.inspectra.core.ui.component.AppLoading
import com.primaraya.inspectra.fitur.masterdata.domain.MasterDefectDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterMaterialDto
import com.primaraya.inspectra.fitur.masterdata.domain.MasterPartDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    onBackClick: () -> Unit,
    viewModel: MasterDataViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MasterDataContract.Effect.TampilPesan -> {
                    snackbarHostState.showSnackbar(effect.pesan)
                }
                is MasterDataContract.Effect.DataTersimpan -> {
                    snackbarHostState.showSnackbar("Data berhasil disimpan")
                }
                is MasterDataContract.Effect.DataDihapus -> {
                    snackbarHostState.showSnackbar("Data berhasil dihapus")
                }
                is MasterDataContract.Effect.TampilError -> {
                    // Handled by UserMessage dialog for now
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Master Data", fontWeight = FontWeight.Black) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (state.tabAktif) {
                        MasterDataContract.TabMasterData.PART -> viewModel.onIntent(MasterDataContract.Intent.TambahPart)
                        MasterDataContract.TabMasterData.MATERIAL -> viewModel.onIntent(MasterDataContract.Intent.TambahMaterial)
                        MasterDataContract.TabMasterData.DEFECT -> viewModel.onIntent(MasterDataContract.Intent.TambahDefect)
                    }
                },
                containerColor = Color(0xFFD97706),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = state.tabAktif.ordinal,
                containerColor = Color(0xFF1A365D),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (state.tabAktif.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[state.tabAktif.ordinal]),
                            color = Color(0xFFD97706)
                        )
                    }
                }
            ) {
                MasterDataContract.TabMasterData.values().forEach { tab ->
                    Tab(
                        selected = state.tabAktif == tab,
                        onClick = { viewModel.onIntent(MasterDataContract.Intent.PilihTab(tab)) }
                    ) {
                        Text(tab.name, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
                if (state.loading) {
                    AppLoading()
                } else {
                    when (state.tabAktif) {
                        MasterDataContract.TabMasterData.PART -> PartList(
                            state.daftarPart,
                            onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditPart(it)) },
                            onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusPart(it)) }
                        )
                        MasterDataContract.TabMasterData.MATERIAL -> MaterialList(
                            state.daftarMaterial,
                            onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditMaterial(it)) },
                            onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusMaterial(it)) }
                        )
                        MasterDataContract.TabMasterData.DEFECT -> DefectList(
                            state.daftarDefect,
                            onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditDefect(it)) },
                            onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusDefect(it)) }
                        )
                    }
                }
            }
        }
    }

    state.dialogForm?.let { form ->
        when (form) {
            is MasterDataContract.DialogForm.FormPart -> {
                PartFormDialog(
                    initialData = form.data,
                    onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                    onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanPart(it)) }
                )
            }
            is MasterDataContract.DialogForm.FormMaterial -> {
                MaterialFormDialog(
                    initialData = form.data,
                    onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                    onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanMaterial(it)) }
                )
            }
            is MasterDataContract.DialogForm.FormDefect -> {
                DefectFormDialog(
                    initialData = form.data,
                    onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                    onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanDefect(it)) }
                )
            }
        }
    }

    state.userMessage?.let { msg ->
        AppFriendlyDialog(
            title = msg.title,
            message = msg.body,
            buttonText = msg.actionLabel ?: "Mengerti",
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.ClearUserMessage) }
        )
    }
}

@Composable
fun PartList(
    parts: List<MasterPartDto>,
    onEdit: (MasterPartDto) -> Unit,
    onDelete: (MasterPartDto) -> Unit
) {
    if (parts.isEmpty()) {
        AppEmptyState(title = "Data part belum tersedia", message = "Tambahkan data part terlebih dahulu.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(parts) { part ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(part.uniq_no, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text(part.nama_part, style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = { onEdit(part) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                            IconButton(onClick = { onDelete(part) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                        }
                        Text("Model: ${part.model ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        Text("Komoditas: ${part.komoditas}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706))
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialList(
    materials: List<MasterMaterialDto>,
    onEdit: (MasterMaterialDto) -> Unit,
    onDelete: (MasterMaterialDto) -> Unit
) {
    if (materials.isEmpty()) {
        AppEmptyState(title = "Data material belum tersedia", message = "Tambahkan data material terlebih dahulu.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(materials) { material ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(material.nama_material, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Supplier: ${material.supplier ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { onEdit(material) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                            IconButton(onClick = { onDelete(material) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                        }
                        Text("Spec: ${material.spec ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DefectList(
    defects: List<MasterDefectDto>,
    onEdit: (MasterDefectDto) -> Unit,
    onDelete: (MasterDefectDto) -> Unit
) {
    if (defects.isEmpty()) {
        AppEmptyState(title = "Data defect belum tersedia", message = "Tambahkan data defect terlebih dahulu.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(defects) { defect ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(defect.nama_defect, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("ID: ${defect.id_defect}", style = MaterialTheme.typography.bodySmall)
                        }
                        SuggestionChip(onClick = {}, label = { Text(defect.kategori) })
                        IconButton(onClick = { onEdit(defect) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { onDelete(defect) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                    }
                }
            }
        }
    }
}

@Composable
fun PartFormDialog(
    initialData: MasterPartDto?,
    onDismiss: () -> Unit,
    onSave: (MasterPartDto) -> Unit
) {
    var uniqNo by remember { mutableStateOf(initialData?.uniq_no ?: "") }
    var partNo by remember { mutableStateOf(initialData?.part_no ?: "") }
    var namaPart by remember { mutableStateOf(initialData?.nama_part ?: "") }
    var model by remember { mutableStateOf(initialData?.model ?: "") }
    var customer by remember { mutableStateOf(initialData?.customer ?: "") }
    var komoditas by remember { mutableStateOf(initialData?.komoditas ?: "PRESS") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialData == null) "Tambah Part" else "Edit Part") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = uniqNo, onValueChange = { uniqNo = it }, label = { Text("UNIQ No") })
                OutlinedTextField(value = partNo, onValueChange = { partNo = it }, label = { Text("Part No") })
                OutlinedTextField(value = namaPart, onValueChange = { namaPart = it }, label = { Text("Nama Part") })
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") })
                OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("Customer") })
                Text("Komoditas: $komoditas")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("PRESS", "SEWING", "CUTTING").forEach {
                        FilterChip(selected = komoditas == it, onClick = { komoditas = it }, label = { Text(it) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    MasterPartDto(
                        id = initialData?.id,
                        uniq_no = uniqNo,
                        part_no = partNo,
                        nama_part = namaPart,
                        model = model,
                        customer = customer,
                        komoditas = komoditas,
                        aktif = true
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun MaterialFormDialog(
    initialData: MasterMaterialDto?,
    onDismiss: () -> Unit,
    onSave: (MasterMaterialDto) -> Unit
) {
    var namaMaterial by remember { mutableStateOf(initialData?.nama_material ?: "") }
    var supplier by remember { mutableStateOf(initialData?.supplier ?: "") }
    var spec by remember { mutableStateOf(initialData?.spec ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialData == null) "Tambah Material" else "Edit Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = namaMaterial, onValueChange = { namaMaterial = it }, label = { Text("Nama Material") })
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") })
                OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Spec") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    MasterMaterialDto(
                        id = initialData?.id,
                        nama_material = namaMaterial,
                        supplier = supplier,
                        spec = spec,
                        aktif = true
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun DefectFormDialog(
    initialData: MasterDefectDto?,
    onDismiss: () -> Unit,
    onSave: (MasterDefectDto) -> Unit
) {
    var idDefect by remember { mutableStateOf(initialData?.id_defect ?: "") }
    var namaDefect by remember { mutableStateOf(initialData?.nama_defect ?: "") }
    var kategori by remember { mutableStateOf(initialData?.kategori ?: "PROSES") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialData == null) "Tambah Defect" else "Edit Defect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = idDefect, 
                    onValueChange = { idDefect = it }, 
                    label = { Text("ID Defect") },
                    enabled = initialData == null
                )
                OutlinedTextField(value = namaDefect, onValueChange = { namaDefect = it }, label = { Text("Nama Defect") })
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("MATERIAL", "PROSES").forEach {
                        FilterChip(selected = kategori == it, onClick = { kategori = it }, label = { Text(it) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    MasterDefectDto(
                        id_defect = idDefect,
                        nama_defect = namaDefect,
                        kategori = kategori,
                        aktif = true
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
