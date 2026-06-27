-- File: 20260628000002_grant_view_app_bootstrap.sql
-- Description: Memberi akses baca pada v_app_bootstrap ke role anon dan authenticated

begin;

create or replace view public.v_app_bootstrap as
select
    'inspectra' as app_code,
    '2026-06-21-master-relation-media-cutting-input-vnext' as schema_revision,
    now() as server_time,
    (
        select jsonb_object_agg(kode, versi)
        from public.m_data_revision
    ) as data_revision,
    (
        select count(*)
        from public.m_part
        where aktif = true and komoditas = 'PRESS'
    ) as total_press_part,
    (
        select count(*)
        from public.m_part
        where aktif = true and komoditas = 'SEWING'
    ) as total_sewing_part,
    (
        select count(*)
        from public.m_material
        where aktif = true
    ) as total_material,
    (
        select count(*)
        from public.m_defect
        where aktif = true
    ) as total_defect,
    (
        select count(*)
        from public.v_checksheet_part_picker
        where status_input <> 'SIAP_INPUT'
    ) as total_part_belum_siap;

grant select on public.v_app_bootstrap to anon, authenticated;

commit;
