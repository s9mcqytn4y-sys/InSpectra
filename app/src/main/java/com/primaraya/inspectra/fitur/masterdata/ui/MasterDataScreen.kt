package com.primaraya.inspectra.fitur.masterdata.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.*
import com.primaraya.inspectra.core.ui.viewmodel.pabrikViewModelAplikasi
import com.primaraya.inspectra.fitur.masterdata.domain.*
import com.primaraya.inspectra.fitur.masterdata.ui.components.*
import com.primaraya.inspectra.fitur.masterdata.ui.forms.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    onBackClick: () -> Unit,
    viewModel: MasterDataViewModel = viewModel(
        factory = pabrikViewModelAplikasi(
            application = LocalContext.current.applicationContext as Application
        ) { MasterDataViewModel(it) }
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
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    InspectraEliteSnackbar(data)
                }
            },
            topBar = {
                DataIndukTopBar(
                    tabAktif = state.tabAktif,
                    onTabSelected = { viewModel.onIntent(MasterDataContract.Intent.PilihTab(it)) },
                    onBackClick = onBackClick
                )
            },
            floatingActionButton = {
                val currentForm = state.dialogForm
                if (!((isTablet && currentForm != null && isSidePaneForm(currentForm)))) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            when (state.tabAktif) {
                                MasterDataContract.TabMasterData.PART -> viewModel.onIntent(MasterDataContract.Intent.TambahPart)
                                MasterDataContract.TabMasterData.MATERIAL -> viewModel.onIntent(MasterDataContract.Intent.TambahMaterial)
                                MasterDataContract.TabMasterData.SUPPLIER -> viewModel.onIntent(MasterDataContract.Intent.TambahSupplier)
                                MasterDataContract.TabMasterData.DEFECT -> viewModel.onIntent(MasterDataContract.Intent.TambahDefect)
                                MasterDataContract.TabMasterData.KARYAWAN -> viewModel.onIntent(MasterDataContract.Intent.TambahKaryawan)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.Add, "Tambah") },
                        text = { Text("Tambah Data") }
                    )
                }
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DataIndukSearchBar(
                        query = state.kataKunci,
                        onQueryChange = { viewModel.onIntent(MasterDataContract.Intent.Cari(it)) },
                        tabAktif = state.tabAktif,
                        filterAktif = state.filterAktif,
                        onFilterSelected = { viewModel.onIntent(MasterDataContract.Intent.PilihFilter(it)) }
                    )

                    state.feedback?.let { fb ->
                        InspectraFeedback(
                            message = fb.message,
                            type = fb.type,
                            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupFeedback) }
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        when (state.tabAktif) {
                            MasterDataContract.TabMasterData.PART -> AsyncList(
                                data = state.parts,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list ->
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 350.dp),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(list, key = { it.uniq_no }) { part ->
                                            PartMasterCard(
                                                part = part,
                                                onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditPart(part)) },
                                                onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusPart(part)) },
                                                onShowDetail = { viewModel.onIntent(MasterDataContract.Intent.TampilDetailPart(part)) }
                                            )
                                        }
                                    }
                                }
                            )
                            MasterDataContract.TabMasterData.MATERIAL -> AsyncList(
                                data = state.materials,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list ->
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 350.dp),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(list, key = { it.id ?: it.nama_material }) { mat ->
                                            MaterialMasterCard(
                                                material = mat,
                                                onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditMaterial(mat)) },
                                                onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusMaterial(mat)) },
                                                onShowDetail = { viewModel.onIntent(MasterDataContract.Intent.TampilDetailMaterial(mat)) }
                                            )
                                        }
                                    }
                                }
                            )
                            MasterDataContract.TabMasterData.SUPPLIER -> AsyncList(
                                data = state.suppliers,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list ->
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 350.dp),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(list, key = { it.id ?: it.nama_supplier }) { supplier ->
                                            SupplierMasterCard(
                                                supplier = supplier,
                                                onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditSupplier(supplier)) },
                                                onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusSupplier(supplier)) },
                                                onShowDetail = { viewModel.onIntent(MasterDataContract.Intent.TampilDetailSupplier(supplier)) }
                                            )
                                        }
                                    }
                                }
                            )
                            MasterDataContract.TabMasterData.DEFECT -> AsyncList(
                                data = state.defects,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list ->
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 350.dp),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(list, key = { it.id_defect }) { defect ->
                                            DefectMasterCard(
                                                defect = defect,
                                                onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditDefect(defect)) },
                                                onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusDefect(defect)) },
                                                onShowDetail = { viewModel.onIntent(MasterDataContract.Intent.TampilDetailDefect(defect)) }
                                            )
                                        }
                                    }
                                }
                            )
                            MasterDataContract.TabMasterData.KARYAWAN -> AsyncList(
                                data = state.karyawan,
                                canLoadMore = state.canLoadMore,
                                loadingMore = state.loadingMore,
                                onLoadMore = { viewModel.onIntent(MasterDataContract.Intent.MuatLebihBanyak) },
                                content = { list ->
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 350.dp),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(list, key = { it.id ?: it.namaLengkap }) { emp ->
                                            KaryawanMasterCard(
                                                karyawan = emp,
                                                onEdit = { viewModel.onIntent(MasterDataContract.Intent.EditKaryawan(emp)) },
                                                onDelete = { viewModel.onIntent(MasterDataContract.Intent.HapusKaryawan(emp)) },
                                                onShowDetail = { viewModel.onIntent(MasterDataContract.Intent.TampilDetailKaryawan(emp)) }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                ResponsiveFormHost(
                    visible = isTablet && state.dialogForm != null && isSidePaneForm(state.dialogForm!!),
                    isTablet = true,
                    onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }
                ) {
                    MasterDataFormContent(state, viewModel)
                }
            }
        }

        ResponsiveFormHost(
            visible = state.dialogForm != null && (!isTablet || !isSidePaneForm(state.dialogForm!!)),
            isTablet = false,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }
        ) {
            MasterDataFormContent(state, viewModel)
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
           form is MasterDataContract.DialogForm.FormDefect ||
           form is MasterDataContract.DialogForm.FormKaryawan
}

