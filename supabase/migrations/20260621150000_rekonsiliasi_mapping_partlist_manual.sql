-- =========================================================
-- Rekonsiliasi mapping manual PARTLIST.xlsx
-- Sumber: workbook lokal yang diaudit pada 2026-06-21.
-- Workbook tidak dipakai saat runtime dan tidak di-commit.
--
-- Lingkup data terverifikasi:
-- - 29 baris material resmi.
-- - 65 relasi part-material yang memiliki identitas material nyata.
-- - 46 relasi material-defect operasional.
--
-- Catatan: BY18 dan BY19 ada pada matriks komposisi, tetapi tidak ada
-- pada master part BTI. Keduanya sengaja tidak dibuat sebagai part sintetis.
-- =========================================================

begin;

create temporary table seed_material_manual (
    nama_supplier text not null,
    nama_material text not null,
    spec_asli text not null,
    satuan_sumber text not null
) on commit drop;

insert into seed_material_manual (nama_supplier, nama_material, spec_asli, satuan_sumber) values
    ('PT. ARTHA LANGGENG MULYA (BTI)', 'Laminasi LDPE 200 Gsm + SPB White', '1,6 X 50', 'Roll'),
    ('PT. ARTHA LANGGENG MULYA (BTI)', 'Laminasi LDPE 200 Gsm', '0,66 X 50', 'Roll'),
    ('PT. ARTHA LANGGENG MULYA (BTI)', 'Laminasi LDPE 200 Gsm', '1,14 X 50', 'Roll'),
    ('PT. BONECOM', 'Nisseki Claff', '1.25 X 300', 'Roll'),
    ('PT. BONECOM', 'Ester Canvas SAB10-NS121 SSP', '121 X 100', 'Roll'),
    ('PT. HASIL DAMAI TEXTILE (BTI)', 'PS Polyester Non Woven Spunbond 100 Gsm White', '1,5 X 50', 'Roll'),
    ('PT. HASIL DAMAI TEXTILE (BTI)', 'PS Polyester Non Woven Spunbond 80 Gsm White', '0,45 X 300', 'Roll'),
    ('PT. HERCULON INDONESIA (BTI)', 'Carpet STKD19 Black', '1,45 X 40', 'Roll'),
    ('PT. HERCULON INDONESIA (BTI)', 'Carpet CBIII', '1,2 X 50', 'Roll'),
    ('PT. IEM', 'Benang Black #60', '1', 'Cones'),
    ('PT. INDAH VARIA EKA SELARAS', 'Hook seat Cover 72752-X1V08', '1', 'Pcs'),
    ('PT. INDAH VARIA EKA SELARAS', 'Plate Seat Cover', '1', 'Pcs'),
    ('PT. KOMKAR', 'EPDM 45mm', '0,45 X 25', 'Roll'),
    ('PT. KOMKAR', 'EPDM 47mm', '0,47 X 25', 'Roll'),
    ('PT. LANI TEDUH (BTI)', 'Recycle Felt GWPS 4mm 450 Gsm', '1,4 X 30', 'Roll'),
    ('PT. LANI TEDUH (BTI)', 'Recycle Felt GWPS 5mm 1000 Gsm', '1,3 X 25', 'Roll'),
    ('PT. LANI TEDUH (BTI)', 'Recycle Felt GWPS 2mm 375 Gsm', '1,8 X 50', 'Roll'),
    ('PT. LANI TEDUH (BTI)', 'Recycle Felt GWPS 2mm 375 Gsm + Spunbond', '1,6 X 50', 'Roll'),
    ('PT. MARGAJAYA (BTI)', 'EPDM 45mm', '0,45 X 25', 'Roll'),
    ('PT. MARGAJAYA (BTI)', 'EPDM 47mm', '0,47 X 25', 'Roll'),
    ('PT. MULTIWARNA KARPETINDO (BTI)', 'Laminasi 200Gsm Transparant', '1,4 X 30', 'Roll'),
    ('PT. NATIONAL LABEL', 'Indication Tag Tafeta Felt PE , PET', '1', 'Pcs'),
    ('PT. RAJAWALI MITRA PRATAMA', 'Lem Fox 2,5 Kg', '2,5 KG', 'Cain'),
    ('PT. SUPERBTEX', 'Silincer T. 15mm 1000 Gsm', '1 X 20', 'Roll'),
    ('PT. SUPERBTEX', 'Silincer T. 10mm 500 Gsm', '1 X 20', 'Roll'),
    ('PT. SUPERBTEX', 'Silincer T. 6mm 350 Gsm', '1 X 20', 'Roll'),
    ('PT. FAM', 'Plastic Packing70 x 100cm', '1', 'Kg'),
    ('PT. FAM', 'Carpet Assy Neddle D26', '1', 'Pcs'),
    ('PT. TRIMITRA SWADAYA', 'Spondbond Black 50gsm 50 x 500 mtr', '1', 'Mtr');

