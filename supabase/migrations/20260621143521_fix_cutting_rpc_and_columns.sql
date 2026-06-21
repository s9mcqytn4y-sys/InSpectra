-- =========================================================
-- INSPECTRA NEXT SCHEMA PATCH
-- Focus:
-- 1. Fix missing columns for DTO (nama_material_snapshot, nama_operator) in e_cutting_batch
-- 2. Transactional RPC for Cutting submit
-- =========================================================

-- 1. Fix DTO columns in e_cutting_batch
alter table if exists public.e_cutting_batch
    add column if not exists nama_material_snapshot varchar,
    add column if not exists nama_operator varchar;

-- 2. TRANSACTIONAL RPC FOR CUTTING
-- This RPC handles the atomic insertion of Sesi, Batch, and Defect details for Cutting.
create or replace function public.rpc_submit_cutting_batch(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    sesi_id uuid;
    batch_id uuid;
    defect_item jsonb;
    total_layer int;
begin
    total_layer := coalesce(nullif(payload ->> 'total_diperiksa', '')::int, 0);

    -- Insert Sesi
    insert into public.e_sesi_checksheet (
        tipe_proses,
        tanggal_pemeriksaan,
        nama_shift,
        nama_operator,
        nama_line,
        total_diperiksa,
        total_ok,
        total_ng,
        rasio_ng_global,
        status
    )
    values (
        'CUTTING',
        coalesce(nullif(payload ->> 'tanggal_pemeriksaan', '')::date, current_date),
        coalesce(nullif(payload ->> 'nama_shift', ''), 'SHIFT_1'),
        nullif(payload ->> 'nama_operator', ''),
        nullif(payload ->> 'nama_line', ''),
        total_layer,
        coalesce(nullif(payload ->> 'total_ok', '')::int, 0),
        coalesce(nullif(payload ->> 'total_ng', '')::int, 0),
        coalesce(nullif(payload ->> 'rasio_ng_global', '')::numeric, 0),
        'TERKIRIM'
    )
    returning id into sesi_id;

    -- Insert Batch
    insert into public.e_cutting_batch (
        id_sesi,
        material_id,
        nama_material_snapshot,
        spec_material_snapshot,
        no_lot_roll,
        no_roll,
        size_cutting_cm,
        ukuran_cutting_cm,
        qty_layer_ok,
        qty_layer_ng,
        waste_panjang_cm,
        nama_operator,
        catatan
    )
    values (
        sesi_id,
        (payload ->> 'material_id')::uuid,
        payload ->> 'nama_material_snapshot',
        nullif(payload ->> 'spec_material_snapshot', ''),
        nullif(payload ->> 'no_lot_roll', ''),
        nullif(payload ->> 'no_roll', ''),
        coalesce(nullif(payload ->> 'size_cutting_cm', '')::numeric, 0),
        coalesce(nullif(payload ->> 'ukuran_cutting_cm', '')::numeric, 0),
        coalesce(nullif(payload ->> 'qty_layer_ok', '')::int, 0),
        coalesce(nullif(payload ->> 'qty_layer_ng', '')::int, 0),
        coalesce(nullif(payload ->> 'waste_panjang_cm', '')::numeric, 0),
        nullif(payload ->> 'nama_operator', ''),
        nullif(payload ->> 'catatan', '')
    )
    returning id into batch_id;

    -- Insert Defects
    for defect_item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftar_defect', '[]'::jsonb))
    loop
        if coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0) > 0 then
            insert into public.e_cutting_defect_detail (
                id_cutting_batch,
                id_defect,
                nama_defect_snapshot,
                slot_waktu_id,
                jumlah_layer_terdampak,
                panjang_defect_cm
            )
            values (
                batch_id,
                defect_item ->> 'id_defect',
                coalesce(defect_item ->> 'nama_defect_snapshot', '-'),
                nullif(defect_item ->> 'slot_waktu_id', '')::uuid,
                coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0),
                nullif(defect_item ->> 'panjang_defect_cm', '')::numeric
            );
        end if;
    end loop;

    return sesi_id;
end;
$$;

grant execute on function public.rpc_submit_cutting_batch(jsonb) to anon, authenticated;

select pg_notify('pgrst', 'reload schema');
