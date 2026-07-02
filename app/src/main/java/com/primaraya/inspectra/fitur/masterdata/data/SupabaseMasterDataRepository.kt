package com.primaraya.inspectra.fitur.masterdata.data

import com.primaraya.inspectra.core.data.DatabaseDriver
import com.primaraya.inspectra.core.data.PageRequest
import com.primaraya.inspectra.core.data.RemoteTable
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.network.InspectraHttpClient
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.common.ReferenceCache
import com.primaraya.inspectra.core.common.CoroutineDispatchersProvider
import com.primaraya.inspectra.core.common.DefaultDispatchersProvider
import com.primaraya.inspectra.fitur.checksheet.domain.PartPickerItem
import com.primaraya.inspectra.fitur.masterdata.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

class SupabaseMasterDataRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver(),
    private val storageDriver: com.primaraya.inspectra.core.data.SupabaseStorageDriver = com.primaraya.inspectra.core.data.SupabaseStorageDriver(),
    private val dispatchers: CoroutineDispatchersProvider = DefaultDispatchersProvider
) : MasterDataRepository {

    private val json = InspectraHttpClient.json
    
    // Memory Cache with TTL
    private val checksheetCache = ReferenceCache<Map<String, List<ChecksheetPartDefectViewDto>>>()
    private val pickerCache = ReferenceCache<Map<String, List<PartPickerItem>>>()
    private val versionCache = ReferenceCache<Map<String, Long>>()
    
    private val partsCache = ReferenceCache<Map<String, List<MasterPartDto>>>()
    private val suppliersCache = ReferenceCache<Map<String, List<MasterSupplierDto>>>()
    private val materialsCache = ReferenceCache<Map<String, List<MasterMaterialDto>>>()
    private val defectsCache = ReferenceCache<Map<String, List<MasterDefectDto>>>()
    
    private var lastChecksheetVersion = -1L
    private var lastPickerVersion = -1L

    override suspend fun healthCheck(): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Part,
                query = "select=id&limit=1",
                decode = { /* No-op: just verify connection and valid response */ }
            )
        }
    }

    private suspend fun isCacheStale(kode: String, lastVersion: Long): Boolean {
        val cachedVersion = versionCache.getIfValid()?.get(kode)
        if (cachedVersion != null) return cachedVersion > lastVersion

        return try {
            val res = driver.getList(
                table = RemoteTable.DataRevision,
                query = "select=versi&kode=eq.$kode",
                decode = { json.decodeFromString<List<JsonObject>>(it) }
            )
            val serverVersion = res.firstOrNull()?.get("versi")?.jsonPrimitive?.longOrNull ?: 0L
            val currentVersions = versionCache.getIfValid() ?: emptyMap()
            versionCache.put(currentVersions + (kode to serverVersion))
            serverVersion > lastVersion
        } catch (e: Exception) {
            true
        }
    }

    override suspend fun getChecksheetData(
        komoditas: String
    ): NetworkResult<List<ChecksheetPartDefectViewDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            val cached = checksheetCache.getIfValid(lastChecksheetVersion)
            if (cached != null) {
                cached[komoditas]?.let { return@runNetworkCatching it }
            }

            val data = driver.getList(
                table = RemoteTable.ViewChecksheetPartDefect,
                query = "select=*&komoditas=eq.$komoditas&order=uniq_no.asc&limit=100",
                decode = { json.decodeFromString(ListSerializer(ChecksheetPartDefectViewDto.serializer()), it) }
            )
            
            val latestVer = driver.getList(
                table = RemoteTable.DataRevision,
                query = "select=versi&kode=eq.CHECKSHEET_REFERENCE",
                decode = { json.decodeFromString<List<JsonObject>>(it).firstOrNull()?.get("versi")?.jsonPrimitive?.longOrNull ?: 0L }
            )
            
            lastChecksheetVersion = latestVer
            val newCache = (checksheetCache.getIfValid() ?: emptyMap()) + (komoditas to data)
            checksheetCache.put(newCache, latestVer)
            data
        }
    }

    override suspend fun getPartPickerItems(tipeProses: String): NetworkResult<List<PartPickerItem>> = withContext(dispatchers.io) {
        runNetworkCatching {
            val cached = pickerCache.getIfValid(lastPickerVersion)
            if (cached != null) {
                cached[tipeProses]?.let { return@runNetworkCatching it }
            }

            val data = driver.getList(
                table = RemoteTable.ViewChecksheetPartPicker,
                query = "komoditas=eq.$tipeProses&order=uniq_no.asc",
                decode = { json.decodeFromString(ListSerializer(PartPickerItem.serializer()), it) }
            )

            val latestVer = driver.getList(
                table = RemoteTable.DataRevision,
                query = "select=versi&kode=eq.CHECKSHEET_REFERENCE",
                decode = { json.decodeFromString<List<JsonObject>>(it).firstOrNull()?.get("versi")?.jsonPrimitive?.longOrNull ?: 0L }
            )

            lastPickerVersion = latestVer
            val newCache = (pickerCache.getIfValid() ?: emptyMap()) + (tipeProses to data)
            pickerCache.put(newCache, latestVer)
            data
        }
    }


    override suspend fun getPartsPage(
        page: PageRequest,
        filter: FilterDataInduk
    ): NetworkResult<List<MasterPartDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            val queryKey = page.toPostgrestQuery() + filter.toPostgrestFilter()
            val cached = partsCache.getIfValid()
            if (cached != null) {
                cached[queryKey]?.let { return@runNetworkCatching it }
            }

            val data = driver.getList(
                table = RemoteTable.ViewDataIndukPart,
                query = queryKey,
                decode = { json.decodeFromString(ListSerializer(MasterPartDto.serializer()), it) }
            )
            
            val newCache = (partsCache.getIfValid() ?: emptyMap()) + (queryKey to data)
            partsCache.put(newCache)
            data
        }
    }

    private fun FilterDataInduk.toPostgrestFilter(): String = when (this) {
        FilterDataInduk.SEMUA -> "&aktif=eq.true"
        FilterDataInduk.SIAP_INPUT -> "&aktif=eq.true&status_input=eq.SIAP_INPUT"
        FilterDataInduk.PERLU_VERIFIKASI -> "&aktif=eq.true&butuh_review=eq.true"
        FilterDataInduk.TANPA_MATERIAL -> "&aktif=eq.true&status_input=eq.TANPA_MATERIAL"
        FilterDataInduk.TANPA_DEFECT -> "&aktif=eq.true&status_input=eq.TANPA_DEFECT"
        FilterDataInduk.NONAKTIF -> "&aktif=eq.false"
    }

    override suspend fun getSuppliersPage(
        page: PageRequest
    ): NetworkResult<List<MasterSupplierDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            val queryKey = page.toPostgrestQuery() + "&aktif=eq.true"
            val cached = suppliersCache.getIfValid()
            if (cached != null) {
                cached[queryKey]?.let { return@runNetworkCatching it }
            }

            val data = driver.getList(
                table = RemoteTable.Supplier,
                query = queryKey,
                decode = { json.decodeFromString(ListSerializer(MasterSupplierDto.serializer()), it) }
            )
            
            val newCache = (suppliersCache.getIfValid() ?: emptyMap()) + (queryKey to data)
            suppliersCache.put(newCache)
            data
        }
    }

    override suspend fun getMaterialsPage(
        page: PageRequest
    ): NetworkResult<List<MasterMaterialDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            val queryKey = page.toPostgrestQuery() + "&aktif=eq.true"
            val cached = materialsCache.getIfValid()
            if (cached != null) {
                cached[queryKey]?.let { return@runNetworkCatching it }
            }

            val data = driver.getList(
                table = RemoteTable.Material,
                query = queryKey,
                decode = { json.decodeFromString(ListSerializer(MasterMaterialDto.serializer()), it) }
            )
            
            val newCache = (materialsCache.getIfValid() ?: emptyMap()) + (queryKey to data)
            materialsCache.put(newCache)
            data
        }
    }

    override suspend fun getDefectsPage(
        page: PageRequest
    ): NetworkResult<List<MasterDefectDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            val queryKey = page.toPostgrestQuery() + "&aktif=eq.true"
            val cached = defectsCache.getIfValid()
            if (cached != null) {
                cached[queryKey]?.let { return@runNetworkCatching it }
            }

            val data = driver.getList(
                table = RemoteTable.Defect,
                query = queryKey,
                decode = { json.decodeFromString(ListSerializer(MasterDefectDto.serializer()), it) }
            )
            
            val newCache = (defectsCache.getIfValid() ?: emptyMap()) + (queryKey to data)
            defectsCache.put(newCache)
            data
        }
    }

    override suspend fun getSlotWaktu(tipeProses: String): NetworkResult<List<MasterSlotWaktuDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.SlotWaktu,
                query = "select=*&tipe_proses=eq.$tipeProses&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterSlotWaktuDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertSupplier(supplier: MasterSupplierDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Supplier,
                body = supplier,
                encode = { json.encodeToString(MasterSupplierDto.serializer(), it) },
                onConflict = "nama_supplier"
            )
            suppliersCache.clear()
        }
    }

    override suspend fun deleteSupplierSoft(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Supplier, idColumn = "id", id = id)
            suppliersCache.clear()
        }
    }

    override suspend fun upsertPart(part: MasterPartDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Part,
                body = part,
                encode = { json.encodeToString(MasterPartDto.serializer(), it) },
                onConflict = "uniq_no"
            )
            partsCache.clear()
        }
    }

    override suspend fun deletePartSoft(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Part, idColumn = "id", id = id)
            partsCache.clear()
        }
    }

    override suspend fun uploadPartImage(uniqNo: String, file: java.io.File): NetworkResult<String> = withContext(dispatchers.io) {
        runNetworkCatching {
            storageDriver.uploadFile(
                bucket = "part-images",
                path = "parts/$uniqNo.jpg",
                file = file
            )
        }
    }

    override suspend fun upsertMaterial(material: MasterMaterialDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Material,
                body = material,
                encode = { json.encodeToString(MasterMaterialDto.serializer(), it) }
            )
            materialsCache.clear()
        }
    }

    override suspend fun deleteMaterialSoft(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Material, idColumn = "id", id = id)
            materialsCache.clear()
        }
    }

    override suspend fun upsertDefect(defect: MasterDefectDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Defect,
                body = defect,
                encode = { json.encodeToString(MasterDefectDto.serializer(), it) },
                onConflict = "id_defect"
            )
            defectsCache.clear()
        }
    }

    override suspend fun deleteDefectSoft(idDefect: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Defect, idColumn = "id_defect", id = idDefect)
            defectsCache.clear()
        }
    }

    override suspend fun getPartDefects(uniqNo: String): NetworkResult<List<MasterPartDefectDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.PartDefect,
                query = "select=*&uniq_no=eq.$uniqNo&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterPartDefectDto.serializer()), it) }
            )
        }
    }

    override suspend fun getPartEffectiveDefects(uniqNo: String): NetworkResult<List<MasterPartEffectiveDefectDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.ViewPartDefectEfektif,
                query = "select=*&uniq_no=eq.$uniqNo&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterPartEffectiveDefectDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertPartDefect(data: MasterPartDefectDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.PartDefect,
                body = data,
                encode = { json.encodeToString(MasterPartDefectDto.serializer(), it) }
            )
        }
    }

    override suspend fun deletePartDefect(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.PartDefect, idColumn = "id", id = id)
        }
    }

    override suspend fun getPartMaterials(uniqNo: String): NetworkResult<List<MasterPartMaterialDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.PartMaterial,
                query = "select=*&uniq_no=eq.$uniqNo&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterPartMaterialDto.serializer()), it) }
            )
        }
    }

    override suspend fun getMaterialUsages(materialId: String): NetworkResult<List<MasterPartMaterialDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.PartMaterial,
                query = "select=*&material_id=eq.$materialId&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterPartMaterialDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertPartMaterial(data: MasterPartMaterialDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.PartMaterial,
                body = data,
                encode = { json.encodeToString(MasterPartMaterialDto.serializer(), it) }
            )
        }
    }

    override suspend fun deletePartMaterial(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.PartMaterial, idColumn = "id", id = id)
        }
    }

    override suspend fun getMaterialDefects(
        materialId: String
    ): NetworkResult<List<MasterMaterialDefectDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.MaterialDefect,
                query = "select=*&material_id=eq.$materialId&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterMaterialDefectDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertMaterialDefect(data: MasterMaterialDefectDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.MaterialDefect,
                body = data,
                encode = { json.encodeToString(MasterMaterialDefectDto.serializer(), it) },
                onConflict = "material_id,id_defect"
            )
        }
    }

    override suspend fun deleteMaterialDefect(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.MaterialDefect, idColumn = "id", id = id)
        }
    }

    override suspend fun getEmployeesPage(
        page: PageRequest
    ): NetworkResult<List<com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto>> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Employee,
                query = page.toPostgrestQuery() + "&aktif=eq.true",
                decode = { json.decodeFromString(ListSerializer(com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertEmployee(employee: com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Employee,
                body = employee,
                encode = { json.encodeToString(com.primaraya.inspectra.fitur.attendance.domain.EmployeeDto.serializer(), it) },
                onConflict = "id"
            )
        }
    }

    override suspend fun deleteEmployeeSoft(id: String): NetworkResult<Unit> = withContext(dispatchers.io) {
        runNetworkCatching {
            driver.softDelete(
                table = RemoteTable.Employee,
                idColumn = "id",
                id = id
            )
        }
    }
}