insert into public.m_supplier (nama_supplier, aktif)
select distinct nama_supplier, true
from seed_material_manual
on conflict (nama_supplier) do update set
    aktif = true,
    diperbarui_pada = now();

insert into public.m_material (
    nama_material,
    supplier_id,
    supplier_manual,
    spec_ringkas,
    spec,
    satuan,
    aktif,
    diperbarui_pada
)
select
    material.nama_material,
    supplier.id,
    material.nama_supplier,
    material.spec_asli,
    material.spec_asli,
    case upper(material.satuan_sumber)
        when 'ROLL' then 'ROLL'::public.satuan_inspectra
        when 'PCS' then 'PCS'::public.satuan_inspectra
        when 'CONES' then 'CONES'::public.satuan_inspectra
        else 'UNKNOWN'::public.satuan_inspectra
    end,
    true,
    now()
from seed_material_manual material
join public.m_supplier supplier on supplier.nama_supplier = material.nama_supplier
on conflict (nama_normalisasi, supplier_id, spec_ringkas) do update set
    nama_material = excluded.nama_material,
    supplier_manual = excluded.supplier_manual,
    spec = excluded.spec,
    satuan = excluded.satuan,
    aktif = true,
    diperbarui_pada = now();

insert into public.m_material_spec (
    material_id,
    spec_asli,
    keterangan,
    aktif,
    diperbarui_pada
)
select
    material.id,
    sumber.spec_asli,
    'Seed manual PARTLIST.xlsx 2026-06-21',
    true,
    now()
from seed_material_manual sumber
join public.m_supplier supplier on supplier.nama_supplier = sumber.nama_supplier
join public.m_material material
    on material.nama_normalisasi = upper(trim(regexp_replace(sumber.nama_material, '\s+', ' ', 'g')))
   and material.supplier_id = supplier.id
   and material.spec_ringkas = sumber.spec_asli
on conflict (material_id, spec_asli) do update set
    keterangan = excluded.keterangan,
    aktif = true,
    diperbarui_pada = now();

