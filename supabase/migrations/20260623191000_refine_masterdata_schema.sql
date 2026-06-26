-- =========================================================
-- INSPECTRA REFINEMENT
-- 1. Enhance m_material with spec columns
-- 2. Enhance v_data_induk_part with split defect counts
-- 3. Create v_part_defect_efektif
-- 4. Create rpc_bootstrap_checksheet_reference
-- =========================================================

begin;

-- 1. Tambahkan kolom spesifikasi ke m_material
alter table public.m_material
    add column if not exists lebar_roll_cm numeric,
    add column if not exists panjang_roll_cm numeric,
    add column if not exists tebal_mm numeric,
    add column if not exists berat_gsm numeric,
    add column if not exists gramasi_gsm numeric,
    add column if not exists warna text,
    add column if not exists catatan_spesifikasi text;

-- 2. View untuk Defect Efektif Part (Proses + Material)
create or replace view public.v_part_defect_efektif as
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

-- 3. Update View ViewDataIndukPart dengan split counts
create or replace view public.v_data_induk_part as
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

-- 4. RPC Bootstrap Checksheet
create or replace function public.rpc_bootstrap_checksheet_reference(p_komoditas text)
returns jsonb
language plpgsql
security definer
as $$
declare
    v_parts jsonb;
    v_defects jsonb;
    v_revision bigint;
begin
    -- Get parts picker list
    select jsonb_agg(v.*) into v_parts
    from public.v_checksheet_part_picker v
    where v.komoditas = p_komoditas;

    -- Get effective defects for all parts in this commodity
    select jsonb_agg(v.*) into v_defects
    from (
        select v.*
        from public.v_checksheet_part_defect v
        where v.komoditas = p_komoditas
    ) v;

    select versi into v_revision
    from public.m_data_revision
    where kode = 'CHECKSHEET_REFERENCE';

    return jsonb_build_object(
        'parts', coalesce(v_parts, '[]'::jsonb),
        'defects', coalesce(v_defects, '[]'::jsonb),
        'revision', coalesce(v_revision, 1)
    );
end;
$$;

grant execute on function public.rpc_bootstrap_checksheet_reference(text) to anon, authenticated;

commit;
