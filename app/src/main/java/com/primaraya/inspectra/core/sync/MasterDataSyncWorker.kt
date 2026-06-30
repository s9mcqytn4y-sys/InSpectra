package com.primaraya.inspectra.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.primaraya.inspectra.core.data.local.InSpectraDatabase
import com.primaraya.inspectra.core.data.local.PartEntity
import com.primaraya.inspectra.core.data.local.MaterialEntity
import com.primaraya.inspectra.core.data.local.DefectEntity
import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.masterdata.data.SupabaseMasterDataRepository
import com.primaraya.inspectra.core.data.PageRequest

/**
 * Worker untuk menyinkronkan data Master dari Supabase ke SQLite lokal.
 */
class MasterDataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = SupabaseMasterDataRepository()
        val dao = InSpectraDatabase.getDatabase(applicationContext).masterDao()

        return try {
            // 1. Sync Parts
            when (val partsRes = repository.getPartsPage(PageRequest(limit = 1000, cursorColumn = "id"), com.primaraya.inspectra.fitur.masterdata.domain.FilterDataInduk.SEMUA)) {
                is NetworkResult.Success -> {
                    val entities = partsRes.data.map { dto ->
                        PartEntity(
                            id = dto.id ?: "",
                            part_no = dto.part_no,
                            uniq_no = dto.uniq_no,
                            nama_part = dto.nama_part,
                            model = dto.model,
                            customer = dto.customer,
                            komoditas = dto.komoditas,
                            lokasi_gambar = dto.lokasi_gambar,
                            aktif = dto.aktif,
                            status_kelengkapan = dto.status_kelengkapan,
                            butuh_review = dto.butuh_review
                        )
                    }
                    dao.insertParts(entities)
                }
                else -> {}
            }

            // 2. Sync Materials
            when (val matRes = repository.getMaterialsPage(PageRequest(limit = 1000, cursorColumn = "id"))) {
                is NetworkResult.Success -> {
                    val entities = matRes.data.map { dto ->
                        MaterialEntity(
                            id = dto.id ?: "",
                            supplier_id = dto.supplier_id,
                            supplier = dto.supplier,
                            nama_material = dto.nama_material,
                            spec = dto.spec,
                            satuan = dto.satuan
                        )
                    }
                    dao.insertMaterials(entities)
                }
                else -> {}
            }

            // 3. Sync Defects
            when (val defRes = repository.getDefectsPage(PageRequest(limit = 1000, cursorColumn = "id_defect"))) {
                is NetworkResult.Success -> {
                    val entities = defRes.data.map { dto ->
                        DefectEntity(
                            id_defect = dto.id_defect,
                            nama_defect = dto.nama_defect,
                            kategori = dto.kategori,
                            aktif = dto.aktif
                        )
                    }
                    dao.insertDefects(entities)
                }
                else -> {}
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
