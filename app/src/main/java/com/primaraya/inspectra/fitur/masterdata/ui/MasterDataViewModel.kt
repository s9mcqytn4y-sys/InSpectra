@file:Suppress("UNCHECKED_CAST")
package com.primaraya.inspectra.fitur.masterdata.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.data.PageRequest
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.ui.UserMessageMapper
import com.primaraya.inspectra.core.ui.KonteksOperasi
import com.primaraya.inspectra.core.ui.component.FeedbackType
import com.primaraya.inspectra.fitur.masterdata.data.MasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import com.primaraya.inspectra.fitur.masterdata.domain.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

class MasterDataViewModel(
    application: Application,
    private val repository: MasterDataRepository = SupabaseMasterDataRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MasterDataContract.State())
    val state: StateFlow<MasterDataContract.State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<MasterDataContract.Effect>()
    val effect: SharedFlow<MasterDataContract.Effect> = _effect.asSharedFlow()

    private var loadJob: Job? = null
    
    private val keywordFlow = MutableStateFlow("")

    init {
        onIntent(MasterDataContract.Intent.MuatAwal)
        setupSearchDebounce()

    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        keywordFlow
            .debounce(350)
            .distinctUntilChanged()
            .onEach { muatDataTabAktif(reset = true) }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: MasterDataContract.Intent) {
        when (intent) {
            MasterDataContract.Intent.MuatAwal -> muatDataTabAktif(reset = true)
            MasterDataContract.Intent.MuatLebihBanyak -> muatDataTabAktif(reset = false)
            is MasterDataContract.Intent.PilihTab -> {
                _state.update { it.copy(tabAktif = intent.tab, kataKunci = "", canLoadMore = true, filterAktif = FilterDataInduk.SEMUA) }
                keywordFlow.value = ""
                muatDataTabAktif(reset = true)
            }
            is MasterDataContract.Intent.PilihFilter -> {
                _state.update { it.copy(filterAktif = intent.filter) }
                muatDataTabAktif(reset = true)
            }
            is MasterDataContract.Intent.Cari -> {
                _state.update { it.copy(kataKunci = intent.keyword) }
                keywordFlow.value = intent.keyword
            }

            // Karyawan
            MasterDataContract.Intent.TambahKaryawan -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormKaryawan()) }
            is MasterDataContract.Intent.EditKaryawan -> {
                val data = intent.data
                val form = KaryawanFormState(
                    id = data.id,
                    namaLengkap = data.namaLengkap,
                    tipePekerja = data.tipePekerja,
                    noReg = data.noReg ?: "",
                    lineProcess = com.primaraya.inspectra.core.common.LineProcess.valueOf(data.lineProcess),
                    aktif = data.aktif
                )
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormKaryawan(data), karyawanFormDraft = form) }
            }
            is MasterDataContract.Intent.UbahFormKaryawan -> {
                _state.update { it.copy(karyawanFormDraft = intent.data) }
            }
            is MasterDataContract.Intent.SimpanKaryawan -> simpanKaryawan(intent.data)
            is MasterDataContract.Intent.HapusKaryawan -> confirmHapus("Hapus Pekerja", "Data pekerja akan dinonaktifkan.") { hapusKaryawan(intent.data) }

            // Part
            MasterDataContract.Intent.TambahPart -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormPart()) }
            is MasterDataContract.Intent.EditPart -> {
                val data = intent.data
                val form = PartFormState(
                    id = data.id,
                    uniqNo = data.uniq_no,
                    partNo = data.part_no ?: "",
                    namaPart = data.nama_part,
                    model = data.model ?: "",
                    customer = data.customer ?: "",
                    komoditas = data.komoditas,
                    totalItemPerKanban = data.total_item_per_kanban?.toString() ?: "",
                    sampleItemPerKanban = data.sample_item_per_kanban?.toString() ?: "",
                    sampleCycleNote = data.sample_cycle_note ?: "",
                    lokasiGambarRemote = data.lokasi_gambar
                )
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormPart(data), partFormDraft = form) }
            }
            is MasterDataContract.Intent.UbahFormPart -> {
                _state.update { it.copy(partFormDraft = intent.data) }
            }
            is MasterDataContract.Intent.SimpanPart -> simpanPart(intent.data)
            is MasterDataContract.Intent.HapusPart -> confirmHapus("Hapus Part", "Part akan dinonaktifkan.") { hapusPart(intent.data) }
            is MasterDataContract.Intent.PilihGambarPart -> pilihGambarPart(intent.file)
            is MasterDataContract.Intent.TogglePartDetail -> togglePartDetail(intent.uniqNo)

            // Material
            MasterDataContract.Intent.TambahMaterial -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial()) }
            is MasterDataContract.Intent.EditMaterial -> {
                val data = intent.data
                val form = MaterialFormState(
                    id = data.id,
                    supplierId = data.supplier_id,
                    supplierNama = data.supplier ?: "",
                    namaMaterial = data.nama_material,
                    spec = data.spec ?: "",
                    satuan = data.satuan ?: "PCS",
                    lebarCm = data.lebar_roll_cm?.toString() ?: "",
                    panjangRollCm = data.panjang_roll_cm?.toString() ?: "",
                    tebalMm = data.tebal_mm?.toString() ?: "",
                    beratGsm = data.berat_gsm?.toString() ?: "",
                    gramasiGsm = data.gramasi_gsm?.toString() ?: "",
                    warna = data.warna ?: "",
                    catatanSpesifikasi = data.catatan_spesifikasi ?: ""
                )
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormMaterial(data), materialFormDraft = form) }
            }
            is MasterDataContract.Intent.UbahFormMaterial -> {
                _state.update { it.copy(materialFormDraft = intent.data) }
            }
            is MasterDataContract.Intent.SimpanMaterial -> simpanMaterial(intent.data)
            is MasterDataContract.Intent.HapusMaterial -> confirmHapus("Nonaktifkan Material", "Data tidak dihapus permanen. Material akan dinonaktifkan.") { hapusMaterial(intent.data) }
            is MasterDataContract.Intent.ToggleMaterialDetail -> toggleMaterialDetail(intent.materialId)

            // Supplier
            MasterDataContract.Intent.TambahSupplier -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormSupplier()) }
            is MasterDataContract.Intent.EditSupplier -> {
                val form = SupplierFormState(
                    id = intent.data.id,
                    kodeSupplier = intent.data.kode_supplier ?: "",
                    namaSupplier = intent.data.nama_supplier,
                    kategori = intent.data.kategori ?: ""
                )
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormSupplier(intent.data), supplierFormDraft = form) }
            }
            is MasterDataContract.Intent.UbahFormSupplier -> {
                _state.update { it.copy(supplierFormDraft = intent.data) }
            }
            is MasterDataContract.Intent.SimpanSupplier -> simpanSupplier(intent.data)
            is MasterDataContract.Intent.HapusSupplier -> confirmHapus("Hapus Supplier", "Supplier akan dinonaktifkan.") { hapusSupplier(intent.data) }

            // Defect
            MasterDataContract.Intent.TambahDefect -> _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect()) }
            is MasterDataContract.Intent.EditDefect -> {
                val form = DefectFormState(
                    idDefect = intent.data.id_defect,
                    namaDefect = intent.data.nama_defect,
                    kategori = intent.data.kategori
                )
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.FormDefect(intent.data), defectFormDraft = form) }
            }
            is MasterDataContract.Intent.UbahFormDefect -> {
                _state.update { it.copy(defectFormDraft = intent.data) }
            }
            is MasterDataContract.Intent.SimpanDefect -> simpanDefect(intent.data)
            is MasterDataContract.Intent.HapusDefect -> confirmHapus("Hapus Defect", "Defect akan dinonaktifkan.") { hapusDefect(intent.data) }

            // Elite Modal Details
            is MasterDataContract.Intent.TampilDetailPart -> {
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.DetailPart(intent.data)) }
                muatDetailPart(intent.data.uniq_no)
            }
            is MasterDataContract.Intent.TampilDetailMaterial -> {
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.DetailMaterial(intent.data)) }
                muatDetailMaterial(intent.data.id ?: "")
            }
            is MasterDataContract.Intent.TampilDetailSupplier -> {
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.DetailSupplier(intent.data)) }
            }
            is MasterDataContract.Intent.TampilDetailDefect -> {
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.DetailDefect(intent.data)) }
            }
            is MasterDataContract.Intent.TampilDetailKaryawan -> {
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.DetailKaryawan(intent.data)) }
            }

            // Relations
            is MasterDataContract.Intent.BukaPilihDefect -> bukaPilihDefectUntukPart(intent.uniqNo)
            is MasterDataContract.Intent.TambahDefectKePart -> tambahDefectKePart(intent.uniqNo, intent.idDefect)
            is MasterDataContract.Intent.HapusDefectDariPart -> confirmHapus("Hapus Tautan", "Defect tidak akan muncul di checksheet part ini.") { hapusDefectDariPart(intent.uniqNo, intent.relationId) }
            is MasterDataContract.Intent.BukaPilihMaterial -> muatMaterialUntukPart(intent.uniqNo)
            is MasterDataContract.Intent.TambahMaterialKePart -> tambahMaterialKePart(intent.uniqNo, intent.materialId, intent.label)
            is MasterDataContract.Intent.HapusMaterialDariPart -> confirmHapus("Hapus Tautan", "Material tidak lagi tertaut ke part ini.") { hapusMaterialDariPart(intent.uniqNo, intent.relationId) }
            
            is MasterDataContract.Intent.BukaPilihSupplier -> bukaPilihSupplier(intent.context)
            is MasterDataContract.Intent.PilihSupplier -> pilihSupplier(intent.context, intent.supplier)

            is MasterDataContract.Intent.BukaPilihDefectUntukMaterial -> bukaPilihDefectUntukMaterial(intent.materialId)
            is MasterDataContract.Intent.TambahDefectKeMaterial -> tambahDefectKeMaterial(intent.materialId, intent.idDefect)
            is MasterDataContract.Intent.HapusDefectDariMaterial -> confirmHapus("Hapus Tautan", "Defect tidak lagi menjadi defect bawaan material ini.") { hapusDefectDariMaterial(intent.materialId, relationId = intent.relationId) }

            MasterDataContract.Intent.TutupDialog -> _state.update { it.copy(dialogForm = null) }
            MasterDataContract.Intent.TutupFeedback -> _state.update { it.copy(feedback = null) }
            MasterDataContract.Intent.ClearUserMessage -> _state.update { it.copy(userMessage = null) }
            MasterDataContract.Intent.Retry -> muatDataTabAktif(reset = true)
        }
    }

    private fun muatDataTabAktif(reset: Boolean) {
        if (!reset && (!_state.value.canLoadMore || _state.value.loadingMore)) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val s = _state.value
            val tab = s.tabAktif
            val search = s.kataKunci
            
            if (reset) {
                _state.update { s ->
                    when (tab) {
                        MasterDataContract.TabMasterData.PART -> s.copy(parts = AsyncData.Loading)
                        MasterDataContract.TabMasterData.MATERIAL -> s.copy(materials = AsyncData.Loading)
                        MasterDataContract.TabMasterData.SUPPLIER -> s.copy(suppliers = AsyncData.Loading)
                        MasterDataContract.TabMasterData.DEFECT -> s.copy(defects = AsyncData.Loading)
                        MasterDataContract.TabMasterData.KARYAWAN -> s.copy(karyawan = AsyncData.Loading)
                    }
                }
            }
            else _state.update { it.copy(loadingMore = true) }

            // Priority: Load from local Room DB if offline or for speed, then sync from network
            // (For MVP, we load from network but could fallback to Room)

            val cursorVal = if (reset) null else when (tab) {
                MasterDataContract.TabMasterData.PART -> (s.parts as? AsyncData.Success<List<MasterPartDto>>)?.data?.lastOrNull()?.uniq_no
                MasterDataContract.TabMasterData.MATERIAL -> (s.materials as? AsyncData.Success<List<MasterMaterialDto>>)?.data?.lastOrNull()?.id
                MasterDataContract.TabMasterData.SUPPLIER -> (s.suppliers as? AsyncData.Success<List<MasterSupplierDto>>)?.data?.lastOrNull()?.id
                MasterDataContract.TabMasterData.DEFECT -> (s.defects as? AsyncData.Success<List<MasterDefectDto>>)?.data?.lastOrNull()?.id_defect
                MasterDataContract.TabMasterData.KARYAWAN -> (s.karyawan as? AsyncData.Success<List<com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto>>)?.data?.lastOrNull()?.id
            }

            val page = when (tab) {
                MasterDataContract.TabMasterData.PART -> PageRequest(cursorColumn = "uniq_no", cursorValue = cursorVal, searchColumn = "nama_part", searchKeyword = search.ifBlank { null })
                MasterDataContract.TabMasterData.MATERIAL -> PageRequest(cursorColumn = "id", cursorValue = cursorVal, searchColumn = "nama_material", searchKeyword = search.ifBlank { null })
                MasterDataContract.TabMasterData.SUPPLIER -> PageRequest(cursorColumn = "id", cursorValue = cursorVal, searchColumn = "nama_supplier", searchKeyword = search.ifBlank { null })
                MasterDataContract.TabMasterData.DEFECT -> PageRequest(cursorColumn = "id_defect", cursorValue = cursorVal, searchColumn = "nama_defect", searchKeyword = search.ifBlank { null })
                MasterDataContract.TabMasterData.KARYAWAN -> PageRequest(cursorColumn = "id", cursorValue = cursorVal, searchColumn = "nama_lengkap", searchKeyword = search.ifBlank { null })
            }

            val result = when (tab) {
                MasterDataContract.TabMasterData.PART -> repository.getPartsPage(page, s.filterAktif)
                MasterDataContract.TabMasterData.MATERIAL -> repository.getMaterialsPage(page)
                MasterDataContract.TabMasterData.SUPPLIER -> repository.getSuppliersPage(page)
                MasterDataContract.TabMasterData.DEFECT -> repository.getDefectsPage(page)
                MasterDataContract.TabMasterData.KARYAWAN -> repository.getEmployeesPage(page)
            }

            _state.update { it.copy(loadingMore = false) }

            when (result) {
                is NetworkResult.Success -> {
                    val newData = result.data.toImmutableList()
                    val canMore = newData.size >= page.limit
                    _state.update { it.copy(canLoadMore = canMore) }

                    if (reset) {
                        if (newData.isEmpty()) {
                            val empty = AsyncData.Empty("Belum ada data", "Data aktif belum tersedia.")
                            _state.update { s ->
                                when (tab) {
                                    MasterDataContract.TabMasterData.PART -> s.copy(parts = empty)
                                    MasterDataContract.TabMasterData.MATERIAL -> s.copy(materials = empty)
                                    MasterDataContract.TabMasterData.SUPPLIER -> s.copy(suppliers = empty)
                                    MasterDataContract.TabMasterData.DEFECT -> s.copy(defects = empty)
                                    MasterDataContract.TabMasterData.KARYAWAN -> s.copy(karyawan = empty)
                                }
                            }
                        }
                        else {
                            _state.update { s ->
                                when (tab) {
                                    MasterDataContract.TabMasterData.PART -> s.copy(parts = AsyncData.Success(newData.filterIsInstance<MasterPartDto>().toImmutableList()))
                                    MasterDataContract.TabMasterData.MATERIAL -> s.copy(materials = AsyncData.Success(newData.filterIsInstance<MasterMaterialDto>().toImmutableList()))
                                    MasterDataContract.TabMasterData.SUPPLIER -> s.copy(suppliers = AsyncData.Success(newData.filterIsInstance<MasterSupplierDto>().toImmutableList()))
                                    MasterDataContract.TabMasterData.DEFECT -> s.copy(defects = AsyncData.Success(newData.filterIsInstance<MasterDefectDto>().toImmutableList()))
                                    MasterDataContract.TabMasterData.KARYAWAN -> s.copy(karyawan = AsyncData.Success(newData.filterIsInstance<com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto>().toImmutableList()))
                                }
                            }
                        }
                    } else {
                        appendDataToTab(tab, newData)
                    }
                }
                is NetworkResult.Error -> {
                    val msg = UserMessageMapper.fromThrowableMessage(result.message, KonteksOperasi.MASTER_DATA)
                    if (reset) {
                        val error = AsyncData.Error(msg.title, msg.body)
                        _state.update { s ->
                            when (tab) {
                                MasterDataContract.TabMasterData.PART -> s.copy(parts = error)
                                MasterDataContract.TabMasterData.MATERIAL -> s.copy(materials = error)
                                MasterDataContract.TabMasterData.SUPPLIER -> s.copy(suppliers = error)
                                MasterDataContract.TabMasterData.DEFECT -> s.copy(defects = error)
                                MasterDataContract.TabMasterData.KARYAWAN -> s.copy(karyawan = error)
                            }
                        }
                    }
                    else _state.update { it.copy(userMessage = msg) }
                }
                else -> Unit
            }
        }
    }

    private fun appendDataToTab(tab: MasterDataContract.TabMasterData, newData: List<*>) {
        _state.update { s ->
            when (tab) {
                MasterDataContract.TabMasterData.PART -> {
                    val currentParts = if (s.parts is AsyncData.Success) s.parts.data else emptyList()
                    val added = newData.filterIsInstance<MasterPartDto>()
                    s.copy(parts = AsyncData.Success((currentParts + added).toImmutableList()))
                }
                MasterDataContract.TabMasterData.MATERIAL -> {
                    val currentMaterials = if (s.materials is AsyncData.Success) s.materials.data else emptyList()
                    val added = newData.filterIsInstance<MasterMaterialDto>()
                    s.copy(materials = AsyncData.Success((currentMaterials + added).toImmutableList()))
                }
                MasterDataContract.TabMasterData.SUPPLIER -> {
                    val currentSuppliers = if (s.suppliers is AsyncData.Success) s.suppliers.data else emptyList()
                    val added = newData.filterIsInstance<MasterSupplierDto>()
                    s.copy(suppliers = AsyncData.Success((currentSuppliers + added).toImmutableList()))
                }
                MasterDataContract.TabMasterData.DEFECT -> {
                    val currentDefects = if (s.defects is AsyncData.Success) s.defects.data else emptyList()
                    val added = newData.filterIsInstance<MasterDefectDto>()
                    s.copy(defects = AsyncData.Success((currentDefects + added).toImmutableList()))
                }
                MasterDataContract.TabMasterData.KARYAWAN -> {
                    val current = if (s.karyawan is AsyncData.Success) s.karyawan.data else emptyList()
                    val added = newData.filterIsInstance<com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto>()
                    s.copy(karyawan = AsyncData.Success((current + added).toImmutableList()))
                }
            }
        }
    }

    private fun togglePartDetail(uniqNo: String) {
        val current = _state.value.partDetails[uniqNo] ?: MasterDataContract.PartRelationState()
        val next = !current.expanded
        _state.update { it.copy(partDetails = it.partDetails + (uniqNo to current.copy(expanded = next))) }
        if (next) muatDetailPart(uniqNo)
    }

    private fun muatDetailPart(uniqNo: String) {
        viewModelScope.launch {
            updatePartRelation(uniqNo) { it.copy(defects = AsyncData.Loading, materials = AsyncData.Loading, effectiveDefects = AsyncData.Loading) }
            val dRes = repository.getPartDefects(uniqNo)
            val eRes = repository.getPartEffectiveDefects(uniqNo)
            val mRes = repository.getPartMaterials(uniqNo)
            updatePartRelation(uniqNo) { s ->
                s.copy(
                    defects = if (dRes is NetworkResult.Success) AsyncData.Success(dRes.data.toImmutableList()) else AsyncData.Error("Gagal", (dRes as? NetworkResult.Error)?.message ?: ""),
                    effectiveDefects = if (eRes is NetworkResult.Success) AsyncData.Success(eRes.data.toImmutableList()) else AsyncData.Error("Gagal", (eRes as? NetworkResult.Error)?.message ?: ""),
                    materials = if (mRes is NetworkResult.Success) AsyncData.Success(mRes.data.toImmutableList()) else AsyncData.Error("Gagal", (mRes as? NetworkResult.Error)?.message ?: "")
                )
            }
        }
    }

    private fun updatePartRelation(u: String, transform: (MasterDataContract.PartRelationState) -> MasterDataContract.PartRelationState) {
        _state.update { it.copy(partDetails = it.partDetails + (u to transform(it.partDetails[u] ?: MasterDataContract.PartRelationState()))) }
    }

    private fun toggleMaterialDetail(materialId: String) {
        val current = _state.value.materialDetails[materialId] ?: MasterDataContract.MaterialRelationState()
        val expanded = !current.expanded
        _state.update { it.copy(materialDetails = it.materialDetails + (materialId to current.copy(expanded = expanded))) }
        if (expanded) muatDetailMaterial(materialId)
    }

    private fun muatDetailMaterial(materialId: String) {
        viewModelScope.launch {
            updateMaterialRelation(materialId) { it.copy(defects = AsyncData.Loading, usageParts = AsyncData.Loading) }
            val dRes = repository.getMaterialDefects(materialId)
            val uRes = repository.getMaterialUsages(materialId)
            updateMaterialRelation(materialId) {
                it.copy(
                    defects = if (dRes is NetworkResult.Success) AsyncData.Success(dRes.data.toImmutableList()) else AsyncData.Error("Gagal", (dRes as? NetworkResult.Error)?.message ?: ""),
                    usageParts = if (uRes is NetworkResult.Success) AsyncData.Success(uRes.data.toImmutableList()) else AsyncData.Error("Gagal", (uRes as? NetworkResult.Error)?.message ?: "")
                )
            }
        }
    }

    private fun updateMaterialRelation(
        materialId: String,
        transform: (MasterDataContract.MaterialRelationState) -> MasterDataContract.MaterialRelationState
    ) {
        _state.update { state ->
            state.copy(
                materialDetails = state.materialDetails + (
                    materialId to transform(state.materialDetails[materialId] ?: MasterDataContract.MaterialRelationState())
                )
            )
        }
    }

    private fun bukaPilihDefectUntukPart(uniqNo: String) {
        muatDefectUntukRelasi { MasterDataContract.DialogForm.PilihDefectUntukPart(uniqNo) }
    }

    private fun bukaPilihDefectUntukMaterial(materialId: String) {
        muatDefectUntukRelasi { MasterDataContract.DialogForm.PilihDefectUntukMaterial(materialId) }
    }

    private fun muatDefectUntukRelasi(
        buatDialog: () -> MasterDataContract.DialogForm
    ) {
        viewModelScope.launch {
            val success = _state.value.defects as? AsyncData.Success<List<MasterDefectDto>>
            if (success != null) {
                _state.update { it.copy(dialogForm = buatDialog()) }
                return@launch
            }

            _state.update { it.copy(defects = AsyncData.Loading) }
            when (val result = repository.getDefectsPage(PageRequest(limit = 100, cursorColumn = "nama_defect"))) {
                is NetworkResult.Success -> _state.update {
                    it.copy(defects = AsyncData.Success(result.data.toImmutableList()), dialogForm = buatDialog())
                }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun muatMaterialUntukPart(uniqNo: String) {
        viewModelScope.launch {
            val success = _state.value.materials as? AsyncData.Success<List<MasterMaterialDto>>
            if (success != null) {
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.PilihMaterialUntukPart(uniqNo)) }
                return@launch
            }

            _state.update { it.copy(materials = AsyncData.Loading) }
            when (val result = repository.getMaterialsPage(PageRequest(limit = 100, cursorColumn = "nama_material"))) {
                is NetworkResult.Success -> _state.update {
                    it.copy(materials = AsyncData.Success(result.data.toImmutableList()), dialogForm = MasterDataContract.DialogForm.PilihMaterialUntukPart(uniqNo))
                }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun bukaPilihSupplier(context: String) {
        viewModelScope.launch {
            val success = _state.value.suppliers as? AsyncData.Success<List<MasterSupplierDto>>
            if (success != null) {
                _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.PilihSupplier(context)) }
                return@launch
            }

            _state.update { it.copy(suppliers = AsyncData.Loading) }
            when (val result = repository.getSuppliersPage(PageRequest(limit = 100, cursorColumn = "nama_supplier"))) {
                is NetworkResult.Success -> _state.update {
                    it.copy(suppliers = AsyncData.Success(result.data.toImmutableList()), dialogForm = MasterDataContract.DialogForm.PilihSupplier(context))
                }
                is NetworkResult.Error -> tampilkanError(result.message)
                else -> Unit
            }
        }
    }

    private fun pilihSupplier(context: String, supplier: MasterSupplierDto) {
        if (context == "MATERIAL") {
            _state.update { 
                it.copy(
                    materialFormDraft = it.materialFormDraft.copy(
                        supplierId = supplier.id,
                        supplierNama = supplier.nama_supplier
                    ),
                    dialogForm = MasterDataContract.DialogForm.FormMaterial()
                )
            }
        }
    }

    private fun pilihGambarPart(file: java.io.File) {
        _state.update { 
            it.copy(
                partFormDraft = it.partFormDraft.copy(lokasiGambarLokal = file.absolutePath)
            )
        }
    }

    private fun simpanPart(f: PartFormState) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }

            var remoteUrl = f.lokasiGambarRemote
            
            // Upload gambar jika ada file lokal baru
            if (f.lokasiGambarLokal != null) {
                val file = java.io.File(f.lokasiGambarLokal)
                val uri = android.net.Uri.fromFile(file)
                val compressedFile = com.primaraya.inspectra.core.util.ImageCompressor.compressImage(getApplication(), uri)
                if (compressedFile == null) {
                    tampilkanError("Gagal mengompres gambar")
                    _state.update { it.copy(menyimpan = false) }
                    return@launch
                }
                when (val uploadRes = repository.uploadPartImage(f.uniqNo, compressedFile)) {
                    is NetworkResult.Success -> remoteUrl = uploadRes.data
                    is NetworkResult.Error -> {
                        tampilkanError("Gagal upload gambar: ${uploadRes.message}")
                        _state.update { it.copy(menyimpan = false) }
                        return@launch
                    }
                    else -> Unit
                }
                
                // Cleanup temp file
                if (compressedFile.exists()) {
                    compressedFile.delete()
                }
            }

            val dto = MasterPartDto(
                id = f.id,
                uniq_no = f.uniqNo,
                part_no = f.partNo,
                nama_part = f.namaPart,
                model = f.model,
                customer = f.customer,
                komoditas = f.komoditas,
                total_item_per_kanban = f.totalItemPerKanban.toIntOrNull(),
                sample_item_per_kanban = f.sampleItemPerKanban.toIntOrNull(),
                sample_cycle_note = f.sampleCycleNote,
                lokasi_gambar = remoteUrl
            )

            if (repository.upsertPart(dto) is NetworkResult.Success) {
                _state.update { it.copy(menyimpan = false, dialogForm = null, feedback = MasterDataContract.FeedbackState("Part berhasil disimpan", FeedbackType.Success)) }
                muatDataTabAktif(reset = true)
            } else tampilkanError("Gagal simpan part")
        }
    }

    private fun simpanMaterial(f: MaterialFormState) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            val dto = MasterMaterialDto(
                id = f.id,
                supplier_id = f.supplierId,
                nama_material = f.namaMaterial,
                spec = f.spec,
                satuan = f.satuan,
                lebar_roll_cm = f.lebarCm.toDoubleOrNull(),
                panjang_roll_cm = f.panjangRollCm.toDoubleOrNull(),
                tebal_mm = f.tebalMm.toDoubleOrNull(),
                berat_gsm = f.beratGsm.toDoubleOrNull(),
                gramasi_gsm = f.gramasiGsm.toDoubleOrNull(),
                warna = f.warna,
                catatan_spesifikasi = f.catatanSpesifikasi
            )
            if (repository.upsertMaterial(dto) is NetworkResult.Success) {
                _state.update { it.copy(menyimpan = false, dialogForm = null, feedback = MasterDataContract.FeedbackState("Material disimpan", FeedbackType.Success)) }
                muatDataTabAktif(reset = true)
            } else tampilkanError("Gagal simpan material")
        }
    }

    private fun simpanSupplier(f: SupplierFormState) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            val dto = MasterSupplierDto(id = f.id, kode_supplier = f.kodeSupplier, nama_supplier = f.namaSupplier, kategori = f.kategori)
            if (repository.upsertSupplier(dto) is NetworkResult.Success) {
                _state.update { it.copy(menyimpan = false, dialogForm = null, feedback = MasterDataContract.FeedbackState("Supplier disimpan", FeedbackType.Success)) }
                muatDataTabAktif(reset = true)
            } else tampilkanError("Gagal simpan supplier")
        }
    }

    private fun simpanDefect(f: DefectFormState) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            val dto = MasterDefectDto(id_defect = f.idDefect, nama_defect = f.namaDefect, kategori = f.kategori)
            if (repository.upsertDefect(dto) is NetworkResult.Success) {
                _state.update { it.copy(menyimpan = false, dialogForm = null, feedback = MasterDataContract.FeedbackState("Defect disimpan", FeedbackType.Success)) }
                muatDataTabAktif(reset = true)
            } else tampilkanError("Gagal simpan defect")
        }
    }

    private fun simpanKaryawan(f: KaryawanFormState) {
        viewModelScope.launch {
            _state.update { it.copy(menyimpan = true) }
            val dto = com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto(
                id = f.id,
                namaLengkap = f.namaLengkap,
                tipePekerja = f.tipePekerja,
                noReg = f.noReg.takeIf { f.tipePekerja == com.primaraya.inspectra.core.common.TipePekerja.KARYAWAN },
                lineProcess = f.lineProcess.name,
                aktif = f.aktif
            )
            // Need to decide which repository handles Karyawan CRUD. 
            // Standardizing on MasterDataRepository for consistency in Master Data tab.
            // (Assuming implementation exists in SupabaseMasterDataRepository or similar)
            // For now, redirecting to a helper or implementing it.
            if (repository.upsertEmployee(dto) is NetworkResult.Success) {
                _state.update { it.copy(menyimpan = false, dialogForm = null, feedback = MasterDataContract.FeedbackState("Pekerja berhasil disimpan", com.primaraya.inspectra.core.ui.component.FeedbackType.Success)) }
                muatDataTabAktif(reset = true)
            } else tampilkanError("Gagal simpan pekerja")
        }
    }

    private fun hapusPart(d: MasterPartDto) = viewModelScope.launch {
        if (repository.deletePartSoft(d.id!!) is NetworkResult.Success) {
            _effect.emit(MasterDataContract.Effect.TampilPesan("Part dinonaktifkan."))
            _state.update { it.copy(dialogForm = null) }
            muatDataTabAktif(reset = true)
        }
    }

    private fun hapusKaryawan(d: com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto) = viewModelScope.launch {
        if (repository.deleteEmployeeSoft(d.id!!) is NetworkResult.Success) {
            _effect.emit(MasterDataContract.Effect.TampilPesan("Pekerja dinonaktifkan."))
            _state.update { it.copy(dialogForm = null) }
            muatDataTabAktif(reset = true)
        }
    }
    private fun hapusMaterial(d: MasterMaterialDto) = viewModelScope.launch { if (repository.deleteMaterialSoft(d.id!!) is NetworkResult.Success) { _effect.emit(MasterDataContract.Effect.TampilPesan("Material dinonaktifkan.")); _state.update { it.copy(dialogForm = null) }; muatDataTabAktif(reset = true) } }
    private fun hapusSupplier(d: MasterSupplierDto) = viewModelScope.launch { if (repository.deleteSupplierSoft(d.id!!) is NetworkResult.Success) { _effect.emit(MasterDataContract.Effect.TampilPesan("Supplier dinonaktifkan.")); _state.update { it.copy(dialogForm = null) }; muatDataTabAktif(reset = true) } }
    private fun hapusDefect(d: MasterDefectDto) = viewModelScope.launch { if (repository.deleteDefectSoft(d.id_defect) is NetworkResult.Success) { _effect.emit(MasterDataContract.Effect.TampilPesan("Defect dinonaktifkan.")); _state.update { it.copy(dialogForm = null) }; muatDataTabAktif(reset = true) } }

    private fun tambahDefectKePart(uniqNo: String, idDefect: String) = viewModelScope.launch {
        _state.update { it.copy(menyimpan = true) }
        val res = repository.upsertPartDefect(MasterPartDefectDto(uniq_no = uniqNo, id_defect = idDefect))
        if (res is NetworkResult.Success) {
            _state.update { it.copy(menyimpan = false, dialogForm = null) }
            _effect.emit(MasterDataContract.Effect.TampilPesan("Defect ditautkan ke part."))
            muatDetailPart(uniqNo)
        } else tampilkanError((res as? NetworkResult.Error)?.message)
    }

    private fun hapusDefectDariPart(uniqNo: String, relationId: String) = viewModelScope.launch {
        _state.update { it.copy(menyimpan = true) }
        val res = repository.deletePartDefect(relationId)
        if (res is NetworkResult.Success) {
            _state.update { it.copy(menyimpan = false, dialogForm = null) }
            _effect.emit(MasterDataContract.Effect.TampilPesan("Tautan defect dihapus."))
            muatDetailPart(uniqNo)
        } else tampilkanError((res as? NetworkResult.Error)?.message)
    }

    private fun tambahMaterialKePart(uniqNo: String, materialId: String, label: String) = viewModelScope.launch {
        _state.update { it.copy(menyimpan = true) }
        val res = repository.upsertPartMaterial(MasterPartMaterialDto(uniq_no = uniqNo, material_id = materialId, label_material = label))
        if (res is NetworkResult.Success) {
            _state.update { it.copy(menyimpan = false, dialogForm = null) }
            _effect.emit(MasterDataContract.Effect.TampilPesan("Material ditautkan ke part."))
            muatDetailPart(uniqNo)
        } else tampilkanError((res as? NetworkResult.Error)?.message)
    }

    private fun hapusMaterialDariPart(uniqNo: String, relationId: String) = viewModelScope.launch {
        _state.update { it.copy(menyimpan = true) }
        val res = repository.deletePartMaterial(relationId)
        if (res is NetworkResult.Success) {
            _state.update { it.copy(menyimpan = false, dialogForm = null) }
            _effect.emit(MasterDataContract.Effect.TampilPesan("Tautan material dihapus."))
            muatDetailPart(uniqNo)
        } else tampilkanError((res as? NetworkResult.Error)?.message)
    }

    private fun tambahDefectKeMaterial(materialId: String, idDefect: String) = viewModelScope.launch {
        _state.update { it.copy(menyimpan = true) }
        val res = repository.upsertMaterialDefect(MasterMaterialDefectDto(material_id = materialId, id_defect = idDefect))
        if (res is NetworkResult.Success) {
            _state.update { it.copy(menyimpan = false, dialogForm = null) }
            _effect.emit(MasterDataContract.Effect.TampilPesan("Defect ditautkan ke material."))
            muatDetailMaterial(materialId)
        } else tampilkanError((res as? NetworkResult.Error)?.message)
    }

    private fun hapusDefectDariMaterial(materialId: String, relationId: String) = viewModelScope.launch {
        _state.update { it.copy(menyimpan = true) }
        val res = repository.deleteMaterialDefect(relationId)
        if (res is NetworkResult.Success) {
            _state.update { it.copy(menyimpan = false, dialogForm = null) }
            _effect.emit(MasterDataContract.Effect.TampilPesan("Tautan defect dihapus."))
            muatDetailMaterial(materialId)
        } else tampilkanError((res as? NetworkResult.Error)?.message)
    }

    private fun confirmHapus(j: String, p: String, a: () -> Unit) { _state.update { it.copy(dialogForm = MasterDataContract.DialogForm.KonfirmasiHapus(j, p, a)) } }
    private fun tampilkanError(r: String?) { val m = UserMessageMapper.fromThrowableMessage(r, KonteksOperasi.MASTER_DATA); _state.update { it.copy(menyimpan = false, userMessage = m) } }
}
