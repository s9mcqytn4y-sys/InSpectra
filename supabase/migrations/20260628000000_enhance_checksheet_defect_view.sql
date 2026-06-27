-- =========================================================
-- ENHANCE CHECKSHEET PART DEFECT VIEW
-- Includes material-sourced defects into the part defect list.
-- =========================================================

begin;

drop view if exists public.v_checksheet_part_defect cascade;

create or replace view public.v_checksheet_part_defect as
with combined_defects as (
    -- Defect Proses (Directly linked to part)
    select
        pd.uniq_no,
        d.id_defect,
        d.nama_defect,
        'PROSES' as kategori,
        pd.urutan
    from public.m_part_defect pd
    join public.m_defect d on d.id_defect = pd.id_defect
    where pd.aktif = true and d.aktif = true

    union

    -- Defect Material (Linked via materials used by part)
    select
        pm.uniq_no,
        d.id_defect,
        d.nama_defect,
        'MATERIAL' as kategori,
        100 + md.urutan as urutan -- Put material defects after process defects
    from public.m_part_material pm
    join public.m_material_defect md on md.material_id = pm.material_id
    join public.m_defect d on d.id_defect = md.id_defect
    where pm.aktif = true and md.aktif = true and d.aktif = true
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
                'id_defect', cd.id_defect,
                'nama_defect', cd.nama_defect,
                'kategori', cd.kategori,
                'urutan', cd.urutan
            )
            order by cd.urutan, cd.nama_defect
        ) filter (where cd.id_defect is not null),
        '[]'::jsonb
    ) as daftar_defect
from public.m_part p
left join combined_defects cd on cd.uniq_no = p.uniq_no
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

grant select on public.v_checksheet_part_defect to anon, authenticated;

-- Ensure schema cache is reloaded
select pg_notify('pgrst', 'reload schema');

commit;
