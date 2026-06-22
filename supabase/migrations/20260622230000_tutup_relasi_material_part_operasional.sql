-- ============================================================================
-- Project     : InSpectra
-- Deskripsi   : Menutup relasi material part operasional dari sumber terverifikasi
--               dan mencegah part berstatus siap diperiksa tanpa material.
-- Sumber      : Part list Google Drive, FM-QA-010, FM-QA-010B, FM-QA-011,
--               Warning Sheet 560B, dan PI Carpet RR Seat No 2.
-- ============================================================================

begin;

set search_path = public;

-- FM-QA-010 menamai material JP6 tanpa supplier atau spesifikasi. Simpan nilai
-- sumber apa adanya, lalu tandai untuk verifikasi alih-alih mengarang supplier.
insert into public.m_material (
    nama_material,
    supplier_id,
    supplier,
    jenis_material,
    satuan,
    aktif,
    status_kelengkapan,
    butuh_review,
    catatan_review
)
select
    'FELT BEFORE LAMINATING',
    supplier_unknown.id,
    supplier_unknown.nama_supplier,
    'FELT',
    'UNKNOWN'::public.satuan_inspectra,
    true,
    'PERLU_REVIEW',
    true,
    'Nama material berasal dari FM-QA-010 untuk JP6; supplier dan spesifikasi belum tersedia.'
from public.m_supplier supplier_unknown
where supplier_unknown.nama_supplier = 'UNKNOWN'
on conflict (nama_normalisasi, supplier_id, spec_ringkas) do update set
    aktif = true,
    status_kelengkapan = excluded.status_kelengkapan,
    butuh_review = excluded.butuh_review,
    catatan_review = excluded.catatan_review,
    diperbarui_pada = now();

-- Setiap relasi berikut memakai identitas part dan material yang muncul pada
-- dokumen sumber. BT18/BT19 tidak diganti menjadi BY18/BY19 karena kedua kode
-- berkonflik antardokumen; nomor part dan modelnya sama, sehingga tetap ditandai
-- Perlu Verifikasi untuk menghindari duplikasi part operasional.
with sumber_relasi (
    uniq_no,
    nama_material,
    nama_supplier,
    urutan,
    label_material
) as (
    values
        ('BT18', 'Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', 1, 'Carpet STKD19 Black'),
        ('BT18', 'Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', 2, 'Plate Seat Cover'),
        ('BT18', 'Benang Black #60', 'PT. IEM', 3, 'Benang Black #60'),
        ('BT19', 'Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', 1, 'Carpet STKD19 Black'),
        ('BT19', 'Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', 2, 'Plate Seat Cover'),
        ('BT19', 'Benang Black #60', 'PT. IEM', 3, 'Benang Black #60'),
        ('BT201', 'Protector', 'UNKNOWN', 1, 'Protector'),
        ('FE7', 'Ester Canvas Strap', 'UNKNOWN', 1, 'Ester Canvas Strap'),
        ('FE8', 'Ester Canvas Strap', 'UNKNOWN', 1, 'Ester Canvas Strap'),
        ('FJ2', 'Fujiseat Hardfelt (9Y8)', 'UNKNOWN', 1, 'Fujiseat Hardfelt (9Y8)'),
        ('FJ3', 'Fujiseat Hardfelt (9Y8)', 'UNKNOWN', 1, 'Fujiseat Hardfelt (9Y8)'),
        ('FJ4', 'Fujiseat Hardfelt (9Y8)', 'UNKNOWN', 1, 'Fujiseat Hardfelt (9Y8)'),
        ('FJ5', 'Fujiseat Hardfelt (9Y8)', 'UNKNOWN', 1, 'Fujiseat Hardfelt (9Y8)'),
        ('FJ6', 'Fujiseat Hardfelt (9Y8)', 'UNKNOWN', 1, 'Fujiseat Hardfelt (9Y8)'),
        ('JP6', 'FELT BEFORE LAMINATING', 'UNKNOWN', 1, 'FELT BEFORE LAMINATING'),
        ('JY8', 'Hardfelt (9Y6)', 'UNKNOWN', 1, 'Hardfelt (9Y6)'),
        ('JZ2', 'Hardfelt (9Y6)', 'UNKNOWN', 1, 'Hardfelt (9Y6)'),
        ('P56', 'Ester Canvas Strap', 'UNKNOWN', 1, 'Ester Canvas Strap')
)
insert into public.m_part_material (
    uniq_no,
    material_id,
    material_spec_id,
    urutan,
    label_material,
    wajib_check,
    aktif
)
select
    sumber.uniq_no,
    material.id,
    null,
    sumber.urutan,
    sumber.label_material,
    true,
    true
