-- ============================================================================
-- Project     : InSpectra
-- File        : 20260626000000_refine_checksheet_data.sql
-- Description : Refine master data, material-defect mapping, and checksheet view
--               based on user reference images.
-- ============================================================================

begin;

-- 1. Sync Master Defects from Reference Image
-- Rename SPUNBOND_MENGERAS to SPUNBOND_HARDEN and add new defects if missing
update public.m_defect
set nama_defect = 'Spunbond Harden'
where id_defect = 'SPUNBOND_MENGERAS';

insert into public.m_defect (id_defect, nama_defect, kategori, proses_default, satuan_input, metode_pengukuran)
values
    ('DIMENSI_OUT_STANDARD', 'Dimensi out standard', 'PROSES', 'PRESS', 'PCS', 'COUNT'),
    ('DIMENSI_TIDAK_STANDAR_RE', 'Dimensi Tidak Standar', 'PROSES', 'PRESS', 'PCS', 'COUNT'), -- Duplicate name but different context in image
    ('HOLE_TA', 'Hole T/A', 'PROSES', 'PRESS', 'PCS', 'COUNT')
on conflict (id_defect) do update set
    nama_defect = excluded.nama_defect,
    kategori = excluded.kategori;

-- 2. Refine Material-Defect Mappings (Based on Image 4)
-- Helper to map defects to material by name
do $$
declare
    v_mat_id uuid;
begin
    -- PS Polyester Non Woven Spunbond 100 Gsm White
    select id into v_mat_id from public.m_material where nama_material = 'PS Polyester Non Woven Spunbond 100 GSM White';
    if v_mat_id is not null then
        insert into public.m_material_defect (material_id, id_defect, urutan)
        values
            (v_mat_id, 'SPUNBOND_TIDAK_MEREKAT', 1),
            (v_mat_id, 'KOTOR', 2),
            (v_mat_id, 'SPUNBOND_TERLIPAT', 3),
            (v_mat_id, 'SPUNBOND_MENGERAS', 4) -- mapped to old ID
        on conflict do nothing;
    end if;

    -- Laminasi LDPE 200 Gsm
    select id into v_mat_id from public.m_material where nama_material = 'Laminasi LDPE 200 GSM';
    if v_mat_id is not null then
        insert into public.m_material_defect (material_id, id_defect, urutan)
        values
            (v_mat_id, 'LAMINATING_BOLONG', 1),
            (v_mat_id, 'LAMINATING_TIDAK_MATANG', 2)
        on conflict do nothing;
    end if;

    -- Recycle Felt GWPS 2mm 375 Gsm
    select id into v_mat_id from public.m_material where nama_material = 'Recycle Felt GWPS 2mm 375 GSM';
    if v_mat_id is not null then
        insert into public.m_material_defect (material_id, id_defect, urutan)
        values
            (v_mat_id, 'SOBEK', 1),
            (v_mat_id, 'BRUDUL', 2),
            (v_mat_id, 'TIPIS', 3)
        on conflict do nothing;
    end if;
end $$;

-- 3. Refine COMPOSITE PART & MATERIAL (Based on Image 2 & 3)
-- Ensure BJ1 has correct materials
do $$
declare
    v_part_uniq text := 'BJ1';
    v_mat_id uuid;
begin
    -- Protector
    select id into v_mat_id from public.m_material where nama_material = 'Protector';
    if v_mat_id is not null then
        insert into public.m_part_material (uniq_no, material_id, label_material, urutan)
        values (v_part_uniq, v_mat_id, 'Protector', 1) on conflict do nothing;
    end if;

    -- Carpet Assy Neddle D26
    select id into v_mat_id from public.m_material where nama_material = 'Carpet Assy Needle D26';
    if v_mat_id is not null then
        insert into public.m_part_material (uniq_no, material_id, label_material, urutan)
        values (v_part_uniq, v_mat_id, 'Carpet Assy Neddle D26', 2) on conflict do nothing;
    end if;

    -- EPDM 45mm / 47mm
    select id into v_mat_id from public.m_material where nama_material = 'EPDM 45mm' limit 1;
    if v_mat_id is not null then
        insert into public.m_part_material (uniq_no, material_id, label_material, urutan)
        values (v_part_uniq, v_mat_id, 'EPDM 45mm / 47mm', 3) on conflict do nothing;
    end if;

    -- Lem Fox 2,5 Kg
    select id into v_mat_id from public.m_material where nama_material = 'Lem Fox 2,5 Kg';
    if v_mat_id is not null then
        insert into public.m_part_material (uniq_no, material_id, label_material, urutan)
        values (v_part_uniq, v_mat_id, 'Lem Fox 2,5 Kg', 5) on conflict do nothing;
    end if;
end $$;

-- 4. Update v_checksheet_part_defect to be Recursive (for Material Composition)
drop view if exists public.v_checksheet_part_defect cascade;
create or replace view public.v_checksheet_part_defect as
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
        'PROSES' as sumber,
        null as material_id
    from public.m_part_defect pd
    where pd.aktif = true

    union

    -- Defect dari Hierarchy Material
    select
        mh.uniq_no,
        md.id_defect,
        md.urutan + 100 as urutan, -- offset material defects
        'MATERIAL' as sumber,
        mh.material_id
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

-- Refresh Cache
select public.f_touch_data_revision('CHECKSHEET_REFERENCE');
select pg_notify('pgrst', 'reload schema');

commit;
