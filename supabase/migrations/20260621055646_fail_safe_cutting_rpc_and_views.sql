-- =========================================================
-- INSPECTRA NEXT SCHEMA PATCH
-- Focus:
-- 1. Fail-safe Cutting column alias
-- 2. Stable views for Android DTO
-- 3. Transactional RPC for Press/Sewing submit
-- 4. Runtime health checks
-- 5. PostgREST schema reload
-- =========================================================

create extension if not exists pgcrypto;

-- =========================================================
-- 1. CUTTING COLUMN ALIAS FAIL-SAFE
-- Canonical DB baru: size_cutting_cm
-- Legacy Android saat ini: ukuran_cutting_cm
-- =========================================================

alter table if exists public.e_cutting_batch
    add column if not exists size_cutting_cm numeric(12, 3),
    add column if not exists ukuran_cutting_cm numeric(12, 3);

update public.e_cutting_batch
set
    size_cutting_cm = coalesce(size_cutting_cm, ukuran_cutting_cm),
    ukuran_cutting_cm = coalesce(ukuran_cutting_cm, size_cutting_cm)
where size_cutting_cm is null
   or ukuran_cutting_cm is null;

create or replace function public.f_sync_cutting_size_alias()
returns trigger
language plpgsql
as $$
begin
    new.size_cutting_cm = coalesce(new.size_cutting_cm, new.ukuran_cutting_cm);
    new.ukuran_cutting_cm = coalesce(new.ukuran_cutting_cm, new.size_cutting_cm);

    if new.size_cutting_cm is null or new.size_cutting_cm <= 0 then
        raise exception 'Ukuran cutting harus lebih besar dari nol.';
    end if;

    return new;
end;
$$;

drop trigger if exists trg_sync_cutting_size_alias on public.e_cutting_batch;

create trigger trg_sync_cutting_size_alias
before insert or update on public.e_cutting_batch
for each row
execute function public.f_sync_cutting_size_alias();

alter table if exists public.m_cutting_size_reference
    add column if not exists size_cutting_cm numeric(12, 3),
    add column if not exists ukuran_cutting_cm numeric(12, 3);

update public.m_cutting_size_reference
set
    size_cutting_cm = coalesce(size_cutting_cm, ukuran_cutting_cm),
    ukuran_cutting_cm = coalesce(ukuran_cutting_cm, size_cutting_cm)
where size_cutting_cm is null
   or ukuran_cutting_cm is null;

-- =========================================================
-- 2. CUTTING MATERIAL OPTION VIEW
-- Cocok dengan OpsiMaterialCutting Android:
-- material_id, nama_material, spec_ringkas, satuan,
-- daftar_ukuran_cutting[{id, ukuran_cutting_cm, urutan}]
-- =========================================================

drop view if exists public.v_cutting_material_option cascade;

create or replace view public.v_cutting_material_option as
select
    m.id as material_id,
    m.nama_material,
    coalesce(m.spec_ringkas, m.spec, '') as spec_ringkas,
    coalesce(m.satuan::text, 'UNKNOWN') as satuan,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'id', ukuran.id,
                'ukuran_cutting_cm', coalesce(ukuran.ukuran_cutting_cm, ukuran.size_cutting_cm),
                'size_cutting_cm', coalesce(ukuran.size_cutting_cm, ukuran.ukuran_cutting_cm),
                'urutan', ukuran.urutan
            )
            order by ukuran.urutan, coalesce(ukuran.ukuran_cutting_cm, ukuran.size_cutting_cm)
        ) filter (where ukuran.id is not null),
        '[]'::jsonb
    ) as daftar_ukuran_cutting
from public.m_material m
left join public.m_cutting_size_reference ukuran
    on ukuran.material_id = m.id
   and ukuran.aktif = true
where m.aktif = true
group by
    m.id,
    m.nama_material,
    m.spec_ringkas,
    m.spec,
    m.satuan;

-- =========================================================
-- 3. CUTTING DAILY SUMMARY VIEW
-- Cocok dengan RingkasanHarianCutting Android.
-- =========================================================

drop view if exists public.v_cutting_daily_summary cascade;

