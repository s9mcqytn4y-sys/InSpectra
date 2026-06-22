package com.primaraya.inspectra.core.data

/**
 * Kontrak driver database remote.
 *
 * Driver tidak tahu business domain. Ia hanya tahu cara GET/INSERT/UPSERT/PATCH
 * ke table atau view remote.
 */
interface DatabaseDriver {

    suspend fun <T> getList(
        table: RemoteTable,
        query: String,
        decode: (String) -> T
    ): T

    suspend fun <TBody, TResponse> insertReturning(
        table: RemoteTable,
        body: TBody,
        encode: (TBody) -> String,
        decode: (String) -> TResponse
    ): TResponse

    suspend fun <TBody> upsert(
        table: RemoteTable,
        body: TBody,
        encode: (TBody) -> String,
        onConflict: String? = null
    )

    suspend fun softDelete(
        table: RemoteTable,
        idColumn: String,
        id: String
    )

    suspend fun <TBody, TResponse> rpc(
        functionName: String,
        body: TBody,
        encode: (TBody) -> String,
        decode: (String) -> TResponse
    ): TResponse
}

/**
 * Nama table/view remote dalam satu tempat agar tidak tersebar sebagai string.
 */
enum class RemoteTable(val value: String) {
    Supplier("m_supplier"),
    Part("m_part"),
    Material("m_material"),
    MaterialSpec("m_material_spec"),
    PartMaterial("m_part_material"),
    Defect("m_defect"),
    PartDefect("m_part_defect"),
    MaterialDefect("m_material_defect"),
    PartCuttingSizeReference("m_part_cutting_size_reference"),
    SlotWaktu("m_slot_waktu"),
    ViewChecksheetPartDefect("v_checksheet_part_defect"),
    ViewChecksheetPartPicker("v_checksheet_part_picker"),
    ViewDataIndukPart("v_data_induk_part"),
    ViewCuttingMaterialOption("v_cutting_material_option"),
    ViewCuttingPartSizeOption("v_cutting_part_size_option"),
    ViewCuttingDailySummary("v_cutting_daily_summary"),
    ViewAppBootstrap("v_app_bootstrap"),
    DataRevision("m_data_revision"),
    SesiChecksheet("e_sesi_checksheet"),
    ItemChecksheet("e_item_checksheet"),
    DefectChecksheet("e_defect_checksheet"),
    DefectSlotChecksheet("e_defect_slot_checksheet"),
    CuttingBatch("e_cutting_batch"),
    CuttingDefectDetail("e_cutting_defect_detail")
}