@Composable
private fun MasterDataFormContent(
    state: MasterDataContract.State,
    viewModel: MasterDataViewModel
) {
    when (val form = state.dialogForm) {
        is MasterDataContract.DialogForm.FormKaryawan -> KaryawanFormSheet(
            state = state.karyawanFormDraft,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormKaryawan(it)) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanKaryawan(it)) },
            isSaving = state.menyimpan
        )
        is MasterDataContract.DialogForm.FormPart -> PartFormSheet(
            state = state.partFormDraft,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormPart(it)) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanPart(it)) },
            onImageSelect = { viewModel.onIntent(MasterDataContract.Intent.PilihGambarPart(it)) },
            isSaving = state.menyimpan
        )
        is MasterDataContract.DialogForm.FormMaterial -> MaterialFormSheet(
            state = state.materialFormDraft,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormMaterial(it)) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanMaterial(it)) },
            onOpenSupplierPicker = { viewModel.onIntent(MasterDataContract.Intent.BukaPilihSupplier("MATERIAL")) },
            isSaving = state.menyimpan
        )
        is MasterDataContract.DialogForm.FormSupplier -> SupplierFormSheet(
            state = state.supplierFormDraft,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormSupplier(it)) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanSupplier(it)) },
            isSaving = state.menyimpan
        )
        is MasterDataContract.DialogForm.FormDefect -> DefectFormSheet(
            state = state.defectFormDraft,
            isEdit = form.data != null,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onUpdate = { viewModel.onIntent(MasterDataContract.Intent.UbahFormDefect(it)) },
            onSave = { viewModel.onIntent(MasterDataContract.Intent.SimpanDefect(it)) },
            isSaving = state.menyimpan
        )
        is MasterDataContract.DialogForm.PilihDefectUntukPart -> {
            val defects = (state.defects as? AsyncData.Success)?.data ?: emptyList()
            PilihDefectDialog(
                defects = defects,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onConfirm = { viewModel.onIntent(MasterDataContract.Intent.TambahDefectKePart(form.uniqNo, it)) }
            )
        }
        is MasterDataContract.DialogForm.PilihMaterialUntukPart -> {
            val materials = (state.materials as? AsyncData.Success)?.data ?: emptyList()
            PilihMaterialDialog(
                materials = materials,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onConfirm = { matId, label -> viewModel.onIntent(MasterDataContract.Intent.TambahMaterialKePart(form.uniqNo, matId, label)) }
            )
        }
        is MasterDataContract.DialogForm.PilihDefectUntukMaterial -> {
            val defects = (state.defects as? AsyncData.Success)?.data ?: emptyList()
            PilihDefectDialog(
                defects = defects,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onConfirm = { viewModel.onIntent(MasterDataContract.Intent.TambahDefectKeMaterial(form.materialId, it)) }
            )
        }
        is MasterDataContract.DialogForm.PilihSupplier -> {
            val suppliers = (state.suppliers as? AsyncData.Success)?.data ?: emptyList()
            PilihSupplierDialog(
                suppliers = suppliers,
                onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                onConfirm = { viewModel.onIntent(MasterDataContract.Intent.PilihSupplier(form.context, it)) }
            )
        }
        is MasterDataContract.DialogForm.KonfirmasiHapus -> {
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
                title = { Text(form.judul) },
                text = { Text(form.pesan) },
                confirmButton = {
                    Button(onClick = form.onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }) {
                        Text("Batal")
                    }
                }
            )
        }
        is MasterDataContract.DialogForm.DetailPart -> PartDetailModal(
            part = form.data,
            relationState = state.partDetails[form.data.uniq_no] ?: MasterDataContract.PartRelationState(),
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onAddDefect = { viewModel.onIntent(MasterDataContract.Intent.BukaPilihDefect(form.data.uniq_no)) },
            onRemoveDefect = { viewModel.onIntent(MasterDataContract.Intent.HapusDefectDariPart(form.data.uniq_no, it)) },
            onAddMaterial = { viewModel.onIntent(MasterDataContract.Intent.BukaPilihMaterial(form.data.uniq_no)) },
            onRemoveMaterial = { viewModel.onIntent(MasterDataContract.Intent.HapusMaterialDariPart(form.data.uniq_no, it)) }
        )
        is MasterDataContract.DialogForm.DetailMaterial -> MaterialDetailModal(
            material = form.data,
            relationState = state.materialDetails[form.data.id ?: ""] ?: MasterDataContract.MaterialRelationState(),
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) },
            onAddDefect = { viewModel.onIntent(MasterDataContract.Intent.BukaPilihDefectUntukMaterial(form.data.id ?: "")) },
            onRemoveDefect = { viewModel.onIntent(MasterDataContract.Intent.HapusDefectDariMaterial(form.data.id ?: "", it)) }
        )
        is MasterDataContract.DialogForm.DetailSupplier -> SupplierDetailModal(
            supplier = form.data,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }
        )
        is MasterDataContract.DialogForm.DetailDefect -> DefectDetailModal(
            defect = form.data,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }
        )
        is MasterDataContract.DialogForm.DetailKaryawan -> KaryawanDetailModal(
            karyawan = form.data,
            onDismiss = { viewModel.onIntent(MasterDataContract.Intent.TutupDialog) }
        )
        else -> Unit
    }
}