drop view if exists public.v_cutting_daily_summary cascade;
create or replace view public.v_cutting_daily_summary as
with batch_normalized as (
    select
        batch.*,
        coalesce(batch.size_cutting_cm, batch.ukuran_cutting_cm) as ukuran_cm
    from public.e_cutting_batch batch
)
select
    sesi.id as id_sesi,
    sesi.tanggal_pemeriksaan,
    sesi.nama_shift,
    sesi.nama_line,

    count(batch.id)::int as total_batch,

    coalesce(sum(batch.qty_layer_ok), 0)::int as total_layer_ok,
    coalesce(sum(batch.qty_layer_ng), 0)::int as total_layer_ng,

    coalesce(sum(batch.ukuran_cm * batch.qty_layer_ok), 0) as total_panjang_ok_cm,
    coalesce(sum(batch.ukuran_cm * batch.qty_layer_ng), 0) as total_panjang_ng_cm,
    coalesce(sum(batch.waste_panjang_cm), 0) as total_waste_cm,

    case
        when coalesce(sum(batch.qty_layer_ok + batch.qty_layer_ng), 0) = 0 then 0
        else round(
            sum(batch.qty_layer_ng)::numeric /
            sum(batch.qty_layer_ok + batch.qty_layer_ng)::numeric * 100,
            3
        )
    end as rasio_ng_layer,

    case
        when coalesce(sum((batch.ukuran_cm * (batch.qty_layer_ok + batch.qty_layer_ng)) + batch.waste_panjang_cm), 0) = 0 then 0
        else round(
            sum(batch.waste_panjang_cm)::numeric /
            sum((batch.ukuran_cm * (batch.qty_layer_ok + batch.qty_layer_ng)) + batch.waste_panjang_cm)::numeric * 100,
            3
        )
    end as rasio_waste_panjang

from public.e_sesi_checksheet sesi
left join batch_normalized batch
    on batch.id_sesi = sesi.id
where sesi.tipe_proses = 'CUTTING'
group by
    sesi.id,
    sesi.tanggal_pemeriksaan,
    sesi.nama_shift,
    sesi.nama_line;

-- =========================================================
-- 4. CHECKSHEET PART DEFECT VIEW
-- Gabungan:
-- - defect manual/proses dari m_part_defect
-- - defect bawaan material dari m_material_defect
-- =========================================================

drop view if exists public.v_checksheet_part_defect cascade;

create or replace view public.v_checksheet_part_defect as
with defect_part as (
    select
        pd.uniq_no,
        pd.id_defect,
        pd.urutan,
        pd.sumber
    from public.m_part_defect pd
    where pd.aktif = true

    union all

    select
        pm.uniq_no,
        md.id_defect,
        md.urutan,
        'MATERIAL' as sumber
    from public.m_part_material pm
    join public.m_material_defect md
        on md.material_id = pm.material_id
       and md.aktif = true
    where pm.aktif = true
),
defect_part_unik as (
    select
        uniq_no,
        id_defect,
        min(urutan) as urutan,
        min(sumber) as sumber
    from defect_part
    group by uniq_no, id_defect
)
select
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas::text as komoditas,
    p.lokasi_gambar,
    p.total_item_per_kanban,
    p.sample_item_per_kanban,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'id_defect', d.id_defect,
                'nama_defect', d.nama_defect,
                'kategori', d.kategori::text,
                'urutan', du.urutan,
                'sumber', du.sumber
            )
            order by du.urutan, d.nama_defect
        ) filter (where d.id_defect is not null),
        '[]'::jsonb
    ) as daftar_defect
from public.m_part p
left join defect_part_unik du
    on du.uniq_no = p.uniq_no
left join public.m_defect d
    on d.id_defect = du.id_defect
   and d.aktif = true
where p.aktif = true
  and p.komoditas in ('PRESS', 'SEWING', 'CUTTING')
group by
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    p.lokasi_gambar,
    p.total_item_per_kanban,
    p.sample_item_per_kanban;

-- =========================================================
-- 5. TRANSACTIONAL RPC FOR PRESS/SEWING
-- Next Android patch: SupabaseChecksheetRepository should call this.
-- =========================================================

