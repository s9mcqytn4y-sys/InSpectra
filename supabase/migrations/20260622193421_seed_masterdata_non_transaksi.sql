-- ============================================================================
-- Project     : InSpectra
-- File        : 20260622193421_seed_masterdata_non_transaksi.sql
-- Deskripsi   : Seed Master Data Non-Transaksi berbasis PARTLIST(16).xlsx,
--               Part list Google Drive, FM-QA-010/010B, dan referensi Cutting.
-- Catatan     : File ini tidak menulis data transaksi.
-- ============================================================================

begin;

set search_path = public;
create extension if not exists pgcrypto;

-- ============================================================================
-- 0. SAFETY & COMPATIBILITY
-- ============================================================================

-- Hapus kolom sumber_data dari tabel referensi ukuran part Cutting sesuai permintaan.
-- Kolom metadata sumber tetap disimpan pada tabel lain yang memang membutuhkan audit.
create table if not exists public.m_part_cutting_size_reference (
    id uuid primary key default gen_random_uuid(),
    uniq_no text not null references public.m_part(uniq_no) on delete restrict,
    size_cutting_cm numeric(12, 3) not null check (size_cutting_cm > 0),
    urutan int not null default 1 check (urutan > 0),
    nama_material_sumber text,
    part_no_sumber text,
    project_sumber text,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    unique (uniq_no, size_cutting_cm)
);

alter table public.m_part_cutting_size_reference
    drop column if exists sumber_data;

alter table if exists public.m_part
    add column if not exists catatan text,
    add column if not exists status_kelengkapan text not null default 'BELUM_DICEK',
    add column if not exists butuh_review boolean not null default false,
    add column if not exists catatan_review text;

alter table if exists public.m_material
    add column if not exists spec text,
    add column if not exists supplier text,
    add column if not exists jenis_material text,
    add column if not exists status_kelengkapan text not null default 'BELUM_DICEK',
    add column if not exists butuh_review boolean not null default false,
    add column if not exists catatan_review text;

alter table if exists public.m_material_spec
    add column if not exists satuan public.satuan_inspectra not null default 'UNKNOWN',
    add column if not exists lebar_cm numeric(12, 3),
    add column if not exists panjang_roll_cm numeric(14, 3),
    add column if not exists tebal_mm numeric(12, 3),
    add column if not exists berat_gsm numeric(12, 3),
    add column if not exists gramasi_gsm numeric(12, 3),
    add column if not exists qty_default numeric(12, 3),
    add column if not exists satuan_qty text,
    add column if not exists is_default boolean not null default false,
    add column if not exists diperbarui_pada timestamptz not null default now();

alter table if exists public.m_defect
    add column if not exists proses_default text,
    add column if not exists deskripsi text,
    add column if not exists severity_default int,
    add column if not exists satuan_input text not null default 'PCS',
    add column if not exists metode_pengukuran text not null default 'COUNT',
    add column if not exists proses_scope text not null default 'ALL',
    add column if not exists diperbarui_pada timestamptz not null default now();

alter table if exists public.m_material_defect
    add column if not exists proses_scope text not null default 'ALL',
    add column if not exists satuan_input text,
    add column if not exists metode_pengukuran text,
    add column if not exists severity int,
    add column if not exists catatan text;

alter table if exists public.m_part_defect
    add column if not exists sumber text not null default 'PROSES',
    add column if not exists proses_scope text not null default 'ALL',
    add column if not exists satuan_input text,
    add column if not exists metode_pengukuran text,
    add column if not exists catatan text;

create table if not exists public.m_material_supplier (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.m_material(id) on delete cascade,
    supplier_id uuid not null references public.m_supplier(id) on delete restrict,
    supplier_part_no text,
    supplier_material_name text,
    is_preferred boolean not null default true,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    unique (material_id, supplier_id)
);

create table if not exists public.m_material_komposisi (
    id uuid primary key default gen_random_uuid(),
    parent_material_id uuid not null references public.m_material(id) on delete cascade,
    child_material_id uuid not null references public.m_material(id) on delete restrict,
    child_material_spec_id uuid references public.m_material_spec(id) on delete restrict,
    peran_material text not null default 'KOMPONEN',
    urutan int not null default 1 check (urutan > 0),
    qty numeric(14, 4),
    satuan text,
    wajib boolean not null default true,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    check (parent_material_id <> child_material_id)
);

create unique index if not exists ux_material_komposisi_parent_child_role
on public.m_material_komposisi (
    parent_material_id,
    child_material_id,
    coalesce(child_material_spec_id, '00000000-0000-0000-0000-000000000000'::uuid),
    peran_material
);

create table if not exists public.m_part_child (
    id uuid primary key default gen_random_uuid(),
    parent_uniq_no text not null references public.m_part(uniq_no) on delete cascade,
    child_uniq_no text not null references public.m_part(uniq_no) on delete restrict,
    peran_child text not null default 'KOMPONEN',
    qty numeric(14, 4) not null default 1 check (qty > 0),
    urutan int not null default 1 check (urutan > 0),
    aktif boolean not null default true,
    catatan text,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    check (parent_uniq_no <> child_uniq_no),
    unique (parent_uniq_no, child_uniq_no, peran_child)
);

create table if not exists public.m_default_image (
    kode text primary key,
    label text not null,
    icon_key text,
    bucket_id text,
    storage_path text,
    public_url text,
    aktif boolean not null default true
);

create table if not exists public.m_data_revision (
    kode text primary key,
    versi bigint not null default 1,
    deskripsi text,
    diperbarui_pada timestamptz not null default now()
);

