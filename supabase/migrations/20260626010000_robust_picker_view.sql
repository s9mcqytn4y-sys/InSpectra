-- =========================================================
-- ROBUST PICKER VIEW & DATA LOADING FIX
-- Ensure CUTTING is included and view is optimized for performance
-- =========================================================

begin;

drop view if exists public.v_checksheet_part_picker cascade;

create or replace view public.v_checksheet_part_picker as
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

-- Update data revision to force app reload
select public.f_touch_data_revision('CHECKSHEET_REFERENCE');

commit;
