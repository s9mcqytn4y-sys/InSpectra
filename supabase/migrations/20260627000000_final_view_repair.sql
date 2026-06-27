-- =========================================================
-- FINAL VIEW REPAIR & RECONCILIATION
-- Force drop and recreate all problematic views to avoid SQLSTATE 42P16
-- =========================================================

begin;

-- 1. DROP ALL VIEWS IN REVERSE DEPENDENCY ORDER
drop view if exists public.v_checksheet_part_picker cascade;
drop view if exists public.v_checksheet_part_defect cascade;
drop view if exists public.v_data_induk_part cascade;
drop view if exists public.v_part_defect_efektif cascade;
drop view if exists public.v_master_part_detail cascade;
drop view if exists public.v_cutting_material_option cascade;
drop view if exists public.v_cutting_part_size_option cascade;

-- 2. RECREATE v_part_defect_efektif
create view public.v_part_defect_efektif as
-- Defect Proses Part
select
    pd.id as relation_id,
    pd.uniq_no,
    pd.id_defect,
    d.nama_defect,
    d.kategori::text as kategori,
    'PROSES_PART' as sumber_defect,
    null::uuid as material_id,
    null as nama_material,
    pd.urutan,
    pd.wajib_check,
    pd.aktif
from public.m_part_defect pd
join public.m_defect d on d.id_defect = pd.id_defect

union all

-- Defect dari Material yang digunakan Part
select
    md.id as relation_id,
    pm.uniq_no,
    md.id_defect,
    d.nama_defect,
    d.kategori::text as kategori,
    'MATERIAL' as sumber_defect,
    m.id as material_id,
    m.nama_material,
    md.urutan,
    md.wajib_check,
    md.aktif
from public.m_part_material pm
join public.m_material m on m.id = pm.material_id
join public.m_material_defect md on md.material_id = m.id
join public.m_defect d on d.id_defect = md.id_defect;

-- 3. RECREATE v_data_induk_part
create view public.v_data_induk_part as
with counts as (
    select
        p.id,
        count(distinct pm.id) as jumlah_material,
        count(distinct pd.id) as jumlah_defect_proses,
        count(distinct md.id) as jumlah_defect_material
    from public.m_part p
    left join public.m_part_material pm on pm.uniq_no = p.uniq_no and pm.aktif = true
    left join public.m_part_defect pd on pd.uniq_no = p.uniq_no and pd.aktif = true
    left join public.m_material_defect md on md.material_id = pm.material_id and md.aktif = true
    group by p.id
)
select
    part.id,
    part.part_no,
    part.uniq_no,
    part.nama_part,
    part.model,
    part.customer,
    part.komoditas::text as komoditas,
    part.lokasi_gambar,
    part.aktif,
    part.status_kelengkapan,
    part.butuh_review,
    part.catatan_review,
    coalesce(c.jumlah_material, 0)::int as jumlah_material,
    coalesce(c.jumlah_defect_proses, 0)::int as jumlah_defect_proses,
    coalesce(c.jumlah_defect_material, 0)::int as jumlah_defect_material,
    (coalesce(c.jumlah_defect_proses, 0) + coalesce(c.jumlah_defect_material, 0))::int as jumlah_defect,
    case
        when not part.aktif then 'NONAKTIF'
        when part.butuh_review then 'PERLU_VERIFIKASI'
        when coalesce(c.jumlah_material, 0) = 0 then 'TANPA_MATERIAL'
        when (coalesce(c.jumlah_defect_proses, 0) + coalesce(c.jumlah_defect_material, 0)) = 0 then 'TANPA_DEFECT'
        else 'SIAP_INPUT'
    end as status_input
from public.m_part part
left join counts c on c.id = part.id;