create or replace function public.f_touch_data_revision(kode_revision text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.m_data_revision (kode, versi, diperbarui_pada)
    values (kode_revision, 1, now())
    on conflict (kode) do update set
        versi = public.m_data_revision.versi + 1,
        diperbarui_pada = now();
end;
$$;

-- ============================================================================
-- 1. TEMP SOURCE TABLES
-- ============================================================================

create temporary table seed_supplier (
    kode_supplier text,
    nama_supplier text,
    kategori text,
    aktif boolean
) on commit drop;

insert into seed_supplier values
        ('SUP-ALM', 'PT. ARTHA LANGGENG MULYA (BTI)', 'MATERIAL', true),
        ('SUP-BONECOM', 'PT. BONECOM', 'MATERIAL', true),
        ('SUP-FAM', 'PT. FAM', 'PART_ASSEMBLY', true),
        ('SUP-HDT', 'PT. HASIL DAMAI TEXTILE (BTI)', 'MATERIAL', true),
        ('SUP-HERCULON', 'PT. HERCULON INDONESIA (BTI)', 'MATERIAL', true),
        ('SUP-IEM', 'PT. IEM', 'BENANG', true),
        ('SUP-IVES', 'PT. INDAH VARIA EKA SELARAS', 'MATERIAL', true),
        ('SUP-KOMKAR', 'PT. KOMKAR', 'MATERIAL', true),
        ('SUP-LANI', 'PT. LANI TEDUH (BTI)', 'MATERIAL', true),
        ('SUP-MARGAJAYA', 'PT. MARGAJAYA (BTI)', 'MATERIAL', true),
        ('SUP-MULTIWARNA', 'PT. MULTIWARNA KARPETINDO (BTI)', 'MATERIAL', true),
        ('SUP-NATIONAL', 'PT. NATIONAL LABEL', 'LABEL', true),
        ('SUP-RMP', 'PT. RAJAWALI MITRA PRATAMA', 'PEREKAT', true),
        ('SUP-SUPERBTEX', 'PT. SUPERBTEX', 'MATERIAL', true),
        ('SUP-TRIMITRA', 'PT. TRIMITRA SWADAYA', 'MATERIAL', true),
        ('SUP-UNKNOWN', 'UNKNOWN', 'BELUM_DITENTUKAN', true);

create temporary table seed_material (
    nama_material text,
    nama_supplier text,
    spec_ringkas text,
    satuan text,
    jenis_material text,
    aktif boolean
) on commit drop;

insert into seed_material values
        ('Benang Black #60', 'PT. IEM', '1', 'CONES', 'BENANG', true),
        ('Black Spunbond 50 GSM', 'UNKNOWN', null, 'ROLL', 'SPUNBOND', true),
        ('Carpet Assy Needle D26', 'PT. FAM', '1', 'PCS', 'CARPET', true),
        ('Carpet black 200GSM+Latex 50GSM (MWSB-87)', 'UNKNOWN', null, 'ROLL', 'CARPET', true),
        ('Carpet CB-III', 'PT. HERCULON INDONESIA (BTI)', '1,2 X 50', 'ROLL', 'CARPET', true),
        ('CARPET CONSOLE BOX', 'UNKNOWN', null, 'UNKNOWN', 'CARPET', true),
        ('CARPET CONSOLE BOX (EXP)', 'UNKNOWN', null, 'UNKNOWN', 'CARPET', true),
        ('CARPET NO. 1 SEAT CUSHION', 'UNKNOWN', null, 'UNKNOWN', 'CARPET', true),
        ('Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', '1,45 X 40', 'ROLL', 'CARPET', true),
        ('CLAF 1', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('CLAF 2', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('CLOTH FR SEAT CUSH UNDER', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('CLOTH SEAT CUSH, UNDER', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('EPDM 45mm', 'PT. KOMKAR', '0,45 X 25', 'ROLL', 'EPDM', true),
        ('EPDM 45mm', 'PT. MARGAJAYA (BTI)', '0,45 X 25', 'ROLL', 'EPDM', true),
        ('EPDM 47mm', 'PT. KOMKAR', '0,47 X 25', 'ROLL', 'EPDM', true),
        ('EPDM 47mm', 'PT. MARGAJAYA (BTI)', '0,47 X 25', 'ROLL', 'EPDM', true),
        ('Ester Canvas SAB10-NS121 SSP', 'PT. BONECOM', '121 X 100', 'ROLL', 'CANVAS', true),
        ('Ester Canvas Strap', 'UNKNOWN', null, 'ROLL', 'CANVAS', true),
        ('FELT', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT 24*188', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT 40%', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT 60%', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT BENCH 6:4', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT CLOTH 560B', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT FR LH', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT FR RH', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT FRONT BACK LH', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT FRONT BACK LH (LOW - G)', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT FRONT BACK RH', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('FELT FRONT BACK RH ( LOW- G', 'UNKNOWN', null, 'UNKNOWN', 'FELT', true),
        ('Fujiseat Hardfelt (9Y8)', 'UNKNOWN', null, 'ROLL', 'FELT', true),
        ('Hardfelt (9Y6)', 'UNKNOWN', null, 'ROLL', 'FELT', true),
        ('Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', 'PCS', 'AKSESORI', true),
        ('Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', 'PCS', 'FELT', true),
        ('INSULATION SHEET NO. 4/FT7', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('INSULATION SHEET NO. 5/FT8', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('Laminasi 200Gsm Transparant', 'PT. MULTIWARNA KARPETINDO (BTI)', '1,4 X 30', 'ROLL', 'LAMINASI', true),
        ('Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 'ROLL', 'LAMINASI', true),
        ('Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 'ROLL', 'LAMINASI', true),
        ('Laminasi LDPE 200 GSM + SPB White', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,6 X 50', 'ROLL', 'LAMINASI', true),
        ('Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', '2,5 KG', 'CAN', 'PEREKAT', true),
        ('Nisseki Claff', 'PT. BONECOM', '1.25 X 300', 'ROLL', 'LAINNYA', true),
        ('PAD FR DOOR SILENCER', 'UNKNOWN', null, 'UNKNOWN', 'SILENCER', true),
        ('PAD FR DOOR SILENCER T5 D 200GR', 'UNKNOWN', null, 'UNKNOWN', 'SILENCER', true),
        ('PAD RR DOOR SILENCER T5 D 200GR', 'UNKNOWN', null, 'UNKNOWN', 'SILENCER', true),
        ('PAD RR SEAT BACK LH', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('PAD RR SEAT BACK RH', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('PAD SETTEN LH', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('PAD SETTEN RH', 'UNKNOWN', null, 'UNKNOWN', 'LAINNYA', true),
        ('Plastic Packing70 x 100cm', 'PT. FAM', '1', 'KG', 'KEMASAN', true),
        ('Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', '1', 'PCS', 'AKSESORI', true),
        ('Protector', 'UNKNOWN', null, 'PCS', 'LAINNYA', true),
        ('PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 'ROLL', 'SPUNBOND', true),
        ('PS Polyester Non Woven Spunbond 80 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '0,45 X 300', 'ROLL', 'SPUNBOND', true),
        ('Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 'ROLL', 'FELT', true),
        ('Recycle Felt GWPS 2mm 375 GSM + Spunbond', 'PT. LANI TEDUH (BTI)', '1,6 X 50', 'ROLL', 'SPUNBOND', true),
        ('Recycle Felt GWPS 4mm 450 GSM', 'PT. LANI TEDUH (BTI)', '1,4 X 30', 'ROLL', 'FELT', true),
        ('Recycle Felt GWPS 5mm 1000 GSM', 'PT. LANI TEDUH (BTI)', '1,3 X 25', 'ROLL', 'FELT', true),
        ('Silencer T. 10mm 500 GSM', 'PT. SUPERBTEX', '1 X 20', 'ROLL', 'SILENCER', true),
        ('Silencer T. 15mm 1000 GSM', 'PT. SUPERBTEX', '1 X 20', 'ROLL', 'SILENCER', true),
        ('Silencer T. 6mm 350 GSM', 'PT. SUPERBTEX', '1 X 20', 'ROLL', 'SILENCER', true),
        ('Spunbond Black 50gsm 50 x 500 MTR', 'PT. TRIMITRA SWADAYA', '1', 'MTR', 'SPUNBOND', true),
        ('STRAP, SEAT COVER', 'UNKNOWN', null, 'UNKNOWN', 'CANVAS', true);

create temporary table seed_material_spec (
    nama_material text,
    nama_supplier text,
    spec_ringkas text,
    spec_asli text,
    satuan text,
    lebar_cm numeric,
    panjang_roll_cm numeric,
    tebal_mm numeric,
    berat_gsm numeric,
    qty_default numeric,
    aktif boolean
) on commit drop;

insert into seed_material_spec values
        ('Benang Black #60', 'PT. IEM', '1', '1', 'CONES', null, null, null, null, 1.0, true),
        ('Black Spunbond 50 GSM', 'UNKNOWN', null, '50 GSM/GR', 'ROLL', null, null, null, 50.0, null, true),
        ('Carpet Assy Needle D26', 'PT. FAM', '1', '1', 'PCS', null, null, null, null, 1.0, true),
        ('Carpet black 200GSM+Latex 50GSM (MWSB-87)', 'UNKNOWN', null, '200 GSM/GR', 'ROLL', null, null, null, 200.0, null, true),
        ('Carpet CB-III', 'PT. HERCULON INDONESIA (BTI)', '1,2 X 50', '1,2 X 50', 'ROLL', 120.0, 5000.0, null, null, null, true),
        ('Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', '1,45 X 40', '1,45 X 40', 'ROLL', 145.0, 4000.0, null, null, null, true),
        ('EPDM 45mm', 'PT. KOMKAR', '0,45 X 25', '0,45 X 25', 'ROLL', 45.0, 2500.0, 45.0, null, null, true),
        ('EPDM 45mm', 'PT. MARGAJAYA (BTI)', '0,45 X 25', '0,45 X 25', 'ROLL', 45.0, 2500.0, 45.0, null, null, true),
        ('EPDM 47mm', 'PT. KOMKAR', '0,47 X 25', '0,47 X 25', 'ROLL', 47.0, 2500.0, 47.0, null, null, true),
        ('EPDM 47mm', 'PT. MARGAJAYA (BTI)', '0,47 X 25', '0,47 X 25', 'ROLL', 47.0, 2500.0, 47.0, null, null, true),
        ('Ester Canvas SAB10-NS121 SSP', 'PT. BONECOM', '121 X 100', '121 X 100', 'ROLL', 121.0, 10000.0, null, null, null, true),
        ('Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', '1', 'PCS', null, null, null, null, 1.0, true),
        ('Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', '1', 'PCS', null, null, null, null, 1.0, true),
        ('Laminasi 200Gsm Transparant', 'PT. MULTIWARNA KARPETINDO (BTI)', '1,4 X 30', '1,4 X 30', 'ROLL', 140.0, 3000.0, null, 200.0, null, true),
        ('Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', '0,66 X 50', 'ROLL', 66.0, 5000.0, null, 200.0, null, true),
        ('Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', '1,14 X 50', 'ROLL', 113.99999999999999, 5000.0, null, 200.0, null, true),
        ('Laminasi LDPE 200 GSM + SPB White', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,6 X 50', '1,6 X 50', 'ROLL', 160.0, 5000.0, null, 200.0, null, true),
        ('Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', '2,5 KG', '2,5 KG', 'CAN', null, null, null, null, 2.5, true),
        ('Nisseki Claff', 'PT. BONECOM', '1.25 X 300', '1.25 X 300', 'ROLL', 125.0, 30000.0, null, null, null, true),
        ('PAD FR DOOR SILENCER T5 D 200GR', 'UNKNOWN', null, '200 GSM/GR', 'UNKNOWN', null, null, null, 200.0, null, true),
        ('PAD RR DOOR SILENCER T5 D 200GR', 'UNKNOWN', null, '200 GSM/GR', 'UNKNOWN', null, null, null, 200.0, null, true),
        ('Plastic Packing70 x 100cm', 'PT. FAM', '1', '1', 'KG', null, null, null, null, 1.0, true),
        ('Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', '1', '1', 'PCS', null, null, null, null, 1.0, true),
        ('PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', '1,5 X 50', 'ROLL', 150.0, 5000.0, null, 100.0, null, true),
        ('PS Polyester Non Woven Spunbond 80 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '0,45 X 300', '0,45 X 300', 'ROLL', 45.0, 30000.0, null, 80.0, null, true),
        ('Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', '1,8 X 50', 'ROLL', 180.0, 5000.0, 2.0, 375.0, null, true),
        ('Recycle Felt GWPS 2mm 375 GSM + Spunbond', 'PT. LANI TEDUH (BTI)', '1,6 X 50', '1,6 X 50', 'ROLL', 160.0, 5000.0, null, 375.0, null, true),
        ('Recycle Felt GWPS 4mm 450 GSM', 'PT. LANI TEDUH (BTI)', '1,4 X 30', '1,4 X 30', 'ROLL', 140.0, 3000.0, null, 450.0, null, true),
        ('Recycle Felt GWPS 5mm 1000 GSM', 'PT. LANI TEDUH (BTI)', '1,3 X 25', '1,3 X 25', 'ROLL', 130.0, 2500.0, null, 1000.0, null, true),
        ('Silencer T. 10mm 500 GSM', 'PT. SUPERBTEX', '1 X 20', '1 X 20', 'ROLL', 100.0, 2000.0, 10.0, 500.0, null, true),
        ('Silencer T. 15mm 1000 GSM', 'PT. SUPERBTEX', '1 X 20', '1 X 20', 'ROLL', 100.0, 2000.0, 15.0, 1000.0, null, true),
        ('Silencer T. 6mm 350 GSM', 'PT. SUPERBTEX', '1 X 20', '1 X 20', 'ROLL', 100.0, 2000.0, 6.0, 350.0, null, true),
        ('Spunbond Black 50gsm 50 x 500 MTR', 'PT. TRIMITRA SWADAYA', '1', '1', 'MTR', null, null, null, 50.0, 1.0, true);

create temporary table seed_part (
    uniq_no text,
    part_no text,
    nama_part text,
    model text,
    customer text,
    komoditas text,
    lokasi_gambar text,
    aktif boolean,
    catatan_seed text
) on commit drop;

insert into seed_part values
        ('CB9', '58815-KK010-00', 'CARPET CONSOLE BOX', '660A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('2529', '58815-KK010-00', 'CARPET CONSOLE BOX (EXP)', 'EXP', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('CB3', '58612-A1016', 'PAD FR DOOR SILENCER', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('ER2', '67812-X7A07', 'PAD FR DOOR SILENCER', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('MM1', '58611-A1011', 'INSULATION SHEET NO. 1', '660A/650', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('ML0', '58611-A1012', 'INSULATION SHEET NO. 2', '660A/650', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('ML1', '58611-A1013', 'INSULATION SHEET NO. 3', 'FMC', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('MO2', '58611-A1014', 'INSULATION SHEET NO. 4/FT7', 'EXP', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('MN9', '58611-A1015', 'INSULATION SHEET NO. 5/FT8', 'EXP', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('EQ5', '79117-0K060-A', 'CARPET NO. 1 SEAT CUSHION', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('DN5', '11115-A1012', 'FELT FRONT BACK RH ( LOW- G', '660A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BY4', '12115-A1012', 'FELT FRONT BACK LH (LOW - G)', '660A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FP7', '11127-A1042', 'FELT FR RH', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FP8', '12127-A1042', 'FELT FR LH', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FP9', '13115-A1042', 'FELT ( 40%)', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FQ0', '14137-A1044', 'FELT 60%', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('JS5', '11123-A1062', 'FELT', '230/231B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('JY8', '12127-A2899', 'FELT LH T2.5 4009 + LDPE 200gr', '230/231B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('JZ2', '11127-A2899', 'FELT LH T2.5 4009 + LDPE 200gr', '230/231B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('JP6', '79976-X1V86-00', 'FELT', '230/231B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FJ0', '71075-F1V01', 'PAD SETTEN RH', 'D14N', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FJ1', '71075-F1V02', 'PAD SETTEN LH', 'D14N', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FJ2', '71075-F1V03', 'USHIRO RH', 'D14N', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FJ3', '71075-F1V04', 'USHIRO LH', 'D14N', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FJ4', '71075-F1V05', 'BOTTOM RH', 'D14N', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FJ5', '71075-F1V06', 'BOTTOM LH', 'D14N', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FJ6', '71075-F1V07', 'KOTAK USHIRO', 'D14N', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FE7', '71781-X7A35-A', 'STRAP SEAT COVER', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('FE8', '71781-X7A36', 'STRAP SEAT COVER', '650A', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('JQ9', '67811-X1V82', 'PAD FR DOOR SILENCER T5 D 200GR', '230/231B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('JR0', '67812-X1V59', 'PAD RR DOOR SILENCER T5 D 200GR', '230/231B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('JR1', '67812-X1V60', 'PAD FR DOOR SILENCER T5 D 200GR', '230/231B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BJ1', '79977-BZ020', 'FELT SEAT BACK', 'D26A', 'BONECOM TRICOM', 'SEWING', null, true, 'PARTLIST_16_BTI'),
        ('B35', '71695-VT070-C', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null, true, 'PARTLIST_16_BTI'),
        ('B63', '71695-VT080', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null, true, 'PARTLIST_16_BTI'),
        ('B51', '71695-VT090-B', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null, true, 'PARTLIST_16_BTI'),
        ('B70', '71695-VT100-B', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null, true, 'PARTLIST_16_BTI'),
        ('B55', '71695-VT110-D', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null, true, 'PARTLIST_16_BTI'),
        ('B72', '71695-VT120-D', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null, true, 'PARTLIST_16_BTI'),
        ('BT138', '71781-X7H01-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT139', '71781-X7H02-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT140', '71781-X7H03-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT141', '71781-X7H04-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT136', '11101-A1211-00', 'FELT FRONT BACK RH', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT137', '11102-A1211-00', 'FELT FRONT BACK RH', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT144', '12101-A1211-00', 'FELT FRONT BACK LH', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT18', '72996-X7H00', 'CARPET RR SEAT NO. 2 RH', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT19', '71997-X7H00', 'CARPET RR SEAT NO. 2 LH', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT200', '13119-A1212', 'FELT BENCH 6:4', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT201', '13120-A1212', 'FELT 60%', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT202', '13121-A1212', 'FELT 40%', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT203', '13122-A1212', 'FELT 24*188', '560B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BM7', '71651-BZ020', 'PAD RR SEAT BACK RH', 'GSK', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BM8', '71652-BZ020', 'PAD RR SEAT BACK LH', 'GSK', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('CL1', '7997A-X7V05', 'CLAF 1', 'D74', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('CL2', '7997A-X7V04', 'CLAF 2', 'D74', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT262', '71782-X7U06', 'STRAP SEAT COVER', 'D03B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT263', '71782-X7U07', 'STRAP SEAT COVER', 'D03B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT264', '71782-X7U08', 'STRAP SEAT COVER', 'D03B', 'BONECOM TRICOM', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('BT332', '78253-X7U00', 'PAD RR SEAT CTR ARMREST (ARTHA)', 'D03B', 'BONECOM TRICOM', 'PASS_THROUGH', null, true, 'PARTLIST_16_BTI'),
        ('9Y6', '11119-A9310-A', 'FELT AFTER LAMINATING', '660A/650', 'BONECOM TRICOM', 'MATERIAL', null, true, 'PARTLIST_16_BTI'),
        ('ID-121', '58815-RAW00', 'CARPET CB-III CONSOLE BOX', '660A/650', 'BONECOM TRICOM', 'MATERIAL', null, true, 'PARTLIST_16_BTI'),
        ('CH8', '71518-X7H01', 'CLOTH SEAT CUSH, UNDER', '560B', 'RAJAWALI MITRA PRATAMA', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('B50', 'RIM01-5601B', 'FELT CLOTH 560B', '560B', 'RAVALIA INTI MANDIRI', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('P56', 'RIM01-5602B', 'STRAP 560B', '560B', 'RAVALIA INTI MANDIRI', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('RM5', '71518-X7U19', 'CLOTH FR SEAT CUSH UNDER', 'D03B', 'RAJAWALI MITRA PRATAMA', 'PRESS', null, true, 'PARTLIST_16_BTI'),
        ('CH9', '57902-T0024', 'QUEENSCORD', '660', 'BONECOM TRICOM', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('EE5', 'HD004-W2200', 'PLASTIC HD', '800A', 'BONECOM TRICOM', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('A47', 'K9905-K0001', 'SPUNDBOND 75 GSM BLACK', 'IMV', 'BONECOM TRICOM', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('Y21', 'AD002-A0069', 'APRON DADA BIRU', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('4Q7', 'JB001-A0075', 'MASKER JILBAB', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('FS2', 'AD003-REC01', 'APRON DADA RECLEANING', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('FS3', 'LAK02-BLACK', 'LAKBAN KAIN 2" BLACK', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('EE8', 'PMWHT-STSG3', 'PAINT MARKING SHACHIHATA WHITE STSG 3', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('EE9', 'THSOL-00331', 'SOLVEN SHACHIHATA 5DL-3-31', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('Y22', 'AK001-A0070', 'APRON KAKI', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('3Z0', 'CUTER-D6765', 'CUTTER KERTAS SDI 6765', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('Y20', 'AT002-A0068', 'APRON TANGAN BIRU', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null, true, 'PARTLIST_16_BTI'),
        ('BY18', null, 'BY18', 'SEWING', null, 'SEWING', null, false, 'PARTLIST_16_RELATION_REVIEW'),
        ('BY19', null, 'BY19', 'SEWING', null, 'SEWING', null, false, 'PARTLIST_16_RELATION_REVIEW');

create temporary table seed_part_material (
    uniq_no text,
    nama_material text,
    nama_supplier text,
    spec_ringkas text,
    urutan int,
    label_material text,
    qty_per_part numeric,
    aktif boolean
) on commit drop;

insert into seed_part_material values
        ('BJ1', 'Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 1, 'Recycle Felt GWPS 2mm 375 GSM', null, true),
        ('BJ1', 'PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 2, 'PS Polyester Non Woven Spunbond 100 GSM White', null, true),
        ('BJ1', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 3, 'Laminasi LDPE 200 GSM', null, true),
        ('BJ1', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 4, 'Laminasi LDPE 200 GSM', null, true),
        ('BJ1', 'Carpet Assy Needle D26', 'PT. FAM', '1', 5, 'Carpet Assy Needle D26', 1, true),
        ('BT200', 'FELT BENCH 6:4', 'UNKNOWN', null, 1, 'FELT BENCH 6:4', null, true),
        ('B35', 'Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 1, 'Recycle Felt GWPS 2mm 375 GSM', null, true),
        ('B35', 'PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 2, 'PS Polyester Non Woven Spunbond 100 GSM White', null, true),
        ('B35', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 3, 'Laminasi LDPE 200 GSM', null, true),
        ('B35', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 4, 'Laminasi LDPE 200 GSM', null, true),
        ('B35', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', 5, 'Indication Tag Tafeta Felt PE , PET', 1, true),
        ('B35', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', 6, 'Hook seat Cover 72752-X1V08', 1, true),
        ('B35', 'Benang Black #60', 'PT. IEM', '1', 7, 'Benang Black #60', 1, true),
        ('BT202', 'FELT 40%', 'UNKNOWN', null, 1, 'FELT 40%', null, true),
        ('B63', 'Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 1, 'Recycle Felt GWPS 2mm 375 GSM', null, true),
        ('B63', 'PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 2, 'PS Polyester Non Woven Spunbond 100 GSM White', null, true),
        ('B63', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 3, 'Laminasi LDPE 200 GSM', null, true),
        ('B63', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 4, 'Laminasi LDPE 200 GSM', null, true),
        ('B63', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', 5, 'Indication Tag Tafeta Felt PE , PET', 1, true),
        ('B63', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', 6, 'Hook seat Cover 72752-X1V08', 1, true),
        ('B63', 'Benang Black #60', 'PT. IEM', '1', 7, 'Benang Black #60', 1, true),
        ('BT203', 'FELT 24*188', 'UNKNOWN', null, 1, 'FELT 24*188', null, true),
        ('B55', 'Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 1, 'Recycle Felt GWPS 2mm 375 GSM', null, true),
        ('B55', 'PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 2, 'PS Polyester Non Woven Spunbond 100 GSM White', null, true),
        ('B55', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 3, 'Laminasi LDPE 200 GSM', null, true),
        ('B55', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 4, 'Laminasi LDPE 200 GSM', null, true),
        ('B55', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', 5, 'Indication Tag Tafeta Felt PE , PET', 1, true),
        ('B55', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', 6, 'Hook seat Cover 72752-X1V08', 1, true),
        ('B55', 'Benang Black #60', 'PT. IEM', '1', 7, 'Benang Black #60', 1, true),
        ('FJ1', 'PAD SETTEN LH', 'UNKNOWN', null, 1, 'PAD SETTEN LH', null, true),
        ('B72', 'Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 1, 'Recycle Felt GWPS 2mm 375 GSM', null, true),
        ('B72', 'PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 2, 'PS Polyester Non Woven Spunbond 100 GSM White', null, true),
        ('B72', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 3, 'Laminasi LDPE 200 GSM', null, true),
        ('B72', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 4, 'Laminasi LDPE 200 GSM', null, true),
        ('B72', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', 5, 'Indication Tag Tafeta Felt PE , PET', 1, true),
        ('B72', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', 6, 'Hook seat Cover 72752-X1V08', 1, true),
        ('B72', 'Benang Black #60', 'PT. IEM', '1', 7, 'Benang Black #60', 1, true),
        ('FJ0', 'PAD SETTEN RH', 'UNKNOWN', null, 1, 'PAD SETTEN RH', null, true),
        ('B51', 'Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 1, 'Recycle Felt GWPS 2mm 375 GSM', null, true),
        ('B51', 'PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 2, 'PS Polyester Non Woven Spunbond 100 GSM White', null, true),
        ('B51', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 3, 'Laminasi LDPE 200 GSM', null, true),
        ('B51', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 4, 'Laminasi LDPE 200 GSM', null, true),
        ('B51', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', 5, 'Indication Tag Tafeta Felt PE , PET', 1, true),
        ('B51', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', 6, 'Hook seat Cover 72752-X1V08', 1, true),
        ('B51', 'Benang Black #60', 'PT. IEM', '1', 7, 'Benang Black #60', 1, true),
        ('BT136', 'FELT FRONT BACK RH', 'UNKNOWN', null, 1, 'FELT FRONT BACK RH', null, true),
        ('B70', 'Recycle Felt GWPS 2mm 375 GSM', 'PT. LANI TEDUH (BTI)', '1,8 X 50', 1, 'Recycle Felt GWPS 2mm 375 GSM', null, true),
        ('B70', 'PS Polyester Non Woven Spunbond 100 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '1,5 X 50', 2, 'PS Polyester Non Woven Spunbond 100 GSM White', null, true),
        ('B70', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '0,66 X 50', 3, 'Laminasi LDPE 200 GSM', null, true),
        ('B70', 'Laminasi LDPE 200 GSM', 'PT. ARTHA LANGGENG MULYA (BTI)', '1,14 X 50', 4, 'Laminasi LDPE 200 GSM', null, true),
        ('B70', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', '1', 5, 'Indication Tag Tafeta Felt PE , PET', 1, true),
        ('B70', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', '1', 6, 'Hook seat Cover 72752-X1V08', 1, true),
        ('B70', 'Benang Black #60', 'PT. IEM', '1', 7, 'Benang Black #60', 1, true),
        ('BT137', 'FELT FRONT BACK RH', 'UNKNOWN', null, 1, 'FELT FRONT BACK RH', null, true),
        ('BY18', 'Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', '1,45 X 40', 1, 'Carpet STKD19 Black', null, true),
        ('BY18', 'Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', '1', 2, 'Plate Seat Cover', 1, true),
        ('BY18', 'Benang Black #60', 'PT. IEM', '1', 3, 'Benang Black #60', 1, true),
        ('BT144', 'FELT FRONT BACK LH', 'UNKNOWN', null, 1, 'FELT FRONT BACK LH', null, true),
        ('BY19', 'Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', '1,45 X 40', 1, 'Carpet STKD19 Black', null, true),
        ('BY19', 'Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', '1', 2, 'Plate Seat Cover', 1, true),
        ('BY19', 'Benang Black #60', 'PT. IEM', '1', 3, 'Benang Black #60', 1, true),
        ('BM7', 'PAD RR SEAT BACK RH', 'UNKNOWN', null, 1, 'PAD RR SEAT BACK RH', null, true),
        ('BM8', 'PAD RR SEAT BACK LH', 'UNKNOWN', null, 1, 'PAD RR SEAT BACK LH', null, true),
        ('FP7', 'FELT FR RH', 'UNKNOWN', null, 1, 'FELT FR RH', null, true),
        ('FP8', 'FELT FR LH', 'UNKNOWN', null, 1, 'FELT FR LH', null, true),
        ('FP9', 'FELT ( 40%)', 'UNKNOWN', null, 1, 'FELT ( 40%)', null, true),
        ('FQ0', 'FELT 60%', 'UNKNOWN', null, 1, 'FELT 60%', null, true),
        ('DN5', 'FELT FRONT BACK RH ( LOW- G', 'UNKNOWN', null, 1, 'FELT FRONT BACK RH ( LOW- G', null, true),
        ('BY4', 'FELT FRONT BACK LH (LOW - G)', 'UNKNOWN', null, 1, 'FELT FRONT BACK LH (LOW - G)', null, true),
        ('JS5', 'FELT', 'UNKNOWN', null, 1, 'FELT', null, true),
        ('BT138', 'STRAP, SEAT COVER', 'UNKNOWN', null, 1, 'STRAP, SEAT COVER', null, true),
        ('BT139', 'STRAP, SEAT COVER', 'UNKNOWN', null, 1, 'STRAP, SEAT COVER', null, true),
        ('BT140', 'STRAP, SEAT COVER', 'UNKNOWN', null, 1, 'STRAP, SEAT COVER', null, true),
        ('BT141', 'STRAP, SEAT COVER', 'UNKNOWN', null, 1, 'STRAP, SEAT COVER', null, true),
        ('CH8', 'CLOTH SEAT CUSH, UNDER', 'UNKNOWN', null, 1, 'CLOTH SEAT CUSH, UNDER', null, true),
        ('RM5', 'CLOTH FR SEAT CUSH UNDER', 'UNKNOWN', null, 1, 'CLOTH FR SEAT CUSH UNDER', null, true),
        ('BT262', 'STRAP SEAT COVER', 'UNKNOWN', null, 1, 'STRAP SEAT COVER', null, true),
        ('BT263', 'STRAP SEAT COVER', 'UNKNOWN', null, 1, 'STRAP SEAT COVER', null, true),
        ('BT264', 'STRAP SEAT COVER', 'UNKNOWN', null, 1, 'STRAP SEAT COVER', null, true),
        ('MM1', 'EPDM 45mm', 'PT. KOMKAR', '0,45 X 25', 1, 'EPDM 45mm', null, true),
        ('MM1', 'EPDM 47mm', 'PT. KOMKAR', '0,47 X 25', 2, 'EPDM 47mm', null, true),
        ('MM1', 'PS Polyester Non Woven Spunbond 80 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '0,45 X 300', 3, 'PS Polyester Non Woven Spunbond 80 GSM White', null, true),
        ('MM1', 'Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', '2,5 KG', 4, 'Lem Fox 2,5 Kg', 2.5, true),
        ('ML0', 'EPDM 45mm', 'PT. KOMKAR', '0,45 X 25', 1, 'EPDM 45mm', null, true),
        ('ML0', 'EPDM 47mm', 'PT. KOMKAR', '0,47 X 25', 2, 'EPDM 47mm', null, true),
        ('ML0', 'PS Polyester Non Woven Spunbond 80 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '0,45 X 300', 3, 'PS Polyester Non Woven Spunbond 80 GSM White', null, true),
        ('ML0', 'Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', '2,5 KG', 4, 'Lem Fox 2,5 Kg', 2.5, true),
        ('ML1', 'EPDM 45mm', 'PT. KOMKAR', '0,45 X 25', 1, 'EPDM 45mm', null, true),
        ('ML1', 'EPDM 47mm', 'PT. KOMKAR', '0,47 X 25', 2, 'EPDM 47mm', null, true),
        ('ML1', 'PS Polyester Non Woven Spunbond 80 GSM White', 'PT. HASIL DAMAI TEXTILE (BTI)', '0,45 X 300', 3, 'PS Polyester Non Woven Spunbond 80 GSM White', null, true),
        ('ML1', 'Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', '2,5 KG', 4, 'Lem Fox 2,5 Kg', 2.5, true),
        ('MO2', 'INSULATION SHEET NO. 4/FT7', 'UNKNOWN', null, 1, 'INSULATION SHEET NO. 4/FT7', null, true),
        ('MN9', 'INSULATION SHEET NO. 5/FT8', 'UNKNOWN', null, 1, 'INSULATION SHEET NO. 5/FT8', null, true),
        ('ER2', 'PAD FR DOOR SILENCER', 'UNKNOWN', null, 1, 'PAD FR DOOR SILENCER', null, true),
        ('CB3', 'PAD FR DOOR SILENCER', 'UNKNOWN', null, 1, 'PAD FR DOOR SILENCER', null, true),
        ('CB9', 'CARPET CONSOLE BOX', 'UNKNOWN', null, 1, 'CARPET CONSOLE BOX', null, true),
        ('2529', 'CARPET CONSOLE BOX (EXP)', 'UNKNOWN', null, 1, 'CARPET CONSOLE BOX (EXP)', null, true),
        ('B50', 'FELT CLOTH 560B', 'UNKNOWN', null, 1, 'FELT CLOTH 560B', null, true),
        ('JR0', 'PAD RR DOOR SILENCER T5 D 200GR', 'UNKNOWN', null, 1, 'PAD RR DOOR SILENCER T5 D 200GR', null, true),
        ('JR1', 'PAD FR DOOR SILENCER T5 D 200GR', 'UNKNOWN', null, 1, 'PAD FR DOOR SILENCER T5 D 200GR', null, true),
        ('JQ9', 'PAD FR DOOR SILENCER T5 D 200GR', 'UNKNOWN', null, 1, 'PAD FR DOOR SILENCER T5 D 200GR', null, true),
        ('EQ5', 'CARPET NO. 1 SEAT CUSHION', 'UNKNOWN', null, 1, 'CARPET NO. 1 SEAT CUSHION', null, true),
        ('CL1', 'CLAF 1', 'UNKNOWN', null, 1, 'CLAF 1', null, true),
        ('CL2', 'CLAF 2', 'UNKNOWN', null, 1, 'CLAF 2', null, true);

create temporary table seed_defect (
    id_defect text,
    nama_defect text,
    kategori text,
    proses_default text,
    satuan_input text,
    metode_pengukuran text,
    aktif boolean
) on commit drop;

insert into seed_defect values
        ('ALUR_SERAT_TIDAK_SESUAI', 'Alur Serat Tidak Sesuai', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('BELANG', 'Belang', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('BERJAMUR', 'Berjamur', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('BERLUBANG', 'Berlubang', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('BRUDUL', 'Brudul', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('CACAT_MATERIAL', 'Cacat Material', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('DENT', 'Dent', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('DIMENSI_TIDAK_STANDAR', 'Dimensi Tidak Standar', 'PROSES', 'PRESS', 'CM', 'LENGTH_CM', true),
        ('GALER', 'Galer', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('HOLE_T_A', 'Hole T/A', 'PROSES', 'PRESS', 'PCS', 'COUNT', true),
        ('KOTOR', 'Kotor', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('KUNCIAN_JEBOL', 'Kuncian Jebol', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('LAMINATING_BOLONG', 'Laminating Bolong', 'MATERIAL', null, 'CM', 'LENGTH_CM', true),
        ('LAMINATING_TIDAK_MATANG', 'Laminating Tidak Matang', 'MATERIAL', null, 'CM', 'LENGTH_CM', true),
        ('LANGKAH_SEWING_TIDAK_SESUAI', 'Langkah Sewing Tidak Sesuai', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('MENGEMBANG', 'Mengembang', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('OVER_CUTTING', 'Over Cutting', 'PROSES', 'CUTTING', 'CM', 'LENGTH_CM', true),
        ('SEWING_LONCAT', 'Sewing Loncat', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('SEWING_LONGGAR', 'Sewing Longgar', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('SEWING_MIRING', 'Sewing Miring', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('SEWING_NITIK', 'Sewing Nitik', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('SEWING_PUTUS', 'Sewing Putus', 'PROSES', 'SEWING', 'PCS', 'COUNT', true),
        ('SOBEK', 'Sobek', 'MATERIAL', null, 'CM', 'LENGTH_CM', true),
        ('SPUNBOND_KOTOR', 'Spunbond Kotor', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('SPUNBOND_MENGERAS', 'Spunbond Mengeras', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('SPUNBOND_TERLIPAT', 'Spunbond Terlipat', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('SPUNBOND_TIDAK_MEREKAT', 'Spunbond Tidak Merekat', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('TERBALIK', 'Terbalik', 'PROSES', 'PRESS', 'PCS', 'COUNT', true),
        ('TERLIPAT', 'Terlipat', 'MATERIAL', null, 'PCS', 'COUNT', true),
        ('TIPIS', 'Tipis', 'MATERIAL', null, 'PCS', 'COUNT', true);

create temporary table seed_material_defect (
    nama_material text,
    id_defect text,
    urutan int,
    aktif boolean
) on commit drop;

insert into seed_material_defect values
        ('Carpet CB-III', 'BELANG', 1, true),
        ('Carpet CB-III', 'BERJAMUR', 2, true),
        ('Carpet CB-III', 'BRUDUL', 3, true),
        ('Carpet CB-III', 'DENT', 4, true),
        ('Carpet CB-III', 'GALER', 5, true),
        ('Carpet CB-III', 'SOBEK', 6, true),
        ('Carpet CB-III', 'TERLIPAT', 7, true),
        ('Carpet CB-III', 'TIPIS', 8, true),
        ('Carpet STKD19 Black', 'BELANG', 9, true),
        ('Carpet STKD19 Black', 'BERJAMUR', 10, true),
        ('Carpet STKD19 Black', 'BRUDUL', 11, true),
        ('Carpet STKD19 Black', 'DENT', 12, true),
        ('Carpet STKD19 Black', 'GALER', 13, true),
        ('Carpet STKD19 Black', 'SOBEK', 14, true),
        ('Carpet STKD19 Black', 'TERLIPAT', 15, true),
        ('Carpet STKD19 Black', 'TIPIS', 16, true),
        ('EPDM 45mm', 'BERLUBANG', 17, true),
        ('EPDM 45mm', 'TIPIS', 18, true),
        ('EPDM 47mm', 'BERLUBANG', 19, true),
        ('EPDM 47mm', 'TIPIS', 20, true),
        ('Ester Canvas SAB10-NS121 SSP', 'BRUDUL', 21, true),
        ('Ester Canvas SAB10-NS121 SSP', 'KOTOR', 22, true),
        ('Ester Canvas SAB10-NS121 SSP', 'SOBEK', 23, true),
        ('Laminasi LDPE 200 GSM', 'LAMINATING_BOLONG', 24, true),
        ('Laminasi LDPE 200 GSM', 'LAMINATING_TIDAK_MATANG', 25, true),
        ('PS Polyester Non Woven Spunbond 100 GSM White', 'SPUNBOND_KOTOR', 26, true),
        ('PS Polyester Non Woven Spunbond 100 GSM White', 'SPUNBOND_MENGERAS', 27, true),
        ('PS Polyester Non Woven Spunbond 100 GSM White', 'SPUNBOND_TERLIPAT', 28, true),
        ('PS Polyester Non Woven Spunbond 100 GSM White', 'SPUNBOND_TIDAK_MEREKAT', 29, true),
        ('Recycle Felt GWPS 2mm 375 GSM', 'BRUDUL', 30, true),
        ('Recycle Felt GWPS 2mm 375 GSM', 'SOBEK', 31, true),
        ('Recycle Felt GWPS 2mm 375 GSM', 'TIPIS', 32, true),
        ('Silencer T. 15mm 1000 GSM', 'BRUDUL', 33, true),
        ('Silencer T. 15mm 1000 GSM', 'KOTOR', 34, true),
        ('Silencer T. 15mm 1000 GSM', 'MENGEMBANG', 35, true),
        ('Silencer T. 15mm 1000 GSM', 'SOBEK', 36, true),
        ('Silencer T. 6mm 350 GSM', 'BRUDUL', 37, true),
        ('Silencer T. 6mm 350 GSM', 'KOTOR', 38, true),
        ('Silencer T. 6mm 350 GSM', 'MENGEMBANG', 39, true),
        ('Silencer T. 6mm 350 GSM', 'SOBEK', 40, true);

create temporary table seed_material_komposisi (
    parent_material text,
    child_material text,
    peran_material text,
    urutan int,
    aktif boolean
) on commit drop;

insert into seed_material_komposisi values
        ('Protector', 'Recycle Felt GWPS 2mm 375 GSM', 'LAPISAN_UTAMA', 1, true),
        ('Protector', 'PS Polyester Non Woven Spunbond 100 GSM White', 'LAPISAN_SPUNBOND', 2, true),
        ('Protector', 'Laminasi LDPE 200 GSM', 'LAMINASI', 3, true),
        ('Protector', 'Indication Tag Tafeta Felt PE , PET', 'LABEL_TAG', 4, true),
        ('Protector', 'Hook seat Cover 72752-X1V08', 'HOOK', 5, true),
        ('Protector', 'Benang Black #60', 'BENANG', 6, true),
        ('Recycle Felt GWPS 2mm 375 GSM + Spunbond', 'Recycle Felt GWPS 2mm 375 GSM', 'LAPISAN_UTAMA', 1, true),
        ('Recycle Felt GWPS 2mm 375 GSM + Spunbond', 'PS Polyester Non Woven Spunbond 100 GSM White', 'LAPISAN_SPUNBOND', 2, true),
        ('Laminasi LDPE 200 GSM + SPB White', 'Laminasi LDPE 200 GSM', 'LAMINASI', 1, true),
        ('Laminasi LDPE 200 GSM + SPB White', 'PS Polyester Non Woven Spunbond 100 GSM White', 'LAPISAN_SPUNBOND', 2, true),
        ('Hardfelt (9Y6)', 'Recycle Felt GWPS 4mm 450 GSM', 'BAHAN_DASAR', 1, true),
        ('Fujiseat Hardfelt (9Y8)', 'Recycle Felt GWPS 2mm 375 GSM', 'BAHAN_DASAR', 1, true),
        ('Fujiseat Hardfelt (9Y8)', 'Laminasi LDPE 200 GSM', 'LAMINASI', 2, true);

create temporary table seed_part_cutting_size (
    uniq_no text,
    size_cutting_cm numeric,
    nama_material_sumber text,
    part_no_sumber text,
    project_sumber text,
    urutan int,
    aktif boolean
) on commit drop;

insert into seed_part_cutting_size values
        ('BT136', 76.0, 'Fujiseat Hardfelt (9Y8)', '11101-A1211', '560B', 1, true),
        ('BT137', 76.0, 'Fujiseat Hardfelt (9Y8)', '11102-A1211', '560B', 2, true),
        ('BT144', 76.0, 'Fujiseat Hardfelt (9Y8)', '12101-A1211', '560B', 3, true),
        ('B50', 50.0, 'Carpet STKD19 Black', 'RIM01-5601B', '560B', 4, true),
        ('CL7', 81.5, 'Carpet STKD19 Black', '71997-X7H00', '560B', 5, true),
        ('CR6', 81.5, 'Carpet STKD19 Black', '72996-X7H00', '560B', 6, true),
        ('BT138', 39.0, 'Ester Canvas Strap', '71781-X7H01', '560B', 7, true),
        ('BT139', 27.0, 'Ester Canvas Strap', '71781-X7H02', '560B', 8, true),
        ('BT140', 40.0, 'Ester Canvas Strap', '71781-X7H03', '560B', 9, true),
        ('BT141', 24.5, 'Ester Canvas Strap', '71781-X7H04', '560B', 10, true),
        ('CH8', 31.0, 'Ester Canvas Strap', '71518-X7H01', '560B', 11, true),
        ('P56', 29.0, 'Ester Canvas Strap', 'RIM01-5602B', '560B', 12, true),
        ('B35', 109.0, 'Protector', '71695-VT070', '560B', 13, true),
        ('B63', 95.0, 'Protector', '71695-VT080', '560B', 14, true),
        ('B51', 82.0, 'Protector', '71695-VT090', '560B', 15, true),
        ('B55', 82.0, 'Protector', '71695-VT110', '560B', 16, true),
        ('B70', 82.0, 'Protector', '71695-VT100', '560B', 17, true),
        ('B72', 82.0, 'Protector', '71695-VT120', '560B', 18, true),
        ('EQ5', 36.0, 'Black Spunbond 50 GSM', '79117-0K060-A', '650/660A', 19, true),
        ('CB9', 63.0, 'Carpet CB-III', '58815-KK010', '650/660A', 20, true),
        ('FE7', 60.0, 'Ester Canvas Strap', '71781-X7A35-A', '650/660A', 21, true),
        ('FE8', 55.0, 'Ester Canvas Strap', '71781-X7A36', '650/660A', 22, true),
        ('BY4', 77.0, 'Hardfelt (9Y6)', '12115-A1012', '650/660A', 23, true),
        ('DN5', 77.0, 'Hardfelt (9Y6)', '11115-A1012', '650/660A', 24, true),
        ('FP7', 68.5, 'Hardfelt (9Y6)', '11127-A1042', '650/660A', 25, true),
        ('FP8', 68.5, 'Hardfelt (9Y6)', '12127-A1042', '650/660A', 26, true),
        ('FP9', 45.0, 'Hardfelt (9Y6)', '13115-A1042', '650/660A', 27, true),
        ('FQ0', 68.5, 'Hardfelt (9Y6)', '14137-A1044', '650/660A', 28, true),
        ('JY8', 68.5, 'Hardfelt (9Y6)', '12127-A2899', '650/660A', 29, true),
        ('JZ2', 68.5, 'Hardfelt (9Y6)', '11127-A2899', '650/660A', 30, true),
        ('JS5', 70.0, 'Hardfelt (9Y6)', '11123-A1062', '650/660A', 31, true),
        ('CB3', 55.0, 'Silencer T. 15mm 1000 GSM', '58612-A1016', '650/660A', 32, true),
        ('ER2', 55.0, 'Silencer T. 15mm 1000 GSM', '58612-A1020', '650/660A', 33, true),
        ('BT262', 40.0, 'Ester Canvas Strap', '71782-X7U06-A', 'D03', 34, true),
        ('BT263', 34.0, 'Ester Canvas Strap', '71782-X7U07-A', 'D03', 35, true),
        ('BT264', 34.0, 'Ester Canvas Strap', '71782-X7U08-A', 'D03', 36, true),
        ('RM5', 31.0, 'Ester Canvas Strap', '71518-X7U19', 'D03', 37, true),
        ('FJ0', 76.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V01', 'D14N', 39, true),
        ('FJ1', 55.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V02', 'D14N', 40, true),
        ('FJ4', 76.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V05', 'D14N', 43, true),
        ('FJ5', 76.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V06', 'D14N', 44, true),
        ('BM7', 76.0, 'Fujiseat Hardfelt (9Y8)', '71651-BZ020', 'D25', 46, true),
        ('BM8', 76.0, 'Fujiseat Hardfelt (9Y8)', '71652-BZ020', 'D25', 47, true),
        ('CL1', 65.0, 'Nisseki Claff', '7997A-X7U05', 'D74', 48, true),
        ('CL2', 85.0, 'Nisseki Claff', '7997A-X7U04', 'D74', 49, true),
        ('BJ0', 38.5, 'Carpet black 200GSM+Latex 50GSM (MWSB-87)', '71831-BZ150-J', 'DCWA / D72A', 50, true),
        ('BJ1', 50.0, 'Protector', '79977-BZO20', 'D26A', 51, true);

create temporary table seed_slot_waktu (
    kode_slot text,
    tipe_proses text,
    nama_shift text,
    label_waktu text,
    jam_mulai time,
    jam_selesai time,
    urutan int,
    aktif boolean
) on commit drop;

insert into seed_slot_waktu values
        ('PRESS_S1_SLOT_1', 'PRESS', 'SHIFT_1', '08:00 - 09:00', '08:00', '09:00', 1, true),
        ('PRESS_S1_SLOT_2', 'PRESS', 'SHIFT_1', '09:00 - 10:00', '09:00', '10:00', 2, true),
        ('PRESS_S1_SLOT_3', 'PRESS', 'SHIFT_1', '10:00 - 11:00', '10:00', '11:00', 3, true),
        ('PRESS_S1_SLOT_4', 'PRESS', 'SHIFT_1', '11:00 - 12:00', '11:00', '12:00', 4, true),
        ('PRESS_S1_SLOT_5', 'PRESS', 'SHIFT_1', '13:00 - 14:00', '13:00', '14:00', 5, true),
        ('PRESS_S1_SLOT_6', 'PRESS', 'SHIFT_1', '14:00 - 15:00', '14:00', '15:00', 6, true),
        ('PRESS_S1_SLOT_7', 'PRESS', 'SHIFT_1', '15:00 - 16:00', '15:00', '16:00', 7, true),
        ('PRESS_S1_SLOT_8', 'PRESS', 'SHIFT_1', '16:00 - 17:00', '16:00', '17:00', 8, true),
        ('SEWING_S1_SLOT_1', 'SEWING', 'SHIFT_1', '08:00 - 09:00', '08:00', '09:00', 1, true),
        ('SEWING_S1_SLOT_2', 'SEWING', 'SHIFT_1', '09:00 - 10:00', '09:00', '10:00', 2, true),
        ('SEWING_S1_SLOT_3', 'SEWING', 'SHIFT_1', '10:00 - 11:00', '10:00', '11:00', 3, true),
        ('SEWING_S1_SLOT_4', 'SEWING', 'SHIFT_1', '11:00 - 12:00', '11:00', '12:00', 4, true),
        ('SEWING_S1_SLOT_5', 'SEWING', 'SHIFT_1', '13:00 - 14:00', '13:00', '14:00', 5, true),
        ('SEWING_S1_SLOT_6', 'SEWING', 'SHIFT_1', '14:00 - 15:00', '14:00', '15:00', 6, true),
        ('SEWING_S1_SLOT_7', 'SEWING', 'SHIFT_1', '15:00 - 16:00', '15:00', '16:00', 7, true),
        ('SEWING_S1_SLOT_8', 'SEWING', 'SHIFT_1', '16:00 - 17:00', '16:00', '17:00', 8, true),
        ('CUTTING_S1_SLOT_1', 'CUTTING', 'SHIFT_1', '08:00 - 09:00', '08:00', '09:00', 1, true),
        ('CUTTING_S1_SLOT_2', 'CUTTING', 'SHIFT_1', '09:00 - 10:00', '09:00', '10:00', 2, true),
        ('CUTTING_S1_SLOT_3', 'CUTTING', 'SHIFT_1', '10:00 - 11:00', '10:00', '11:00', 3, true),
        ('CUTTING_S1_SLOT_4', 'CUTTING', 'SHIFT_1', '11:00 - 12:00', '11:00', '12:00', 4, true),
        ('CUTTING_S1_SLOT_5', 'CUTTING', 'SHIFT_1', '13:00 - 14:00', '13:00', '14:00', 5, true),
        ('CUTTING_S1_SLOT_6', 'CUTTING', 'SHIFT_1', '14:00 - 15:00', '14:00', '15:00', 6, true),
        ('CUTTING_S1_SLOT_7', 'CUTTING', 'SHIFT_1', '15:00 - 16:00', '15:00', '16:00', 7, true),
        ('CUTTING_S1_SLOT_8', 'CUTTING', 'SHIFT_1', '16:00 - 17:00', '16:00', '17:00', 8, true);

-- ============================================================================
-- 2. UPSERT SUPPLIER
-- ============================================================================

insert into public.m_supplier (
    kode_supplier,
    nama_supplier,
    kategori,
    aktif
)
select
    kode_supplier,
    nama_supplier,
    kategori,
    aktif
from seed_supplier
on conflict (nama_supplier) do update set
    kode_supplier = excluded.kode_supplier,
    kategori = excluded.kategori,
    aktif = true,
    diperbarui_pada = now();

-- ============================================================================
-- 3. UPSERT MATERIAL + SUPPLIER
-- ============================================================================

insert into public.m_material (
    nama_material,
    supplier,
    supplier_id,
    spec,
    spec_ringkas,
    satuan,
    jenis_material,
    aktif
)
select
    sm.nama_material,
    sm.nama_supplier,
    sup.id,
    sm.spec_ringkas,
    sm.spec_ringkas,
    sm.satuan::public.satuan_inspectra,
    sm.jenis_material,
    sm.aktif
from seed_material sm
join public.m_supplier sup
    on sup.nama_supplier = sm.nama_supplier
on conflict (nama_normalisasi, supplier_id, spec_ringkas) do update set
    supplier = excluded.supplier,
    spec = excluded.spec,
    satuan = excluded.satuan,
    jenis_material = excluded.jenis_material,
    aktif = true,
    diperbarui_pada = now();

insert into public.m_material_supplier (
    material_id,
    supplier_id,
    supplier_material_name,
    is_preferred,
    aktif
)
select
    mat.id,
    sup.id,
    sm.nama_material,
    true,
    true
from seed_material sm
join public.m_supplier sup
    on sup.nama_supplier = sm.nama_supplier
join public.m_material mat
    on mat.nama_material = sm.nama_material
   and mat.supplier_id = sup.id
   and coalesce(mat.spec_ringkas, '') = coalesce(sm.spec_ringkas, '')
on conflict (material_id, supplier_id) do update set
    supplier_material_name = excluded.supplier_material_name,
    is_preferred = true,
    aktif = true,
    diperbarui_pada = now();

-- ============================================================================
-- 4. UPSERT MATERIAL SPEC
-- ============================================================================

insert into public.m_material_spec (
    material_id,
    spec_asli,
    satuan,
    lebar_value,
    lebar_unit,
    panjang_value,
    panjang_unit,
    tebal_value,
    tebal_unit,
    berat_value,
    berat_unit,
    qty_value,
    qty_unit,
    lebar_cm,
    panjang_roll_cm,
    tebal_mm,
    berat_gsm,
    gramasi_gsm,
    qty_default,
    satuan_qty,
    is_default,
    aktif
)
select
    mat.id,
    sms.spec_asli,
    sms.satuan::public.satuan_inspectra,
    sms.lebar_cm,
    'CM',
    sms.panjang_roll_cm,
    'CM',
    sms.tebal_mm,
    'MM',
    sms.berat_gsm,
    'GSM',
    sms.qty_default,
    sms.satuan::public.satuan_inspectra,
    sms.lebar_cm,
    sms.panjang_roll_cm,
    sms.tebal_mm,
    sms.berat_gsm,
    sms.berat_gsm,
    sms.qty_default,
    sms.satuan,
    true,
    true
from seed_material_spec sms
join public.m_supplier sup
    on sup.nama_supplier = sms.nama_supplier
join public.m_material mat
    on mat.nama_material = sms.nama_material
   and mat.supplier_id = sup.id
   and coalesce(mat.spec_ringkas, '') = coalesce(sms.spec_ringkas, '')
on conflict (material_id, spec_asli) do update set
    satuan = excluded.satuan,
    lebar_value = excluded.lebar_value,
    lebar_unit = 'CM',
    panjang_value = excluded.panjang_value,
    panjang_unit = 'CM',
    tebal_value = excluded.tebal_value,
    tebal_unit = 'MM',
    berat_value = excluded.berat_value,
    berat_unit = 'GSM',
    qty_value = excluded.qty_value,
    qty_unit = excluded.qty_unit,
    lebar_cm = excluded.lebar_cm,
    panjang_roll_cm = excluded.panjang_roll_cm,
    tebal_mm = excluded.tebal_mm,
    berat_gsm = excluded.berat_gsm,
    gramasi_gsm = excluded.gramasi_gsm,
    qty_default = excluded.qty_default,
    satuan_qty = excluded.satuan_qty,
    is_default = true,
    aktif = true,
    diperbarui_pada = now();

-- ============================================================================
-- 5. UPSERT PART
-- ============================================================================

insert into public.m_part (
    uniq_no,
    part_no,
    nama_part,
    model,
    customer,
    komoditas,
    lokasi_gambar,
    aktif,
    status_kelengkapan,
    butuh_review,
    catatan_review
)
select
    sp.uniq_no,
    sp.part_no,
    sp.nama_part,
    sp.model,
    sp.customer,
    sp.komoditas::public.tipe_proses_inspectra,
    sp.lokasi_gambar,
    sp.aktif,
    case
        when sp.aktif = false then 'PERLU_REVIEW'
        else 'SIAP_DICEK'
    end,
    sp.aktif = false,
    case
        when sp.aktif = false then 'Data muncul pada relasi material tetapi belum ada pada sheet BTI. Disimpan nonaktif agar tidak muncul di E-Checksheet.'
        else null
    end
from seed_part sp
on conflict (uniq_no) do update set
    part_no = excluded.part_no,
    nama_part = excluded.nama_part,
    model = excluded.model,
    customer = excluded.customer,
    komoditas = excluded.komoditas,
    lokasi_gambar = excluded.lokasi_gambar,
    aktif = excluded.aktif,
    status_kelengkapan = excluded.status_kelengkapan,
    butuh_review = excluded.butuh_review,
    catatan_review = excluded.catatan_review,
    diperbarui_pada = now();

-- Koreksi typo komoditas historis.
update public.m_part
set komoditas = 'PASS_THROUGH',
    diperbarui_pada = now()
where upper(komoditas::text) in ('PASSTROUGH', 'PASS TROUGH', 'PASS-THROUGH');

-- ============================================================================
-- 6. UPSERT PART - MATERIAL
-- ============================================================================

-- Update relasi yang sudah ada.
update public.m_part_material target
set
    urutan = src.urutan,
    label_material = src.label_material,
    qty_per_part = src.qty_per_part,
    wajib_check = true,
    aktif = src.aktif,
    diperbarui_pada = now()
from (
    select
        spm.uniq_no,
        mat.id as material_id,
        ms.id as material_spec_id,
        spm.urutan,
        spm.label_material,
        spm.qty_per_part,
        spm.aktif
    from seed_part_material spm
    join public.m_supplier sup
        on sup.nama_supplier = spm.nama_supplier
    join public.m_material mat
        on mat.nama_material = spm.nama_material
       and mat.supplier_id = sup.id
       and coalesce(mat.spec_ringkas, '') = coalesce(spm.spec_ringkas, '')
    left join public.m_material_spec ms
        on ms.material_id = mat.id
       and coalesce(ms.spec_asli, '') = coalesce(spm.spec_ringkas, '')
    join public.m_part part
        on part.uniq_no = spm.uniq_no
) src
where target.uniq_no = src.uniq_no
  and target.material_id = src.material_id
  and coalesce(target.material_spec_id, '00000000-0000-0000-0000-000000000000'::uuid)
      = coalesce(src.material_spec_id, '00000000-0000-0000-0000-000000000000'::uuid);

-- Insert relasi yang belum ada.
insert into public.m_part_material (
    uniq_no,
    material_id,
    material_spec_id,
    urutan,
    label_material,
    qty_per_part,
    wajib_check,
    aktif
)
select
    src.uniq_no,
    src.material_id,
    src.material_spec_id,
    src.urutan,
    src.label_material,
    src.qty_per_part,
    true,
    src.aktif
from (
    select
        spm.uniq_no,
        mat.id as material_id,
        ms.id as material_spec_id,
        spm.urutan,
        spm.label_material,
        spm.qty_per_part,
        spm.aktif
    from seed_part_material spm
    join public.m_supplier sup
        on sup.nama_supplier = spm.nama_supplier
    join public.m_material mat
        on mat.nama_material = spm.nama_material
       and mat.supplier_id = sup.id
       and coalesce(mat.spec_ringkas, '') = coalesce(spm.spec_ringkas, '')
    left join public.m_material_spec ms
        on ms.material_id = mat.id
       and coalesce(ms.spec_asli, '') = coalesce(spm.spec_ringkas, '')
    join public.m_part part
        on part.uniq_no = spm.uniq_no
) src
where not exists (
    select 1
    from public.m_part_material existing
    where existing.uniq_no = src.uniq_no
      and existing.material_id = src.material_id
      and coalesce(existing.material_spec_id, '00000000-0000-0000-0000-000000000000'::uuid)
          = coalesce(src.material_spec_id, '00000000-0000-0000-0000-000000000000'::uuid)
);

-- ============================================================================
-- 7. UPSERT MATERIAL COMPOSITION
-- ============================================================================

insert into public.m_material_komposisi (
    parent_material_id,
    child_material_id,
    child_material_spec_id,
    peran_material,
    urutan,
    wajib,
    aktif
)
select
    parent.id,
    child.id,
    child_spec.id,
    sk.peran_material,
    sk.urutan,
    true,
    sk.aktif
from seed_material_komposisi sk
join public.m_material parent
    on parent.nama_normalisasi = upper(trim(regexp_replace(sk.parent_material, '\s+', ' ', 'g')))
join public.m_material child
    on child.nama_normalisasi = upper(trim(regexp_replace(sk.child_material, '\s+', ' ', 'g')))
left join public.m_material_spec child_spec
    on child_spec.material_id = child.id
   and child_spec.is_default = true
on conflict do nothing;

-- ============================================================================
-- 8. UPSERT DEFECT MASTER
-- ============================================================================

insert into public.m_defect (
    id_defect,
    nama_defect,
    kategori,
    proses_default,
    satuan_input,
    metode_pengukuran,
    aktif
)
select
    sd.id_defect,
    sd.nama_defect,
    sd.kategori::public.kategori_defect_inspectra,
    sd.proses_default::public.tipe_proses_inspectra,
    sd.satuan_input,
    sd.metode_pengukuran,
    sd.aktif
from seed_defect sd
on conflict (id_defect) do update set
    nama_defect = excluded.nama_defect,
    kategori = excluded.kategori,
    proses_default = excluded.proses_default,
    satuan_input = excluded.satuan_input,
    metode_pengukuran = excluded.metode_pengukuran,
    aktif = true,
    diperbarui_pada = now();

-- ============================================================================
-- 9. UPSERT MATERIAL - DEFECT
-- ============================================================================

insert into public.m_material_defect (
    material_id,
    id_defect,
    urutan,
    wajib_check,
    proses_scope,
    satuan_input,
    metode_pengukuran,
    aktif
)
select
    mat.id,
    smd.id_defect,
    smd.urutan,
    true,
    'ALL',
    defect.satuan_input,
    defect.metode_pengukuran,
    smd.aktif
from seed_material_defect smd
join public.m_material mat
    on mat.nama_normalisasi = upper(trim(regexp_replace(smd.nama_material, '\s+', ' ', 'g')))
join public.m_defect defect
    on defect.id_defect = smd.id_defect
on conflict (material_id, id_defect) do update set
    urutan = excluded.urutan,
    wajib_check = true,
    proses_scope = excluded.proses_scope,
    satuan_input = excluded.satuan_input,
    metode_pengukuran = excluded.metode_pengukuran,
    aktif = true;

-- ============================================================================
-- 10. UPSERT DEFAULT PROCESS DEFECT PER PART
-- ============================================================================

insert into public.m_part_defect (
    uniq_no,
    id_defect,
    urutan,
    wajib_check,
    sumber,
    proses_scope,
    satuan_input,
    metode_pengukuran,
    aktif
)
select
    part.uniq_no,
    defect.id_defect,
    row_number() over (partition by part.uniq_no order by defect.nama_defect)::int as urutan,
    true,
    'PROSES',
    part.komoditas::text,
    defect.satuan_input,
    defect.metode_pengukuran,
    true
from public.m_part part
join public.m_defect defect
    on defect.aktif = true
   and defect.kategori::text = 'PROSES'
   and defect.proses_default::text = part.komoditas::text
where part.aktif = true
  and part.komoditas::text in ('PRESS', 'SEWING')
on conflict (uniq_no, id_defect) do update set
    sumber = 'PROSES',
    proses_scope = excluded.proses_scope,
    satuan_input = excluded.satuan_input,
    metode_pengukuran = excluded.metode_pengukuran,
    aktif = true,
    diperbarui_pada = now();

-- ============================================================================
-- 11. UPSERT PART CUTTING SIZE REFERENCE
-- ============================================================================

insert into public.m_part_cutting_size_reference (
    uniq_no,
    size_cutting_cm,
    nama_material_sumber,
    part_no_sumber,
    project_sumber,
    urutan,
    aktif
)
select
    scs.uniq_no,
    scs.size_cutting_cm,
    scs.nama_material_sumber,
    scs.part_no_sumber,
    scs.project_sumber,
    scs.urutan,
    scs.aktif
from seed_part_cutting_size scs
join public.m_part part
    on part.uniq_no = scs.uniq_no
on conflict (uniq_no, size_cutting_cm) do update set
    nama_material_sumber = excluded.nama_material_sumber,
    part_no_sumber = excluded.part_no_sumber,
    project_sumber = excluded.project_sumber,
    urutan = excluded.urutan,
    aktif = true,
    diperbarui_pada = now();

-- Material-level Cutting size dropdown.
insert into public.m_cutting_size_reference (
    material_id,
    material_spec_id,
    size_cutting_cm,
    ukuran_cutting_cm,
    lebar_roll_cm,
    panjang_roll_cm,
    berat_gsm,
    tebal_mm,
    label_ukuran,
    urutan,
    is_default,
    aktif
)
select distinct on (mat.id, scs.size_cutting_cm)
    mat.id,
    ms.id,
    scs.size_cutting_cm,
    scs.size_cutting_cm,
    ms.lebar_cm,
    ms.panjang_roll_cm,
    ms.berat_gsm,
    ms.tebal_mm,
    trim(to_char(scs.size_cutting_cm, 'FM999999990.###')) || ' cm',
    scs.urutan,
    scs.urutan = 1,
    true
from seed_part_cutting_size scs
join public.m_material mat
    on mat.nama_normalisasi = upper(trim(regexp_replace(scs.nama_material_sumber, '\s+', ' ', 'g')))
left join public.m_material_spec ms
    on ms.material_id = mat.id
   and ms.is_default = true
where scs.aktif = true
order by mat.id, scs.size_cutting_cm, scs.urutan
on conflict (material_id, size_cutting_cm) do update set
    material_spec_id = coalesce(excluded.material_spec_id, public.m_cutting_size_reference.material_spec_id),
    ukuran_cutting_cm = excluded.ukuran_cutting_cm,
    lebar_roll_cm = coalesce(excluded.lebar_roll_cm, public.m_cutting_size_reference.lebar_roll_cm),
    panjang_roll_cm = coalesce(excluded.panjang_roll_cm, public.m_cutting_size_reference.panjang_roll_cm),
    berat_gsm = coalesce(excluded.berat_gsm, public.m_cutting_size_reference.berat_gsm),
    tebal_mm = coalesce(excluded.tebal_mm, public.m_cutting_size_reference.tebal_mm),
    label_ukuran = excluded.label_ukuran,
    urutan = least(public.m_cutting_size_reference.urutan, excluded.urutan),
    is_default = public.m_cutting_size_reference.is_default or excluded.is_default,
    aktif = true,
    diperbarui_pada = now();

-- ============================================================================
-- 12. SLOT WAKTU & DEFAULT IMAGE
-- ============================================================================

insert into public.m_slot_waktu (
    kode_slot,
    tipe_proses,
    nama_shift,
    label_waktu,
    jam_mulai,
    jam_selesai,
    urutan,
    aktif
)
select
    kode_slot,
    tipe_proses::public.tipe_proses_inspectra,
    nama_shift,
    label_waktu,
    jam_mulai,
    jam_selesai,
    urutan,
    aktif
from seed_slot_waktu
on conflict (kode_slot) do update set
    label_waktu = excluded.label_waktu,
    jam_mulai = excluded.jam_mulai,
    jam_selesai = excluded.jam_selesai,
    urutan = excluded.urutan,
    aktif = true;

insert into public.m_default_image (
    kode,
    label,
    icon_key,
    aktif
)
values
    ('PART_DEFAULT', 'Gambar part belum tersedia', 'inventory_2', true),
    ('MATERIAL_DEFAULT', 'Gambar material belum tersedia', 'layers', true),
    ('DEFECT_DEFAULT', 'Gambar defect belum tersedia', 'report_problem', true)
on conflict (kode) do update set
    label = excluded.label,
    icon_key = excluded.icon_key,
    aktif = true;

-- ============================================================================
-- 13. REFRESH VIEWS
-- ============================================================================

create or replace view public.v_checksheet_part_defect as
with defect_part as (
    select
        pd.uniq_no,
        pd.id_defect,
        pd.urutan,
        pd.sumber,
        coalesce(pd.satuan_input, d.satuan_input) as satuan_input,
        coalesce(pd.metode_pengukuran, d.metode_pengukuran) as metode_pengukuran
    from public.m_part_defect pd
    join public.m_defect d on d.id_defect = pd.id_defect
    where pd.aktif = true and d.aktif = true

    union all

    select
        pm.uniq_no,
        md.id_defect,
        md.urutan,
        'MATERIAL' as sumber,
        coalesce(md.satuan_input, d.satuan_input) as satuan_input,
        coalesce(md.metode_pengukuran, d.metode_pengukuran) as metode_pengukuran
    from public.m_part_material pm
    join public.m_material_defect md
        on md.material_id = pm.material_id
       and md.aktif = true
    join public.m_defect d
        on d.id_defect = md.id_defect
       and d.aktif = true
    where pm.aktif = true
),
defect_unik as (
    select
        uniq_no,
        id_defect,
        min(urutan) as urutan,
        min(sumber) as sumber,
        min(satuan_input) as satuan_input,
        min(metode_pengukuran) as metode_pengukuran
    from defect_part
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
                'sumber', du.sumber,
                'satuan_input', du.satuan_input,
                'metode_pengukuran', du.metode_pengukuran
            )
            order by du.urutan, d.nama_defect
        ) filter (where d.id_defect is not null),
        '[]'::jsonb
    ) as daftar_defect
from public.m_part p
left join defect_unik du
    on du.uniq_no = p.uniq_no
left join public.m_defect d
    on d.id_defect = du.id_defect
   and d.aktif = true
where p.aktif = true
  and p.komoditas::text in ('PRESS', 'SEWING')
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
    count(distinct pm.material_id)::int as jumlah_material,
    coalesce(jsonb_array_length(v.daftar_defect), 0)::int as jumlah_defect,
    case
        when count(distinct pm.material_id) = 0 then 'TANPA_MATERIAL'
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
  and p.komoditas::text in ('PRESS', 'SEWING')
group by
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    p.lokasi_gambar,
    v.daftar_defect;

-- Referensi ukuran Cutting ber-grain part. View ini dipisahkan dari picker
-- checksheet agar part PRESS/SEWING dengan ukuran Cutting tetap dapat dipakai
-- sebagai acuan tanpa diperlakukan sebagai part checksheet aktif Cutting.
create or replace view public.v_cutting_part_size_option as
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

create or replace view public.v_cutting_material_option as
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

grant select on public.v_cutting_part_size_option to anon, authenticated;

-- ============================================================================
-- 14. REVISION, CACHE RELOAD, VALIDATION
-- ============================================================================

select public.f_touch_data_revision('MASTER_DATA');
select public.f_touch_data_revision('CHECKSHEET_REFERENCE');
select public.f_touch_data_revision('CUTTING_REFERENCE');

select pg_notify('pgrst', 'reload schema');

do $$
declare
    v_supplier int;
    v_material int;
    v_part int;
    v_press int;
    v_sewing int;
    v_part_material int;
    v_material_defect int;
    v_defect int;
    v_part_cutting int;
    v_bad_column int;
begin
    select count(*) into v_supplier from public.m_supplier where aktif = true;
    select count(*) into v_material from public.m_material where aktif = true;
    select count(*) into v_part from public.m_part where aktif = true;
    select count(*) into v_press from public.m_part where aktif = true and komoditas::text = 'PRESS';
    select count(*) into v_sewing from public.m_part where aktif = true and komoditas::text = 'SEWING';
    select count(*) into v_part_material from public.m_part_material where aktif = true;
    select count(*) into v_material_defect from public.m_material_defect where aktif = true;
    select count(*) into v_defect from public.m_defect where aktif = true;
    select count(*) into v_part_cutting from public.m_part_cutting_size_reference where aktif = true;
    select count(*) into v_bad_column
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'm_part_cutting_size_reference'
      and column_name = 'sumber_data';

    if v_supplier < 16 then
        raise exception 'Seed gagal: supplier aktif kurang dari 16. Aktual=%', v_supplier;
    end if;

    if v_material < 60 then
        raise exception 'Seed gagal: material aktif kurang dari 60. Aktual=%', v_material;
    end if;

    if v_part < 78 then
        raise exception 'Seed gagal: part aktif kurang dari 78. Aktual=%', v_part;
    end if;

    if v_press < 50 then
        raise exception 'Seed gagal: part PRESS aktif kurang dari 50. Aktual=%', v_press;
    end if;

    if v_sewing <> 7 then
        raise exception 'Seed gagal: part SEWING aktif wajib 7 berdasarkan PARTLIST(16). Aktual=%', v_sewing;
    end if;

    if v_part_material < 90 then
        raise exception 'Seed gagal: relasi part-material aktif kurang dari 90. Aktual=%', v_part_material;
    end if;

    if v_material_defect < 35 then
        raise exception 'Seed gagal: relasi material-defect aktif kurang dari 35. Aktual=%', v_material_defect;
    end if;

    if v_defect < 25 then
        raise exception 'Seed gagal: master defect aktif kurang dari 25. Aktual=%', v_defect;
    end if;

    if v_part_cutting < 40 then
        raise exception 'Seed gagal: referensi ukuran Cutting per part kurang dari 40. Aktual=%', v_part_cutting;
    end if;

    if v_bad_column > 0 then
        raise exception 'Seed gagal: kolom sumber_data masih ada pada m_part_cutting_size_reference.';
    end if;

    raise notice 'Seed master data sukses. supplier=%, material=%, part=%, press=%, sewing=%, relasi_part_material=%, material_defect=%, defect=%, ukuran_cutting=%',
        v_supplier, v_material, v_part, v_press, v_sewing, v_part_material, v_material_defect, v_defect, v_part_cutting;
end $$;

commit;
