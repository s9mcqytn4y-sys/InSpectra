package com.primaraya.inspectra.fitur.masterdata.ui

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.*
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.masterdata.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    onBackClick: () -> Unit,
    viewModel: MasterDataViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application,
            pembuat = { MasterDataViewModel(it) }
        )
    )
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

    AppResponsiveContent { isTablet, _ ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Data Induk", fontWeight = FontWeight.Black) },
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
                val currentForm = state.dialogForm
                if (!(isTablet && currentForm != null && isSidePaneForm(currentForm))) {
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
                        Icon(Icons.Filled.Add, contentDescription = "Tambah data")
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                        MasterDataContract.TabMasterData.entries.forEach { tab ->
                            Tab(
                                selected = state.tabAktif == tab,
                                onClick = { viewModel.onIntent(MasterDataContract.Intent.PilihTab(tab)) }
                            ) {
                                Text(tab.labelIndonesia(), modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.kataKunci,
                        onValueChange = { viewModel.onIntent(MasterDataContract.Intent.Cari(it)) },
                        label = { Text("Cari ${state.tabAktif.labelIndonesia().lowercase()}") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MasterDataContract.FilterMasterData.entries.forEach { filter ->
                            FilterChip(
                                selected = state.filterAktif == filter,
                                onClick = { viewModel.onIntent(MasterDataContract.Intent.PilihFilter(filter)) },
                                label = { Text(filter.labelIndonesia(), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
                        when (state.tabAktif) {
                            MasterDataContract.TabMasterData.PART -> AsyncList(
                                data = state.parts,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list ->
                                    PartList(
                                        parts = list, 
                                        detailState = state.partDetails,
                                        onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditPart(d)) }, 
                                        onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusPart(d)) },
                                        onToggleDetail = { uniqNo -> viewModel.onIntent(MasterDataContract.Intent.TogglePartDetail(uniqNo)) },
                                        onRemoveDefect = { uniqNo, relId -> viewModel.onIntent(MasterDataContract.Intent.HapusDefectDariPart(uniqNo, relId)) },
                                        onAddDefect = { uniqNo -> viewModel.onIntent(MasterDataContract.Intent.BukaPilihDefect(uniqNo)) },
                                        onRemoveMaterial = { uniqNo, relId -> viewModel.onIntent(MasterDataContract.Intent.HapusMaterialDariPart(uniqNo, relId)) },
                                        onAddMaterial = { uniqNo -> viewModel.onIntent(MasterDataContract.Intent.BukaPilihMaterial(uniqNo)) }
                                    ) 
                                }
                            )
                            MasterDataContract.TabMasterData.MATERIAL -> AsyncList(
                                data = state.materials,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list ->
                                    MaterialList(
                                        materials = list,
                                        relationState = state.materialDetails,
                                        onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditMaterial(d)) },
                                        onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusMaterial(d)) },
                                        onToggleDetail = { id -> viewModel.onIntent(MasterDataContract.Intent.ToggleMaterialDetail(id)) },
                                        onAddDefect = { id -> viewModel.onIntent(MasterDataContract.Intent.BukaPilihDefectUntukMaterial(id)) },
                                        onRemoveDefect = { id, relationId -> viewModel.onIntent(MasterDataContract.Intent.HapusDefectDariMaterial(id, relationId)) }
                                    )
                                }
                            )
                            MasterDataContract.TabMasterData.SUPPLIER -> AsyncList(
                                data = state.suppliers,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list -> SupplierList(list, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditSupplier(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusSupplier(d)) }) }
                            )
                            MasterDataContract.TabMasterData.DEFECT -> AsyncList(
                                data = state.defects,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list -> DefectList(list, onEdit = { d -> viewModel.onIntent(MasterDataContract.Intent.EditDefect(d)) }, onDelete = { d -> viewModel.onIntent(MasterDataContract.Intent.HapusDefect(d)) }) }
                            )
                        }
                    }
                }

                val currentForm = state.dialogForm
                if (isTablet && currentForm != null && isSidePaneForm(currentForm)) {
                    Surface(
                        modifier = Modifier.width(420.dp).fillMaxHeight(),
                        tonalElevation = 6.dp
                    ) {
                        MasterDataFormContent(state, viewModel)
                    }
                }
            }
        }

        val currentForm = state.dialogForm
        if (currentForm != null && (!isTablet || !isSidePaneForm(currentForm))) {
            ModalBottomSheet(onDismissRequest = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }) {
                MasterDataFormContent(state, viewModel)
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

private fun isSidePaneForm(form: MasterDataContract.DialogForm): Boolean {
    return form is MasterDataContract.DialogForm.FormPart ||
           form is MasterDataContract.DialogForm.FormMaterial ||
           form is MasterDataContract.DialogForm.FormSupplier ||
           form is MasterDataContract.DialogForm.FormDefect
}

@Composable
fun MasterDataFormContent(
    state: MasterDataContract.State,
    viewModel: MasterDataViewModel
) {
    when (val form = state.dialogForm) {
        is MasterDataContract.DialogForm.FormPart -> PartFormSheet(
            initialData = state.partFormDraft,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanPart(it)) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormPart(it)) },
            saving = state.menyimpan
        )
        is MasterDataContract.DialogForm.FormMaterial -> MaterialFormSheet(
            initialData = state.materialFormDraft,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanMaterial(it)) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormMaterial(it)) },
            saving = state.menyimpan
        )
        is MasterDataContract.DialogForm.FormSupplier -> SupplierFormSheet(
            initialData = state.supplierFormDraft,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanSupplier(it)) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormSupplier(it)) },
            saving = state.menyimpan
        )
        is MasterDataContract.DialogForm.FormDefect -> DefectFormSheet(
            initialData = state.defectFormDraft,
            isEdit = form.data != null,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanDefect(it)) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormDefect(it)) },
            saving = state.menyimpan
        )
        is MasterDataContract.DialogForm.PilihDefectUntukPart -> {
            PilihDefectDialog(
                availableDefects = (state.defects as? AsyncData.Success<List<MasterDefectDto>>)?.data ?: emptyList(),
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSelect = { idDefect -> viewModel.onIntent(MasterDataContract.Intent.TambahDefectKePart(form.uniqNo, idDefect)) }
            )
        }
        is MasterDataContract.DialogForm.PilihMaterialUntukPart -> {
            PilihMaterialDialog(
                availableMaterials = (state.materials as? AsyncData.Success<List<MasterMaterialDto>>)?.data ?: emptyList(),
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSelect = { matId, label -> viewModel.onIntent(MasterDataContract.Intent.TambahMaterialKePart(form.uniqNo, matId, label)) }
            )
        }
        is MasterDataContract.DialogForm.PilihDefectUntukMaterial -> {
            PilihDefectDialog(
                availableDefects = (state.defects as? AsyncData.Success<List<MasterDefectDto>>)?.data ?: emptyList(),
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onSelect = { idDefect ->
                    viewModel.onIntent(MasterDataContract.Intent.TambahDefectKeMaterial(form.materialId, idDefect))
                }
            )
        }
        is MasterDataContract.DialogForm.KonfirmasiHapus -> {
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                title = { Text(form.judul) },
                text = { Text(form.pesan) },
                confirmButton = {
                    Button(onClick = form.onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Konfirmasi", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }) {
                        Text("Batal")
                    }
                }
            )
        }
        else -> Unit
    }
}