-- Pastikan part Sewing yang menjadi target mapping tidak kembali menjadi Press.
insert into public.m_part (uniq_no, part_no, nama_part, model, customer, komoditas, aktif, diperbarui_pada) values
    ('BJ1', '79977-BZ020', 'FELT SEAT BACK', 'D26A', 'BONECOM TRICOM', 'SEWING', true, now()),
    ('B35', '71695-VT070-C', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', true, now()),
    ('B63', '71695-VT080', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', true, now()),
    ('B51', '71695-VT090-B', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', true, now()),
    ('B70', '71695-VT100-B', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', true, now()),
    ('B55', '71695-VT110-D', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', true, now()),
    ('B72', '71695-VT120-D', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', true, now())
on conflict (uniq_no) do update set
    part_no = excluded.part_no,
    nama_part = excluded.nama_part,
    model = excluded.model,
    customer = excluded.customer,
    komoditas = excluded.komoditas,
    aktif = true,
    diperbarui_pada = now();

create temporary table seed_part_material_manual (
    uniq_no text not null,
    nama_supplier text not null,
    nama_material text not null,
    spec_asli text not null,
    urutan int not null
) on commit drop;

-- Komposisi BJ1 yang sudah mempunyai master part.
insert into seed_part_material_manual values
    ('BJ1', 'PT. LANI TEDUH (BTI)', 'Recycle Felt GWPS 2mm 375 Gsm', '1,8 X 50', 1),
    ('BJ1', 'PT. HASIL DAMAI TEXTILE (BTI)', 'PS Polyester Non Woven Spunbond 100 Gsm White', '1,5 X 50', 2),
    ('BJ1', 'PT. ARTHA LANGGENG MULYA (BTI)', 'Laminasi LDPE 200 Gsm', '0,66 X 50', 3),
    ('BJ1', 'PT. ARTHA LANGGENG MULYA (BTI)', 'Laminasi LDPE 200 Gsm', '1,14 X 50', 4),
    ('BJ1', 'PT. FAM', 'Carpet Assy Neddle D26', '1', 5);

-- Enam varian Protector memakai komposisi material yang sama.
insert into seed_part_material_manual (uniq_no, nama_supplier, nama_material, spec_asli, urutan)
select
    part.uniq_no,
    bahan.nama_supplier,
    bahan.nama_material,
    bahan.spec_asli,
    bahan.urutan
from (values ('B35'), ('B63'), ('B51'), ('B70'), ('B55'), ('B72')) as part(uniq_no)
cross join (values
    ('PT. LANI TEDUH (BTI)', 'Recycle Felt GWPS 2mm 375 Gsm', '1,8 X 50', 1),
    ('PT. HASIL DAMAI TEXTILE (BTI)', 'PS Polyester Non Woven Spunbond 100 Gsm White', '1,5 X 50', 2),
    ('PT. ARTHA LANGGENG MULYA (BTI)', 'Laminasi LDPE 200 Gsm', '0,66 X 50', 3),
    ('PT. ARTHA LANGGENG MULYA (BTI)', 'Laminasi LDPE 200 Gsm', '1,14 X 50', 4),
    ('PT. NATIONAL LABEL', 'Indication Tag Tafeta Felt PE , PET', '1', 5),
    ('PT. INDAH VARIA EKA SELARAS', 'Hook seat Cover 72752-X1V08', '1', 6),
    ('PT. IEM', 'Benang Black #60', '1', 7)
) as bahan(nama_supplier, nama_material, spec_asli, urutan);

-- Insulation Sheet 1-3 menerima EPDM dari seluruh supplier yang disetujui.
insert into seed_part_material_manual (uniq_no, nama_supplier, nama_material, spec_asli, urutan)
select
    part.uniq_no,
    bahan.nama_supplier,
    bahan.nama_material,
    bahan.spec_asli,
    bahan.urutan
from (values ('MM1'), ('ML0'), ('ML1')) as part(uniq_no)
cross join (values
    ('PT. KOMKAR', 'EPDM 45mm', '0,45 X 25', 1),
    ('PT. KOMKAR', 'EPDM 47mm', '0,47 X 25', 2),
    ('PT. MARGAJAYA (BTI)', 'EPDM 45mm', '0,45 X 25', 3),
    ('PT. MARGAJAYA (BTI)', 'EPDM 47mm', '0,47 X 25', 4),
    ('PT. HASIL DAMAI TEXTILE (BTI)', 'PS Polyester Non Woven Spunbond 80 Gsm White', '0,45 X 300', 5),
    ('PT. RAJAWALI MITRA PRATAMA', 'Lem Fox 2,5 Kg', '2,5 KG', 6)
) as bahan(nama_supplier, nama_material, spec_asli, urutan);

-- Hanya part di bawah ini yang direkonsiliasi. Relasi di luar sumber manual tidak disentuh.
update public.m_part_material pemetaan
set aktif = false,
    diperbarui_pada = now()
where pemetaan.uniq_no in (select distinct uniq_no from seed_part_material_manual)
  and pemetaan.aktif = true;

with relasi_terpecahkan as (
    select
        sumber.uniq_no,
        material.id as material_id,
        spec.id as material_spec_id,
        sumber.urutan,
        material.nama_material as label_material
    from seed_part_material_manual sumber
    join public.m_supplier supplier on supplier.nama_supplier = sumber.nama_supplier
    join public.m_material material
        on material.nama_normalisasi = upper(trim(regexp_replace(sumber.nama_material, '\s+', ' ', 'g')))
       and material.supplier_id = supplier.id
       and material.spec_ringkas = sumber.spec_asli
    join public.m_material_spec spec
        on spec.material_id = material.id
       and spec.spec_asli = sumber.spec_asli
)
update public.m_part_material pemetaan
set urutan = sumber.urutan,
    label_material = sumber.label_material,
    aktif = true,
    diperbarui_pada = now()
from relasi_terpecahkan sumber
where pemetaan.uniq_no = sumber.uniq_no
  and pemetaan.material_id = sumber.material_id
  and pemetaan.material_spec_id = sumber.material_spec_id;

with relasi_terpecahkan as (
    select
        sumber.uniq_no,
        material.id as material_id,
        spec.id as material_spec_id,
        sumber.urutan,
        material.nama_material as label_material
    from seed_part_material_manual sumber
    join public.m_supplier supplier on supplier.nama_supplier = sumber.nama_supplier
    join public.m_material material
        on material.nama_normalisasi = upper(trim(regexp_replace(sumber.nama_material, '\s+', ' ', 'g')))
       and material.supplier_id = supplier.id
       and material.spec_ringkas = sumber.spec_asli
    join public.m_material_spec spec
        on spec.material_id = material.id
       and spec.spec_asli = sumber.spec_asli
)
insert into public.m_part_material (
    uniq_no,
    material_id,
    material_spec_id,
    urutan,
    label_material,
    aktif,
    diperbarui_pada
)
select
    sumber.uniq_no,
    sumber.material_id,
    sumber.material_spec_id,
    sumber.urutan,
    sumber.label_material,
    true,
    now()
from relasi_terpecahkan sumber
where not exists (
    select 1
    from public.m_part_material pemetaan
    where pemetaan.uniq_no = sumber.uniq_no
      and pemetaan.material_id = sumber.material_id
      and pemetaan.material_spec_id = sumber.material_spec_id
);

create temporary table seed_material_defect_manual (
    nama_material text not null,
    nama_defect text not null
) on commit drop;

insert into seed_material_defect_manual (nama_material, nama_defect) values
    ('PS Polyester Non Woven Spunbond 100 Gsm White', 'SPUNBOUND TIDAK MEREKAT'),
    ('PS Polyester Non Woven Spunbond 100 Gsm White', 'SPUNBOUND KOTOR'),
    ('PS Polyester Non Woven Spunbond 100 Gsm White', 'SPUNDBOUND TERLIPAT'),
    ('PS Polyester Non Woven Spunbond 100 Gsm White', 'SPUNDBOND HARDEN'),
    ('Laminasi LDPE 200 Gsm', 'LAMINATING BOLONG'),
    ('Laminasi LDPE 200 Gsm', 'LAMINATING TIDAK MATANG'),
    ('Recycle Felt GWPS 2mm 375 Gsm', 'SOBEK'),
    ('Recycle Felt GWPS 2mm 375 Gsm', 'BRUDUL'),
    ('Recycle Felt GWPS 2mm 375 Gsm', 'TIPIS'),
    ('Carpet STKD19 Black', 'SOBEK'),
    ('Carpet STKD19 Black', 'BRUDUL'),
    ('Carpet STKD19 Black', 'TIPIS'),
    ('Carpet STKD19 Black', 'BERJAMUR'),
    ('Carpet STKD19 Black', 'GALER'),
    ('Carpet STKD19 Black', 'DENT'),
    ('Carpet STKD19 Black', 'TERLIPAT'),
    ('Carpet STKD19 Black', 'BELANG'),
    ('Carpet CBIII', 'SOBEK'),
    ('Carpet CBIII', 'BRUDUL'),
    ('Carpet CBIII', 'TIPIS'),
    ('Carpet CBIII', 'BERJAMUR'),
    ('Carpet CBIII', 'GALER'),
    ('Carpet CBIII', 'DENT'),
    ('Carpet CBIII', 'TERLIPAT'),
    ('Carpet CBIII', 'BELANG'),
    ('EPDM 45mm', 'BERLUBANG'),
    ('EPDM 45mm', 'TIPIS'),
    ('EPDM 47mm', 'BERLUBANG'),
    ('EPDM 47mm', 'TIPIS'),
    ('Ester Canvas SAB10-NS121 SSP', 'SOBEK'),
    ('Ester Canvas SAB10-NS121 SSP', 'BRUDUL'),
    ('Ester Canvas SAB10-NS121 SSP', 'KOTOR'),
    ('Silincer T. 15mm 1000 Gsm', 'SOBEK'),
    ('Silincer T. 15mm 1000 Gsm', 'BRUDUL'),
    ('Silincer T. 15mm 1000 Gsm', 'KOTOR'),
    ('Silincer T. 15mm 1000 Gsm', 'MENGEMBANG'),
    ('Silincer T. 6mm 350 Gsm', 'SOBEK'),
    ('Silincer T. 6mm 350 Gsm', 'BRUDUL'),
    ('Silincer T. 6mm 350 Gsm', 'KOTOR'),
    ('Silincer T. 6mm 350 Gsm', 'MENGEMBANG');

insert into public.m_defect (id_defect, nama_defect, kategori, aktif)
select
    'MAT_' || upper(substr(md5(upper(trim(sumber.nama_defect))), 1, 12)),
    sumber.nama_defect,
    'MATERIAL'::public.kategori_defect_inspectra,
    true
from (select distinct nama_defect from seed_material_defect_manual) sumber
where not exists (
    select 1
    from public.m_defect defect
    where upper(trim(defect.nama_defect)) = upper(trim(sumber.nama_defect))
      and defect.kategori::text = 'MATERIAL'
);

with relasi_defect as (
    select
        material.id as material_id,
        defect.id_defect,
        row_number() over (
            partition by material.id
            order by sumber.nama_defect
        )::int as urutan
    from seed_material_defect_manual sumber
    join public.m_material material
        on material.nama_normalisasi = upper(trim(regexp_replace(sumber.nama_material, '\s+', ' ', 'g')))
       and material.aktif = true
    join public.m_defect defect
        on upper(trim(defect.nama_defect)) = upper(trim(sumber.nama_defect))
       and defect.kategori::text = 'MATERIAL'
)
insert into public.m_material_defect (
    material_id,
    id_defect,
    urutan,
    aktif,
    diperbarui_pada
)
select material_id, id_defect, urutan, true, now()
from relasi_defect
on conflict (material_id, id_defect) do update set
    urutan = excluded.urutan,
    aktif = true,
    diperbarui_pada = now();

create or replace view public.v_mapping_partlist_status as
select
    29::int as jumlah_material_sumber,
    65::int as jumlah_relasi_terverifikasi,
    2::int as jumlah_relasi_tanpa_master_part,
    (select count(*)::int from public.m_part_material where aktif) as jumlah_relasi_aktif,
    (select count(*)::int from public.m_material_defect where aktif) as jumlah_relasi_material_defect_aktif,
    now() as diperbarui_pada;

select pg_notify('pgrst', 'reload schema');

commit;
