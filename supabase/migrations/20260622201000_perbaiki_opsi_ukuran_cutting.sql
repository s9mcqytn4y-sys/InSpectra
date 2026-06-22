-- Jangan tampilkan referensi Cutting lama yang tidak memiliki ukuran numerik.

begin;

create or replace view public.v_cutting_material_option as
select
    material.id as material_id,
    material.nama_material,
    coalesce(material.spec_ringkas, material.spec, '') as spec_ringkas,
    coalesce(material.satuan::text, material.default_satuan, 'UNKNOWN') as satuan,
    (
        select coalesce(
            jsonb_agg(opsi order by (opsi ->> 'is_default')::boolean desc, (opsi ->> 'urutan')::int, (opsi ->> 'size_cutting_cm')::numeric),
            '[]'::jsonb
        )
        from (
            select jsonb_build_object(
                'id', ukuran.id,
                'ukuran_cutting_cm', coalesce(ukuran.size_cutting_cm, ukuran.ukuran_cutting_cm),
                'size_cutting_cm', coalesce(ukuran.size_cutting_cm, ukuran.ukuran_cutting_cm),
                'lebar_roll_cm', ukuran.lebar_roll_cm,
                'panjang_roll_cm', ukuran.panjang_roll_cm,
                'berat_gsm', ukuran.berat_gsm,
                'tebal_mm', ukuran.tebal_mm,
                'label_ukuran', ukuran.label_ukuran,
                'is_default', ukuran.is_default,
                'urutan', ukuran.urutan
            ) as opsi
            from public.m_cutting_size_reference ukuran
            where ukuran.material_id = material.id
              and ukuran.aktif = true
              and coalesce(ukuran.size_cutting_cm, ukuran.ukuran_cutting_cm) > 0
        ) ukuran_valid
    ) as daftar_ukuran_cutting,
    (
        select coalesce(
            jsonb_agg(defect order by (defect ->> 'urutan')::int, defect ->> 'nama_defect'),
            '[]'::jsonb
        )
        from (
            select jsonb_build_object(
                'id_defect', data_defect.id_defect,
                'nama_defect', data_defect.nama_defect,
                'satuan_input', coalesce(relasi_defect.satuan_input, data_defect.satuan_input),
                'metode_pengukuran', coalesce(relasi_defect.metode_pengukuran, data_defect.metode_pengukuran),
                'urutan', relasi_defect.urutan
            ) as defect
            from public.m_material_defect relasi_defect
            join public.m_defect data_defect on data_defect.id_defect = relasi_defect.id_defect
            where relasi_defect.material_id = material.id
              and relasi_defect.aktif = true
              and data_defect.aktif = true
              and relasi_defect.proses_scope in ('ALL', 'CUTTING')
        ) defect_valid
    ) as daftar_defect_cutting
from public.m_material material
where material.aktif = true;

grant select on public.v_cutting_material_option to anon, authenticated;

select pg_notify('pgrst', 'reload schema');

commit;