@Composable
fun PilihSupplierDialog(
    suppliers: List<MasterSupplierDto>,
    onDismiss: () -> Unit,
    onConfirm: (MasterSupplierDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    
    val filtered = suppliers.filter { 
        it.nama_supplier.contains(query, ignoreCase = true) || 
        (it.kode_supplier?.contains(query, ignoreCase = true) ?: false) 
    }

    InspectraAdvancedPickerSheet(
        title = "Pilih Supplier",
        query = query,
        onQueryChange = { query = it },
        items = filtered.map { 
            PickerItemUi(
                id = it.id ?: "", 
                title = it.nama_supplier, 
                subtitle = "Kode: ${it.kode_supplier ?: "-"}",
                badges = listOf(it.kategori ?: "UMUM")
            ) 
        },
        selectedId = selectedId,
        onSelect = { selectedId = it },
        onDismiss = onDismiss,
        onConfirm = { 
            filtered.find { it.id == selectedId }?.let(onConfirm)
        }
    )
}

@Composable
fun PilihDefectDialog(
    defects: List<MasterDefectDto>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    
    val filtered = defects.filter { 
        it.nama_defect.contains(query, ignoreCase = true) || 
        it.id_defect.contains(query, ignoreCase = true) 
    }

    InspectraAdvancedPickerSheet(
        title = "Pilih Defect",
        query = query,
        onQueryChange = { query = it },
        items = filtered.map { 
            PickerItemUi(
                id = it.id_defect, 
                title = it.nama_defect, 
                subtitle = "Kategori: ${it.kategori}",
                badges = listOf(it.id_defect)
            ) 
        },
        selectedId = selectedId,
        onSelect = { selectedId = it },
        onDismiss = onDismiss,
        onConfirm = { selectedId?.let(onConfirm) }
    )
}

@Composable
fun PilihMaterialDialog(
    materials: List<MasterMaterialDto>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var label by remember { mutableStateOf("Material Utama") }
    
    val filtered = materials.filter { 
        it.nama_material.contains(query, ignoreCase = true) || 
        (it.supplier?.contains(query, ignoreCase = true) ?: false) 
    }

    Column {
        InspectraAdvancedPickerSheet(
            title = "Pilih Material",
            query = query,
            onQueryChange = { query = it },
            items = filtered.map { 
                PickerItemUi(
                    id = it.id ?: "", 
                    title = it.nama_material, 
                    subtitle = "Supplier: ${it.supplier ?: "-"}",
                    description = it.spec
                ) 
            },
            selectedId = selectedId,
            onSelect = { selectedId = it },
            onDismiss = onDismiss,
            onConfirm = { selectedId?.let { onConfirm(it, label) } },
            modifier = Modifier.weight(1f, fill = false)
        )
        
        if (selectedId != null) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text("Label Penggunaan:", style = MaterialTheme.typography.labelMedium)
                com.primaraya.inspectra.core.ui.component.AppDropdownField(
                    label = "Pilih Label",
                    value = label,
                    options = listOf("Material Utama", "Lapisan Atas", "Lapisan Bawah", "Laminasi", "Benang", "Hook", "Label/Tag"),
                    onSelected = { label = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