@Composable
fun <T> AsyncList(
    data: AsyncData<List<T>>,
    canLoadMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    content: @Composable (List<T>) -> Unit
) {
    when (data) {
        is AsyncData.Loading -> AppListSkeleton()
        is AsyncData.Success -> {
            Box(modifier = Modifier.fillMaxSize()) {
                content(data.data)
                
                if (loadingMore) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(20.dp)
                    )
                } else if (canLoadMore) {
                    FilledTonalButton(
                        onClick = {
                        onLoadMore()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(20.dp)
                    ) {
                        Text("Muat data berikutnya")
                    }
                }
            }
        }
        is AsyncData.Empty -> AppEmptyState(title = data.title, message = data.message)
        is AsyncData.Error -> AppEmptyState(title = data.title, message = data.message)
        else -> Unit
    }
}

private fun MasterDataContract.TabMasterData.labelIndonesia(): String {
    return when (this) {
        MasterDataContract.TabMasterData.PART -> "Part"
        MasterDataContract.TabMasterData.MATERIAL -> "Material"
        MasterDataContract.TabMasterData.SUPPLIER -> "Supplier"
        MasterDataContract.TabMasterData.DEFECT -> "Defect"
    }
}

private fun MasterDataContract.FilterMasterData.labelIndonesia(): String {
    return when (this) {
        MasterDataContract.FilterMasterData.SEMUA -> "Semua"
        MasterDataContract.FilterMasterData.TANPA_MATERIAL -> "Tanpa Material"
        MasterDataContract.FilterMasterData.TANPA_DEFECT -> "Tanpa Defect"
        MasterDataContract.FilterMasterData.NONAKTIF -> "Nonaktif"
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
    onAddDefect: (String) -> Unit,
    onRemoveMaterial: (String, String) -> Unit,
    onAddMaterial: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(parts, key = { it.uniq_no }) { part ->
            val detail = detailState[part.uniq_no] ?: MasterDataContract.PartRelationState()
            
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth().animateContentSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleDetail(part.uniq_no) }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(part.uniq_no, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(part.nama_part, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { onEdit(part) }) { Icon(Icons.Filled.Edit, contentDescription = "Ubah part") }
                        IconButton(onClick = { onDelete(part) }) { Icon(Icons.Filled.Delete, contentDescription = "Nonaktifkan part", tint = Color.Red) }
                        Icon(
                            imageVector = if (detail.expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (detail.expanded) "Tutup detail part" else "Buka detail part",
                            tint = Color.Gray
                        )
                    }
                    Text("Model: ${part.model ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    Text("Komoditas: ${part.komoditas}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706))
                    
                    if (detail.expanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("Template Defect", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    AsyncRelationList(detail.defects) { rels ->
                                        rels.forEach { rel ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("- ${rel.id_defect}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                                IconButton(onClick = { onRemoveDefect(part.uniq_no, rel.id ?: "") }) {
                                                    Icon(Icons.Filled.LinkOff, contentDescription = "Hapus tautan defect", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        TextButton(onClick = { onAddDefect(part.uniq_no) }) {
                                            Icon(Icons.Filled.AddLink, contentDescription = "Tambah defect", modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Tambah", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Komposisi Material", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    AsyncRelationList(detail.materials) { rels ->
                                        rels.forEach { rel ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(rel.label_material, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    Text(rel.material_id.take(8), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                                IconButton(onClick = { onRemoveMaterial(part.uniq_no, rel.id ?: "") }) {
                                                    Icon(Icons.Filled.LinkOff, contentDescription = "Hapus tautan material", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        TextButton(onClick = { onAddMaterial(part.uniq_no) }) {
                                            Icon(Icons.Filled.AddLink, contentDescription = "Tambah material", modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Tambah", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> AsyncRelationList(data: AsyncData<T>, content: @Composable (T) -> Unit) {
    when (data) {
        is AsyncData.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is AsyncData.Success -> content(data.data)
        is AsyncData.Error -> Text("Gagal: ${data.message}", color = Color.Red, style = MaterialTheme.typography.bodySmall)
        else -> Unit
    }
}

@Composable
fun SupplierList(
    suppliers: List<MasterSupplierDto>,
    onEdit: (MasterSupplierDto) -> Unit,
    onDelete: (MasterSupplierDto) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(suppliers, key = { it.id ?: it.nama_supplier }) { supplier ->
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(supplier.nama_supplier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Kode: ${supplier.kode_supplier ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onEdit(supplier) }) { Icon(Icons.Filled.Edit, contentDescription = "Ubah supplier") }
                        IconButton(onClick = { onDelete(supplier) }) { Icon(Icons.Filled.Delete, contentDescription = "Nonaktifkan supplier", tint = Color.Red) }
                    }
                    Text("Kategori: ${supplier.kategori ?: "-"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun MaterialList(
    materials: List<MasterMaterialDto>,
    relationState: Map<String, MasterDataContract.MaterialRelationState>,
    onEdit: (MasterMaterialDto) -> Unit,
    onDelete: (MasterMaterialDto) -> Unit,
    onToggleDetail: (String) -> Unit,
    onAddDefect: (String) -> Unit,
    onRemoveDefect: (String, String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(materials, key = { it.id ?: it.nama_material }) { material ->
            val materialId = material.id ?: return@items
            val detail = relationState[materialId] ?: MasterDataContract.MaterialRelationState()
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth().animateContentSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleDetail(materialId) }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(material.nama_material, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Supplier: ${material.supplier ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onEdit(material) }) { Icon(Icons.Filled.Edit, contentDescription = "Ubah material") }
                        IconButton(onClick = { onDelete(material) }) { Icon(Icons.Filled.Delete, contentDescription = "Nonaktifkan material", tint = Color.Red) }
                        Icon(
                            imageVector = if (detail.expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (detail.expanded) "Tutup defect bawaan" else "Buka defect bawaan",
                            tint = Color.Gray
                        )
                    }
                    Text("Spesifikasi: ${material.spec ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    if (detail.expanded) {
                        HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                        Text("Defect Bawaan Material", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        AsyncRelationList(detail.defects) { relations ->
                            relations.forEach { relation ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(relation.id_defect, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { onRemoveDefect(materialId, relation.id.orEmpty()) }) {
                                        Icon(
                                            Icons.Filled.LinkOff,
                                            contentDescription = "Hapus tautan defect material",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            TextButton(onClick = { onAddDefect(materialId) }) {
                                Icon(Icons.Filled.AddLink, contentDescription = "Tambah defect bawaan", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tambah defect")
                            }
                        }
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
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(defects, key = { it.id_defect }) { defect ->
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(defect.nama_defect, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("ID: ${defect.id_defect}", style = MaterialTheme.typography.bodySmall)
                    }
                    SuggestionChip(onClick = {}, label = { Text(defect.kategori) })
                    IconButton(onClick = { onEdit(defect) }) { Icon(Icons.Filled.Edit, contentDescription = "Ubah defect") }
                    IconButton(onClick = { onDelete(defect) }) { Icon(Icons.Filled.Delete, contentDescription = "Nonaktifkan defect", tint = Color.Red) }
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
fun PilihMaterialDialog(
    availableMaterials: List<MasterMaterialDto>,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var selectedMatId by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label, contoh: Inner Layer") }, modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(availableMaterials) { mat ->
                        ListItem(
                            headlineContent = { Text(mat.nama_material) },
                            supportingContent = { Text(mat.supplier ?: "-") },
                            trailingContent = { if (selectedMatId == mat.id) Icon(Icons.Filled.Check, "Material dipilih", tint = Color.Green) },
                            modifier = Modifier.clickable { selectedMatId = mat.id }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selectedMatId?.let { onSelect(it, label) } }, enabled = selectedMatId != null && label.isNotBlank()) {
                Text("Pilih")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

// Forms

@Composable
fun PartFormSheet(
    initialData: PartFormState,
    onDismiss: () -> Unit,
    onSave: (PartFormState) -> Unit,
    onUpdate: (PartFormState) -> Unit,
    saving: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Form Part", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = initialData.uniqNo,
            onValueChange = { onUpdate(initialData.copy(uniqNo = it.uppercase())) },
            label = { Text("UNIQ (ID)") },
            isError = initialData.uniqNoError != null,
            supportingText = initialData.uniqNoError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = initialData.partNo,
            onValueChange = { onUpdate(initialData.copy(partNo = it.uppercase())) },
            label = { Text("Nomor Part") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = initialData.namaPart,
            onValueChange = { onUpdate(initialData.copy(namaPart = it.uppercase())) },
            label = { Text("Nama Part") },
            isError = initialData.namaPartError != null,
            supportingText = initialData.namaPartError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = initialData.model,
                onValueChange = { onUpdate(initialData.copy(model = it.uppercase())) },
                label = { Text("Model") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = initialData.customer,
                onValueChange = { onUpdate(initialData.copy(customer = it.uppercase())) },
                label = { Text("Customer") },
                modifier = Modifier.weight(1f)
            )
        }

        AppDropdownField(
            label = "Komoditas",
            value = initialData.komoditas,
            options = listOf("PRESS", "SEWING", "CUTTING"),
            onSelected = { onUpdate(initialData.copy(komoditas = it)) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = initialData.totalItemPerKanban,
                onValueChange = { onUpdate(initialData.copy(totalItemPerKanban = it)) },
                label = { Text("Qty/Kanban") },
                isError = initialData.totalKanbanError != null,
                supportingText = initialData.totalKanbanError?.let { { Text(it) } },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = initialData.sampleItemPerKanban,
                onValueChange = { onUpdate(initialData.copy(sampleItemPerKanban = it)) },
                label = { Text("Sample/Kanban") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = initialData.sampleCycleNote,
            onValueChange = { onUpdate(initialData.copy(sampleCycleNote = it)) },
            label = { Text("Catatan Siklus Sample") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onSave(initialData.copy(submitted = true)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving && (initialData.uniqNo.isNotBlank() && initialData.namaPart.isNotBlank())
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
            else Text("Simpan Part")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Batal")
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun MaterialFormSheet(
    initialData: MaterialFormState,
    onDismiss: () -> Unit,
    onSave: (MaterialFormState) -> Unit,
    onUpdate: (MaterialFormState) -> Unit,
    saving: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Form Material", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = initialData.namaMaterial,
            onValueChange = { onUpdate(initialData.copy(namaMaterial = it.uppercase())) },
            label = { Text("Nama Material") },
            isError = initialData.namaMaterialError != null,
            supportingText = initialData.namaMaterialError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = initialData.supplier,
            onValueChange = { onUpdate(initialData.copy(supplier = it.uppercase())) },
            label = { Text("Supplier") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = initialData.spec,
            onValueChange = { onUpdate(initialData.copy(spec = it.uppercase())) },
            label = { Text("Spesifikasi / Deskripsi") },
            modifier = Modifier.fillMaxWidth()
        )

        AppDropdownField(
            label = "Satuan",
            value = initialData.satuan,
            options = listOf("ROLL", "PCS", "MTR", "KG", "GRAM", "SET"),
            onSelected = { onUpdate(initialData.copy(satuan = it)) }
        )

        Button(
            onClick = { onSave(initialData.copy(submitted = true)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving && initialData.valid
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
            else Text("Simpan Material")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Batal")
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SupplierFormSheet(
    initialData: SupplierFormState,
    onDismiss: () -> Unit,
    onSave: (SupplierFormState) -> Unit,
    onUpdate: (SupplierFormState) -> Unit,
    saving: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Form Supplier", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = initialData.namaSupplier,
            onValueChange = { onUpdate(initialData.copy(namaSupplier = it.uppercase())) },
            label = { Text("Nama Supplier") },
            isError = initialData.namaSupplierError != null,
            supportingText = initialData.namaSupplierError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = initialData.kodeSupplier,
            onValueChange = { onUpdate(initialData.copy(kodeSupplier = it.uppercase())) },
            label = { Text("Kode Supplier") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = initialData.kategori,
            onValueChange = { onUpdate(initialData.copy(kategori = it.uppercase())) },
            label = { Text("Kategori") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onSave(initialData.copy(submitted = true)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving && initialData.valid
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
            else Text("Simpan Supplier")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Batal")
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun DefectFormSheet(
    initialData: DefectFormState,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (DefectFormState) -> Unit,
    onUpdate: (DefectFormState) -> Unit,
    saving: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Form Defect", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = initialData.idDefect,
            onValueChange = { onUpdate(initialData.copy(idDefect = it.uppercase())) },
            label = { Text("ID Defect") },
            enabled = !isEdit,
            isError = initialData.idDefectError != null,
            supportingText = initialData.idDefectError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = initialData.namaDefect,
            onValueChange = { onUpdate(initialData.copy(namaDefect = it.uppercase())) },
            label = { Text("Nama Defect") },
            isError = initialData.namaDefectError != null,
            supportingText = initialData.namaDefectError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        AppDropdownField(
            label = "Kategori",
            value = initialData.kategori,
            options = listOf("PROSES", "MATERIAL"),
            onSelected = { onUpdate(initialData.copy(kategori = it)) }
        )

        Button(
            onClick = { onSave(initialData.copy(submitted = true)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving && initialData.valid
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
            else Text("Simpan Defect")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Batal")
        }
        
        Spacer(Modifier.height(32.dp))
    }
}
