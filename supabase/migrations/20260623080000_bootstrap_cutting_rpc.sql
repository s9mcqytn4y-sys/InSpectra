-- RPC untuk konsolidasi bootstrap data Cutting
-- Mengurangi round-trip request saat inisialisasi form Cutting.

create or replace function public.rpc_bootstrap_cutting()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    result jsonb;
begin
    select jsonb_build_object(
        'slot_waktu', (
            select coalesce(jsonb_agg(row_to_json(s)), '[]'::jsonb)
            from public.m_slot_waktu s
            where s.tipe_proses = 'CUTTING'
              and s.aktif = true
        ),
        'material_option', (
            select coalesce(jsonb_agg(row_to_json(m)), '[]'::jsonb)
            from public.v_cutting_material_option m
        ),
        'part_size_option', (
            select coalesce(jsonb_agg(row_to_json(p)), '[]'::jsonb)
            from public.v_cutting_part_size_option p
        ),
        'defect_option', (
            select coalesce(jsonb_agg(row_to_json(d)), '[]'::jsonb)
            from (
                select id_defect, nama_defect, kategori
                from public.m_defect
                where aktif = true
                order by nama_defect
                limit 100
            ) d
        )
    ) into result;

    return result;
end;
$$;

grant execute on function public.rpc_bootstrap_cutting() to anon, authenticated;
