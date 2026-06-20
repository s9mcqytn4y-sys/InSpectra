package com.primaraya.inspectra.fitur.masterdata.ui

import androidx.compose.animation.AnimatedVisibility
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
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.*
import com.primaraya.inspectra.fitur.masterdata.domain.*

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
                is MasterDataContract.Effect.TampilPesan -> snackbarHostState.showSnackbar(effect.pesan)
                is MasterDataContract.Effect.DataTersimpan -> snackbarHostState.showSnackbar("Data berhasil disimpan")
                is MasterDataContract.Effect.DataDihapus -> snackbarHostState.showSnackbar("Data berhasil dihapus")
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
                        MasterDataContract.TabMasterData.SUPPLIER -> viewModel.onIntent(MasterDataContract.Intent.TambahSupplier)
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
                when (state.tabAktif) {
                    MasterDataContract.TabMasterData.PART -> AsyncList(
                        data = state.parts,
                        content = { list ->
                            PartList(
                                parts = list, 
                                detailState = state.partDetails,
                                onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditPart(d)) }, 
                                onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusPart(d)) },
                                onToggleDetail = { uniqNo -> viewModel.onIntent(MasterDataContract.Intent.TogglePartDetail(uniqNo)) },
                                onRemoveDefect = { uniqNo, relId -> viewModel.onIntent(MasterDataContract.Intent.HapusDefectDariPart(uniqNo, relId)) },
                                onAddDefect = { uniqNo -> viewModel.onIntent(MasterDataContract.Intent.PilihDefectUntukPart(uniqNo)) }
                            ) 
                        }
                    )
                    MasterDataContract.TabMasterData.MATERIAL -> AsyncList(
                        data = state.materials,
                        content = { list -> MaterialList(list, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditMaterial(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusMaterial(d)) }) }
                    )
                    MasterDataContract.TabMasterData.SUPPLIER -> AsyncList(
                        data = state.suppliers,
                        content = { list -> SupplierList(list, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditSupplier(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusSupplier(d)) }) }
                    )
                    MasterDataContract.TabMasterData.DEFECT -> AsyncList(
                        data = state.defects,
                        content = { list -> DefectList(list, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditDefect(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusDefect(d)) }) }
                    )
                }
            }
        }
    }

    state.dialogForm?.let { form ->
        when (form) {
            is MasterDataContract.DialogForm.FormPart -> PartFormSheet(
                initialData = form.data,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanPart(it)) },
                saving = state.menyimpan
            )
            is MasterDataContract.DialogForm.FormMaterial -> MaterialFormSheet(
                initialData = form.data,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanMaterial(it)) },
                saving = state.menyimpan
            )
            is MasterDataContract.DialogForm.FormSupplier -> SupplierFormSheet(
                initialData = form.data,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanSupplier(it)) },
                saving = state.menyimpan
            )
            is MasterDataContract.DialogForm.FormDefect -> DefectFormSheet(
                initialData = form.data,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanDefect(it)) },
                saving = state.menyimpan
            )
            is MasterDataContract.DialogForm.PilihDefectUntukPart -> {
                PilihDefectDialog(
                    availableDefects = (state.defects as? AsyncData.Success)?.data ?: emptyList(),
                    onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                    onSelect = { idDefect -> viewModel.onIntent(MasterDataContract.Intent.TambahDefectKePart(form.uniqNo, idDefect)) }
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
fun <T> AsyncList(
    data: AsyncData<T>,
    content: @Composable (T) -> Unit
) {
    when (data) {
        is AsyncData.Loading -> AppListSkeleton()
        is AsyncData.Success -> content(data.data)
        is AsyncData.Empty -> AppEmptyState(title = data.title, message = data.message)
        is AsyncData.Error -> AppEmptyState(title = data.title, message = data.message)
        else -> Unit
    }
}

@Composable
fun PartList(
    parts: List<MasterPartDto>,
    detailState: Map<String, MasterDataContract.PartRelationState>,
    onEdit: (MasterPartDto) -> Unit,
    onDelete: (MasterPartDto) -> Unit,
    onToggleDetail: (String) -> Unit,
    onRemoveDefect: (String, String) -> Unit,
    onAddDefect: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(parts) { part ->
            val detail = detailState[part.uniq_no] ?: MasterDataContract.PartRelationState()
            
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleDetail(part.uniq_no) }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(part.uniq_no, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(part.nama_part, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { onEdit(part) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { onDelete(part) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                        Icon(
                            imageVector = if (detail.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                    Text("Model: ${part.model ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    Text("Komoditas: ${part.komoditas}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706))
                    
                    AnimatedVisibility(visible = detail.expanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text("Defect Template", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            
                            when (val defects = detail.defects) {
                                is AsyncData.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                                is AsyncData.Success -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        defects.data.forEach { rel ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("• ${rel.id_defect}", modifier = Modifier.weight(1f))
                                                IconButton(onClick = { onRemoveDefect(part.uniq_no, rel.id ?: "") }) {
                                                    Icon(Icons.Default.LinkOff, contentDescription = "Hapus", tint = Color.LightGray)
                                                }
                                            }
                                        }
                                        TextButton(onClick = { onAddDefect(part.uniq_no) }) {
                                            Icon(Icons.Default.AddLink, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Tambah Defect")
                                        }
                                    }
                                }
                                is AsyncData.Error -> Text("Gagal memuat template: ${defects.message}", color = Color.Red)
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierList(
    suppliers: List<MasterSupplierDto>,
    onEdit: (MasterSupplierDto) -> Unit,
    onDelete: (MasterSupplierDto) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(suppliers) { supplier ->
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(supplier.nama_supplier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Kode: ${supplier.kode_supplier ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onEdit(supplier) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { onDelete(supplier) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                    }
                    Text("Kategori: ${supplier.kategori ?: "-"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PilihDefectDialog(
    availableDefects: List<MasterDefectDto>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Defect") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(availableDefects) { defect ->
                    ListItem(
                        headlineContent = { Text(defect.nama_defect) },
                        supportingContent = { Text(defect.id_defect) },
                        modifier = Modifier.clickable { onSelect(defect.id_defect) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun MaterialList(
    materials: List<MasterMaterialDto>,
    onEdit: (MasterMaterialDto) -> Unit,
    onDelete: (MasterMaterialDto) -> Unit
) {
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

@Composable
fun DefectList(
    defects: List<MasterDefectDto>,
    onEdit: (MasterDefectDto) -> Unit,
    onDelete: (MasterDefectDto) -> Unit
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartFormSheet(
    initialData: MasterPartDto?,
    onDismiss: () -> Unit,
    onSave: (MasterPartDto) -> Unit,
    saving: Boolean
) {
    var uniqNo by remember { mutableStateOf(initialData?.uniq_no ?: "") }
    var partNo by remember { mutableStateOf(initialData?.part_no ?: "") }
    var namaPart by remember { mutableStateOf(initialData?.nama_part ?: "") }
    var model by remember { mutableStateOf(initialData?.model ?: "") }
    var customer by remember { mutableStateOf(initialData?.customer ?: "") }
    var komoditas by remember { mutableStateOf(initialData?.komoditas ?: "PRESS") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initialData == null) "Tambah Part" else "Edit Part", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            OutlinedTextField(value = uniqNo, onValueChange = { uniqNo = it.uppercase() }, label = { Text("UNIQ No") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = partNo, onValueChange = { partNo = it.uppercase() }, label = { Text("Part No") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = namaPart, onValueChange = { namaPart = it.uppercase() }, label = { Text("Nama Part") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PRESS", "SEWING", "CUTTING").forEach {
                    FilterChip(selected = komoditas == it, onClick = { komoditas = it }, label = { Text(it) })
                }
            }
            Button(onClick = { onSave(MasterPartDto(initialData?.id, partNo, uniqNo, namaPart, model, customer, komoditas, null, true)) }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Simpan")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialFormSheet(
    initialData: MasterMaterialDto?,
    onDismiss: () -> Unit,
    onSave: (MasterMaterialDto) -> Unit,
    saving: Boolean
) {
    var namaMaterial by remember { mutableStateOf(initialData?.nama_material ?: "") }
    var supplier by remember { mutableStateOf(initialData?.supplier ?: "") }
    var spec by remember { mutableStateOf(initialData?.spec ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initialData == null) "Tambah Material" else "Edit Material", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            OutlinedTextField(value = namaMaterial, onValueChange = { namaMaterial = it.uppercase() }, label = { Text("Nama Material") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = supplier, onValueChange = { supplier = it.uppercase() }, label = { Text("Supplier") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = spec, onValueChange = { spec = it.uppercase() }, label = { Text("Spec") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onSave(MasterMaterialDto(initialData?.id, supplier, namaMaterial, spec, null, true)) }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Simpan")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFormSheet(
    initialData: MasterSupplierDto?,
    onDismiss: () -> Unit,
    onSave: (MasterSupplierDto) -> Unit,
    saving: Boolean
) {
    var namaSupplier by remember { mutableStateOf(initialData?.nama_supplier ?: "") }
    var kodeSupplier by remember { mutableStateOf(initialData?.kode_supplier ?: "") }
    var kategori by remember { mutableStateOf(initialData?.kategori ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initialData == null) "Tambah Supplier" else "Edit Supplier", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            OutlinedTextField(value = namaSupplier, onValueChange = { namaSupplier = it.uppercase() }, label = { Text("Nama Supplier") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = kodeSupplier, onValueChange = { kodeSupplier = it.uppercase() }, label = { Text("Kode Supplier") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = kategori, onValueChange = { kategori = it.uppercase() }, label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onSave(MasterSupplierDto(initialData?.id, kodeSupplier, namaSupplier, kategori, true)) }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Simpan")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefectFormSheet(
    initialData: MasterDefectDto?,
    onDismiss: () -> Unit,
    onSave: (MasterDefectDto) -> Unit,
    saving: Boolean
) {
    var idDefect by remember { mutableStateOf(initialData?.id_defect ?: "") }
    var namaDefect by remember { mutableStateOf(initialData?.nama_defect ?: "") }
    var kategori by remember { mutableStateOf(initialData?.kategori ?: "PROSES") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initialData == null) "Tambah Defect" else "Edit Defect", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            OutlinedTextField(value = idDefect, onValueChange = { idDefect = it.uppercase() }, label = { Text("ID Defect") }, enabled = initialData == null, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = namaDefect, onValueChange = { namaDefect = it.uppercase() }, label = { Text("Nama Defect") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MATERIAL", "PROSES").forEach {
                    FilterChip(selected = kategori == it, onClick = { kategori = it }, label = { Text(it) })
                }
            }
            Button(onClick = { onSave(MasterDefectDto(idDefect, namaDefect, kategori, true)) }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Simpan")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