-- 4. RECREATE v_checksheet_part_defect (Recursive)
create view public.v_checksheet_part_defect as
with recursive material_hierarchy as (
    -- Base: Direct materials for each part
    select
        pm.uniq_no,
        pm.material_id,
        pm.label_material as source_name
    from public.m_part_material pm
    where pm.aktif = true

    union all

    -- Recursive: Components of materials
    select
        mh.uniq_no,
        mk.child_material_id as material_id,
        mh.source_name
    from material_hierarchy mh
    join public.m_material_komposisi mk on mk.parent_material_id = mh.material_id
    where mk.aktif = true
),
all_defects as (
    -- Defect Proses Part
    select
        pd.uniq_no,
        pd.id_defect,
        pd.urutan,
        'PROSES' as sumber
    from public.m_part_defect pd
    where pd.aktif = true

    union

    -- Defect dari Hierarchy Material
    select
        mh.uniq_no,
        md.id_defect,
        md.urutan + 100 as urutan,
        'MATERIAL' as sumber
    from material_hierarchy mh
    join public.m_material_defect md on md.material_id = mh.material_id
    where md.aktif = true
),
defect_unik as (
    select
        uniq_no,
        id_defect,
        min(urutan) as urutan,
        min(sumber) as sumber
    from all_defects
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
left join defect_unik du on du.uniq_no = p.uniq_no
left join public.m_defect d on d.id_defect = du.id_defect and d.aktif = true
where p.aktif = true
  and p.komoditas::text in ('PRESS', 'SEWING', 'CUTTING')
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

-- 5. RECREATE v_checksheet_part_picker
create view public.v_checksheet_part_picker as
select
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas::text as komoditas,
    p.lokasi_gambar as image_url,
    false as menggunakan_default,
    count(distinct pm.id) as jumlah_material,
    coalesce(jsonb_array_length(v.daftar_defect), 0)::int as jumlah_defect,
    case
        when not p.aktif then 'NONAKTIF'
        when count(distinct pm.id) = 0 then 'TANPA_MATERIAL'
        when coalesce(jsonb_array_length(v.daftar_defect), 0) = 0 then 'TANPA_DEFECT'
        else 'SIAP_INPUT'
    end as status_input
from public.m_part p
left join public.m_part_material pm
    on pm.uniq_no = p.uniq_no
   and pm.aktif = true
left join public.v_checksheet_part_defect v
    on v.uniq_no = p.uniq_no
where p.aktif = true
  and p.komoditas::text in ('PRESS', 'SEWING', 'CUTTING')
group by
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    p.lokasi_gambar,
    v.daftar_defect,
    p.aktif;

-- 6. RECREATE v_master_part_detail
create view public.v_master_part_detail as
select
    p.id,
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas::text as komoditas,
    p.total_item_per_kanban,
    p.sample_item_per_kanban,
    p.sample_cycle_note,
    p.lokasi_gambar,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'id', pm.id,
                'label_material', pm.label_material,
                'nama_material', m.nama_material,
                'supplier', coalesce(s.nama_supplier, m.supplier_manual),
                'spec_ringkas', m.spec_ringkas,
                'spec_asli', ms.spec_asli,
                'satuan', m.satuan::text,
                'lebar_value', ms.lebar_value,
                'panjang_value', ms.panjang_value,
                'tebal_value', ms.tebal_value,
                'berat_value', ms.berat_value,
                'qty_value', ms.qty_value,
                'warna', ms.warna,
                'grade', ms.grade
            )
        ) filter (where pm.id is not null),
        '[]'::jsonb
    ) as daftar_material
from public.m_part p
left join public.m_part_material pm on pm.uniq_no = p.uniq_no and pm.aktif = true
left join public.m_material m on m.id = pm.material_id and m.aktif = true
left join public.m_supplier s on s.id = m.supplier_id and s.aktif = true
left join public.m_material_spec ms on ms.id = pm.material_spec_id and ms.aktif = true
where p.aktif = true
group by
    p.id,
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    p.total_item_per_kanban,
    p.sample_item_per_kanban,
    p.sample_cycle_note,
    p.lokasi_gambar;

-- 7. RECREATE CUTTING VIEWS
create view public.v_cutting_material_option as
select
    m.id as material_id,
    m.nama_material,
    coalesce(m.spec_ringkas, m.spec, '') as spec_ringkas,
    coalesce(m.satuan::text, 'UNKNOWN') as satuan,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'id', ukuran.id,
                'ukuran_cutting_cm', ukuran.size_cutting_cm,
                'size_cutting_cm', ukuran.size_cutting_cm,
                'lebar_roll_cm', ukuran.lebar_roll_cm,
                'panjang_roll_cm', ukuran.panjang_roll_cm,
                'berat_gsm', ukuran.berat_gsm,
                'tebal_mm', ukuran.tebal_mm,
                'label_ukuran', ukuran.label_ukuran,
                'is_default', ukuran.is_default,
                'urutan', ukuran.urutan
            )
        ) filter (where ukuran.id is not null and ukuran.aktif = true),
        '[]'::jsonb
    ) as daftar_ukuran_cutting,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'id_defect', d.id_defect,
                'nama_defect', d.nama_defect,
                'satuan_input', coalesce(md.satuan_input, d.satuan_input),
                'metode_pengukuran', coalesce(md.metode_pengukuran, d.metode_pengukuran),
                'urutan', md.urutan
            )
        ) filter (where d.id_defect is not null),
        '[]'::jsonb
    ) as daftar_defect_cutting
from public.m_material m
left join public.m_cutting_size_reference ukuran
    on ukuran.material_id = m.id
   and ukuran.aktif = true
left join public.m_material_defect md
    on md.material_id = m.id
   and md.aktif = true
   and md.proses_scope in ('ALL', 'CUTTING')
left join public.m_defect d
    on d.id_defect = md.id_defect
   and d.aktif = true
where m.aktif = true
group by
    m.id,
    m.nama_material,
    m.spec_ringkas,
    m.spec,
    m.satuan;

create view public.v_cutting_part_size_option as
select
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.komoditas::text as komoditas,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'id', referensi.id,
                'size_cutting_cm', referensi.size_cutting_cm,
                'ukuran_cutting_cm', referensi.size_cutting_cm,
                'urutan', referensi.urutan
            ) order by referensi.urutan, referensi.size_cutting_cm
        ) filter (where referensi.id is not null),
        '[]'::jsonb
    ) as daftar_ukuran_cutting
from public.m_part p
join public.m_part_cutting_size_reference referensi
    on referensi.uniq_no = p.uniq_no
   and referensi.aktif = true
where p.aktif = true
group by p.uniq_no, p.part_no, p.nama_part, p.model, p.komoditas;

-- 8. PERMISSIONS
-- In standard PostgreSQL, GRANT ON ALL TABLES includes Views.
grant select on all tables in schema public to anon, authenticated;

-- Refresh Cache
select public.f_touch_data_revision('CHECKSHEET_REFERENCE');
select pg_notify('pgrst', 'reload schema');

commit;
