package com.primaraya.inspectra.fitur.masterdata.data

import com.primaraya.inspectra.core.data.DatabaseDriver
import com.primaraya.inspectra.core.data.PageRequest
import com.primaraya.inspectra.core.data.RemoteTable
import com.primaraya.inspectra.core.data.SupabasePgRestDriver
import com.primaraya.inspectra.core.network.InspectraHttpClient
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.core.network.runNetworkCatching
import com.primaraya.inspectra.fitur.masterdata.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

class SupabaseMasterDataRepository(
    private val driver: DatabaseDriver = SupabasePgRestDriver()
) : MasterDataRepository {

    private val json = InspectraHttpClient.json

    override suspend fun healthCheck(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Part,
                query = "select=id&limit=1",
                decode = { /* No-op: just verify connection and valid response */ }
            )
        }
    }

    override suspend fun getChecksheetData(
        komoditas: String
    ): NetworkResult<List<ChecksheetPartDefectViewDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.ViewChecksheetPartDefect,
                query = "select=*&komoditas=eq.$komoditas&order=uniq_no.asc&limit=100",
                decode = { json.decodeFromString(ListSerializer(ChecksheetPartDefectViewDto.serializer()), it) }
            )
        }
    }

    override suspend fun getPartsPage(
        page: PageRequest
    ): NetworkResult<List<MasterPartDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Part,
                query = page.toPostgrestQuery() + "&aktif=eq.true",
                decode = { json.decodeFromString(ListSerializer(MasterPartDto.serializer()), it) }
            )
        }
    }

    override suspend fun getSuppliersPage(
        page: PageRequest
    ): NetworkResult<List<MasterSupplierDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Supplier,
                query = page.toPostgrestQuery() + "&aktif=eq.true",
                decode = { json.decodeFromString(ListSerializer(MasterSupplierDto.serializer()), it) }
            )
        }
    }

    override suspend fun getMaterialsPage(
        page: PageRequest
    ): NetworkResult<List<MasterMaterialDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Material,
                query = page.toPostgrestQuery() + "&aktif=eq.true",
                decode = { json.decodeFromString(ListSerializer(MasterMaterialDto.serializer()), it) }
            )
        }
    }

    override suspend fun getDefectsPage(
        page: PageRequest
    ): NetworkResult<List<MasterDefectDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.Defect,
                query = page.toPostgrestQuery() + "&aktif=eq.true",
                decode = { json.decodeFromString(ListSerializer(MasterDefectDto.serializer()), it) }
            )
        }
    }

    override suspend fun getSlotWaktu(tipeProses: String): NetworkResult<List<MasterSlotWaktuDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.SlotWaktu,
                query = "select=*&tipe_proses=eq.$tipeProses&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterSlotWaktuDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertSupplier(supplier: MasterSupplierDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Supplier,
                body = supplier,
                encode = { json.encodeToString(MasterSupplierDto.serializer(), it) }
            )
        }
    }

    override suspend fun deleteSupplierSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Supplier, idColumn = "id", id = id)
        }
    }

    override suspend fun upsertPart(part: MasterPartDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Part,
                body = part,
                encode = { json.encodeToString(MasterPartDto.serializer(), it) },
                onConflict = "uniq_no"
            )
        }
    }

    override suspend fun deletePartSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Part, idColumn = "id", id = id)
        }
    }

    override suspend fun upsertMaterial(material: MasterMaterialDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Material,
                body = material,
                encode = { json.encodeToString(MasterMaterialDto.serializer(), it) }
            )
        }
    }

    override suspend fun deleteMaterialSoft(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Material, idColumn = "id", id = id)
        }
    }

    override suspend fun upsertDefect(defect: MasterDefectDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.Defect,
                body = defect,
                encode = { json.encodeToString(MasterDefectDto.serializer(), it) }
            )
        }
    }

    override suspend fun deleteDefectSoft(idDefect: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.Defect, idColumn = "id_defect", id = idDefect)
        }
    }

    override suspend fun getPartDefects(uniqNo: String): NetworkResult<List<MasterPartDefectDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.PartDefect,
                query = "select=*&uniq_no=eq.$uniqNo&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterPartDefectDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertPartDefect(data: MasterPartDefectDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.PartDefect,
                body = data,
                encode = { json.encodeToString(MasterPartDefectDto.serializer(), it) }
            )
        }
    }

    override suspend fun deletePartDefect(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.PartDefect, idColumn = "id", id = id)
        }
    }

    override suspend fun getPartMaterials(uniqNo: String): NetworkResult<List<MasterPartMaterialDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.PartMaterial,
                query = "select=*&uniq_no=eq.$uniqNo&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterPartMaterialDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertPartMaterial(data: MasterPartMaterialDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.PartMaterial,
                body = data,
                encode = { json.encodeToString(MasterPartMaterialDto.serializer(), it) }
            )
        }
    }

    override suspend fun deletePartMaterial(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.PartMaterial, idColumn = "id", id = id)
        }
    }

    override suspend fun getMaterialDefects(
        materialId: String
    ): NetworkResult<List<MasterMaterialDefectDto>> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.getList(
                table = RemoteTable.MaterialDefect,
                query = "select=*&material_id=eq.$materialId&aktif=eq.true&order=urutan.asc",
                decode = { json.decodeFromString(ListSerializer(MasterMaterialDefectDto.serializer()), it) }
            )
        }
    }

    override suspend fun upsertMaterialDefect(data: MasterMaterialDefectDto): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.upsert(
                table = RemoteTable.MaterialDefect,
                body = data,
                encode = { json.encodeToString(MasterMaterialDefectDto.serializer(), it) },
                onConflict = "material_id,id_defect"
            )
        }
    }

    override suspend fun deleteMaterialDefect(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        runNetworkCatching {
            driver.softDelete(table = RemoteTable.MaterialDefect, idColumn = "id", id = id)
        }
    }
}