create or replace function public.rpc_submit_checksheet(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    sesi_id uuid;
    item_id uuid;
    defect_row_id uuid;

    item jsonb;
    defect_item jsonb;
    slot_item jsonb;

    total_diperiksa int;
    total_ok int;
    total_ng int;
    tipe text;
begin
    tipe := payload ->> 'tipeProses';

    if tipe not in ('PRESS', 'SEWING') then
        raise exception 'RPC ini hanya untuk Press dan Sewing.';
    end if;

    total_diperiksa := coalesce(nullif(payload ->> 'totalDiperiksa', '')::int, 0);
    total_ok := coalesce(nullif(payload ->> 'totalOk', '')::int, 0);
    total_ng := coalesce(nullif(payload ->> 'totalNg', '')::int, 0);

    if total_diperiksa <> total_ok + total_ng then
        raise exception 'Total pemeriksaan tidak sesuai.';
    end if;

    insert into public.e_sesi_checksheet (
        kode_sesi,
        tipe_proses,
        tanggal_pemeriksaan,
        nama_shift,
        nama_operator,
        nama_line,
        device_id,
        app_version,
        total_diperiksa,
        total_ok,
        total_ng,
        rasio_ng_global,
        status
    )
    values (
        coalesce(
            payload ->> 'kodeSesi',
            'INS-' || extract(epoch from now())::bigint || '-' || substr(gen_random_uuid()::text, 1, 8)
        ),
        tipe,
        coalesce(nullif(payload ->> 'tanggalPemeriksaan', '')::date, current_date),
        coalesce(nullif(payload ->> 'namaShift', ''), 'SHIFT_1'),
        nullif(payload ->> 'namaOperator', ''),
        nullif(payload ->> 'namaLine', ''),
        nullif(payload ->> 'deviceId', ''),
        nullif(payload ->> 'appVersion', ''),
        total_diperiksa,
        total_ok,
        total_ng,
        coalesce(nullif(payload ->> 'rasioNgGlobal', '')::numeric, 0),
        'TERKIRIM'
    )
    returning id into sesi_id;

    for item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftarPart', '[]'::jsonb))
    loop
        if coalesce(nullif(item ->> 'jumlahDiperiksa', '')::int, 0) > 0
           or coalesce(nullif(item ->> 'jumlahNg', '')::int, 0) > 0
        then
            insert into public.e_item_checksheet (
                id_sesi,
                uniq_no,
                jumlah_diperiksa,
                jumlah_ok,
                jumlah_ng,
                rasio_ng,
                catatan
            )
            values (
                sesi_id,
                item ->> 'uniqNo',
                coalesce(nullif(item ->> 'jumlahDiperiksa', '')::int, 0),
                coalesce(nullif(item ->> 'jumlahOk', '')::int, 0),
                coalesce(nullif(item ->> 'jumlahNg', '')::int, 0),
                coalesce(nullif(item ->> 'rasioNg', '')::numeric, 0),
                nullif(item ->> 'catatan', '')
            )
            returning id into item_id;

            for defect_item in
                select * from jsonb_array_elements(coalesce(item -> 'daftarDefectNg', '[]'::jsonb))
            loop
                if coalesce(nullif(defect_item ->> 'jumlahNg', '')::int, 0) > 0 then
                    insert into public.e_defect_checksheet (
                        id_item,
                        id_defect,
                        nama_defect_snapshot,
                        kategori,
                        jumlah
                    )
                    values (
                        item_id,
                        defect_item ->> 'idDefect',
                        coalesce(defect_item ->> 'namaDefect', '-'),
                        coalesce(defect_item ->> 'kategori', 'PROSES'),
                        coalesce(nullif(defect_item ->> 'jumlahNg', '')::int, 0)
                    )
                    returning id into defect_row_id;

                    for slot_item in
                        select * from jsonb_array_elements(coalesce(defect_item -> 'detailSlot', '[]'::jsonb))
                    loop
                        if coalesce(nullif(slot_item ->> 'jumlah', '')::int, 0) > 0 then
                            insert into public.e_defect_slot_checksheet (
                                id_defect_checksheet,
                                slot_waktu_id,
                                jumlah
                            )
                            values (
                                defect_row_id,
                                nullif(slot_item ->> 'slotId', '')::uuid,
                                coalesce(nullif(slot_item ->> 'jumlah', '')::int, 0)
                            );
                        end if;
                    end loop;
                end if;
            end loop;
        end if;
    end loop;

    return sesi_id;
end;
$$;

grant execute on function public.rpc_submit_checksheet(jsonb) to anon, authenticated;

-- =========================================================
-- 6. MASTER DATA HEALTH VIEW
-- Used by app/admin to show setup readiness.
-- =========================================================

drop view if exists public.v_masterdata_health cascade;

create or replace view public.v_masterdata_health as
select
    (select count(*) from public.m_part where aktif = true) as total_part,
    (select count(*) from public.m_material where aktif = true) as total_material,
    (select count(*) from public.m_supplier where aktif = true) as total_supplier,
    (select count(*) from public.m_defect where aktif = true) as total_defect,
    (
        select count(*)
        from public.m_part p
        where p.aktif = true
          and p.komoditas in ('PRESS', 'SEWING', 'CUTTING')
          and not exists (
              select 1
              from public.m_part_material pm
              where pm.uniq_no = p.uniq_no
                and pm.aktif = true
          )
    ) as part_tanpa_material,
    (
        select count(*)
        from public.m_part p
        where p.aktif = true
          and p.komoditas in ('PRESS', 'SEWING', 'CUTTING')
          and not exists (
              select 1
              from public.v_checksheet_part_defect v
              where v.uniq_no = p.uniq_no
                and jsonb_array_length(v.daftar_defect) > 0
          )
    ) as part_tanpa_defect;

select pg_notify('pgrst', 'reload schema');
