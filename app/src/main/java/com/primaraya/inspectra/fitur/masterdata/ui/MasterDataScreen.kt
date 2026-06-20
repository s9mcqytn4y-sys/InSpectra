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
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.AppEmptyState
import com.primaraya.inspectra.core.ui.component.AppFriendlyDialog
import com.primaraya.inspectra.core.ui.component.AppListSkeleton
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
                        content = { PartList(it, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditPart(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusPart(d)) }) }
                    )
                    MasterDataContract.TabMasterData.MATERIAL -> AsyncList(
                        data = state.materials,
                        content = { MaterialList(it, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditMaterial(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusMaterial(d)) }) }
                    )
                    MasterDataContract.TabMasterData.DEFECT -> AsyncList(
                        data = state.defects,
                        content = { DefectList(it, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditDefect(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusDefect(d)) }) }
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
            is MasterDataContract.DialogForm.FormDefect -> DefectFormSheet(
                initialData = form.data,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanDefect(it)) },
                saving = state.menyimpan
            )
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
    onEdit: (MasterPartDto) -> Unit,
    onDelete: (MasterPartDto) -> Unit
) {
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

// Form State and Sheets

data class PartFormState(
    val id: String? = null,
    val uniqNo: String = "",
    val partNo: String = "",
    val namaPart: String = "",
    val model: String = "",
    val customer: String = "",
    val komoditas: String = "PRESS",
    val sudahDisubmit: Boolean = false
) {
    val uniqNoError: String? get() = if (sudahDisubmit && uniqNo.isBlank()) "UNIQ wajib diisi." else null
    val namaPartError: String? get() = if (sudahDisubmit && namaPart.isBlank()) "Nama part wajib diisi." else null
}

@Composable
fun PartFormSheet(
    initialData: MasterPartDto?,
    onDismiss: () -> Unit,
    onSave: (MasterPartDto) -> Unit,
    saving: Boolean
) {
    var formState by remember { mutableStateOf(
        PartFormState(
            id = initialData?.id,
            uniqNo = initialData?.uniq_no ?: "",
            partNo = initialData?.part_no ?: "",
            namaPart = initialData?.nama_part ?: "",
            model = initialData?.model ?: "",
            customer = initialData?.customer ?: "",
            komoditas = initialData?.komoditas ?: "PRESS"
        )
    ) }

    @OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initialData == null) "Tambah Part" else "Edit Part", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            
            OutlinedTextField(value = formState.uniqNo, onValueChange = { formState = formState.copy(uniqNo = it.uppercase()) }, label = { Text("UNIQ No") }, isError = formState.uniqNoError != null, supportingText = formState.uniqNoError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = formState.partNo, onValueChange = { formState = formState.copy(partNo = it.uppercase()) }, label = { Text("Part No") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = formState.namaPart, onValueChange = { formState = formState.copy(namaPart = it.uppercase()) }, label = { Text("Nama Part") }, isError = formState.namaPartError != null, supportingText = formState.namaPartError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PRESS", "SEWING", "CUTTING").forEach {
                    FilterChip(selected = formState.komoditas == it, onClick = { formState = formState.copy(komoditas = it) }, label = { Text(it) })
                }
            }

            Button(onClick = {
                formState = formState.copy(sudahDisubmit = true)
                if (formState.uniqNoError == null && formState.namaPartError == null) {
                    onSave(MasterPartDto(formState.id, formState.partNo, formState.uniqNo, formState.namaPart, formState.model, formState.customer, formState.komoditas, null, true))
                }
            }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Simpan")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

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

    @OptIn(ExperimentalMaterial3Api::class)
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

    @OptIn(ExperimentalMaterial3Api::class)
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