from sumber_relasi sumber
join public.m_part part
    on part.uniq_no = sumber.uniq_no
join public.m_supplier supplier
    on supplier.nama_supplier = sumber.nama_supplier
join public.m_material material
    on material.nama_material = sumber.nama_material
   and material.supplier_id = supplier.id
where not exists (
    select 1
    from public.m_part_material target
    where target.uniq_no = sumber.uniq_no
      and target.material_id = material.id
      and target.material_spec_id is null
);

update public.m_part
set
    butuh_review = true,
    status_kelengkapan = 'PERLU_REVIEW',
    catatan_review = 'Dokumen PI dan Warning Sheet memakai kode BY18/BY19, sedangkan Part List memakai BT18/BT19. Relasi material dipetakan memakai nomor part dan model 560B; kode UNIQ masih perlu konfirmasi.',
    diperbarui_pada = now()
where uniq_no in ('BT18', 'BT19');

create or replace function public.f_validasi_kesiapan_part_material()
returns trigger
language plpgsql
set search_path = public
as $$
declare
    kode_part text;
begin
    if tg_op = 'DELETE' then
        kode_part := old.uniq_no;
    else
        kode_part := new.uniq_no;
    end if;

    if exists (
        select 1
        from public.m_part part
        where part.uniq_no = kode_part
          and part.aktif = true
          and part.komoditas in ('PRESS', 'SEWING', 'CUTTING')
          and part.status_kelengkapan = 'SIAP_DICEK'
          and not exists (
              select 1
              from public.m_part_material relasi
              where relasi.uniq_no = part.uniq_no
                and relasi.aktif = true
          )
    ) then
        raise exception 'Part % berstatus SIAP_DICEK wajib memiliki minimal satu material aktif.', kode_part;
    end if;

    return null;
end;
$$;

drop trigger if exists trg_validasi_kesiapan_part_material_part on public.m_part;
create constraint trigger trg_validasi_kesiapan_part_material_part
after insert or update or delete on public.m_part
deferrable initially deferred
for each row
execute function public.f_validasi_kesiapan_part_material();

drop trigger if exists trg_validasi_kesiapan_part_material_relasi on public.m_part_material;
create constraint trigger trg_validasi_kesiapan_part_material_relasi
after insert or update or delete on public.m_part_material
deferrable initially deferred
for each row
execute function public.f_validasi_kesiapan_part_material();

create or replace view public.v_data_induk_part as
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
    count(distinct relasi.material_id)::int as jumlah_material,
    coalesce(jsonb_array_length(defect.daftar_defect), 0)::int as jumlah_defect,
    case
        when not part.aktif then 'NONAKTIF'
        when count(distinct relasi.material_id) = 0 then 'TANPA_MATERIAL'
        when coalesce(jsonb_array_length(defect.daftar_defect), 0) = 0 then 'TANPA_DEFECT'
        else 'SIAP_INPUT'
    end as status_input
from public.m_part part
left join public.m_part_material relasi
    on relasi.uniq_no = part.uniq_no
   and relasi.aktif = true
left join public.v_checksheet_part_defect defect
    on defect.uniq_no = part.uniq_no
group by
    part.id,
    part.part_no,
    part.uniq_no,
    part.nama_part,
    part.model,
    part.customer,
    part.komoditas,
    part.lokasi_gambar,
    part.aktif,
    part.status_kelengkapan,
    part.butuh_review,
    part.catatan_review,
    defect.daftar_defect;

grant select on public.v_data_induk_part to anon, authenticated;

select public.f_touch_data_revision('MASTER_DATA');
select public.f_touch_data_revision('CHECKSHEET_REFERENCE');
select pg_notify('pgrst', 'reload schema');

commit;
