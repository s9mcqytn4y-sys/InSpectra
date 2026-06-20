-- =========================================================
-- CLEAN UP PROTOTYPE TABLES (Ensures Clean Slate)
-- =========================================================
drop view if exists public.v_checksheet_part_defect cascade;
drop view if exists public.v_master_part_detail cascade;
drop table if exists public.e_detail_cutting cascade;
drop table if exists public.e_defect_slot_checksheet cascade;
drop table if exists public.e_defect_checksheet cascade;
drop table if exists public.e_item_checksheet cascade;
drop table if exists public.e_sesi_checksheet cascade;
drop table if exists public.m_slot_waktu cascade;
drop table if exists public.m_part_defect cascade;
drop table if exists public.m_defect cascade;
drop table if exists public.m_part_material cascade;
drop table if exists public.m_material_spec cascade;
drop table if exists public.m_material cascade;
drop table if exists public.m_part cascade;
drop table if exists public.m_supplier cascade;

create extension if not exists pgcrypto;

-- =========================================================
-- DOMAIN CHECKS
-- =========================================================

do $$
begin
    if not exists (select 1 from pg_type where typname = 'tipe_proses_inspectra') then
        create type public.tipe_proses_inspectra as enum (
            'PRESS',
            'SEWING',
            'CUTTING',
            'MATERIAL',
            'PASS_THROUGH',
            'CONSUMABLE'
        );
    end if;

    if not exists (select 1 from pg_type where typname = 'kategori_defect_inspectra') then
        create type public.kategori_defect_inspectra as enum (
            'MATERIAL',
            'PROSES'
        );
    end if;

    if not exists (select 1 from pg_type where typname = 'satuan_inspectra') then
        create type public.satuan_inspectra as enum (
            'PCS',
            'ROLL',
            'MTR',
            'KG',
            'GRAM',
            'CONES',
            'CAN',
            'SET',
            'LEMBAR',
            'UNKNOWN'
        );
    end if;
end $$;

-- =========================================================
-- MASTER DATA
-- =========================================================

create table public.m_supplier (
    id uuid primary key default gen_random_uuid(),
    kode_supplier text unique,
    nama_supplier text not null unique,
    kategori text,
    alamat text,
    kontak text,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now()
);

create table public.m_material (
    id uuid primary key default gen_random_uuid(),
    kode_material text unique,
    nama_material text not null,
    nama_normalisasi text generated always as (
        upper(trim(regexp_replace(nama_material, '\s+', ' ', 'g')))
    ) stored,
    supplier_id uuid references public.m_supplier(id) on delete restrict,
    supplier_manual text,
    jenis_material text,
    spec_ringkas text,
    satuan public.satuan_inspectra not null default 'UNKNOWN',
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    unique (nama_normalisasi, supplier_id, spec_ringkas)
);

create table public.m_material_spec (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.m_material(id) on delete cascade,
    spec_asli text,
    lebar_value numeric(12, 3),
    lebar_unit text default 'm',
    panjang_value numeric(12, 3),
    panjang_unit text default 'm',
    tebal_value numeric(12, 3),
    tebal_unit text default 'mm',
    berat_value numeric(12, 3),
    berat_unit text default 'gsm',
    qty_value numeric(12, 3),
    qty_unit public.satuan_inspectra default 'UNKNOWN',
    warna text,
    grade text,
    keterangan text,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    unique (material_id, spec_asli)
);

create table public.m_part (
    id uuid primary key default gen_random_uuid(),
    uniq_no text not null unique,
    part_no text,
    nama_part text not null,
    model text,
    customer text,
    komoditas public.tipe_proses_inspectra not null,
    total_item_per_kanban int check (total_item_per_kanban is null or total_item_per_kanban >= 0),
    sample_item_per_kanban int check (sample_item_per_kanban is null or sample_item_per_kanban >= 0),
    sample_cycle_note text,
    lokasi_gambar text,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now()
);

create table public.m_part_material (
    id uuid primary key default gen_random_uuid(),
    uniq_no text not null references public.m_part(uniq_no) on delete cascade,
    material_id uuid not null references public.m_material(id) on delete restrict,
    material_spec_id uuid references public.m_material_spec(id) on delete restrict,
    urutan int not null default 1 check (urutan > 0),
    label_material text not null,
    qty_per_part numeric(12, 4),
    wajib_check boolean not null default true,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    unique (uniq_no, material_id, material_spec_id)
);

create table public.m_defect (
    id_defect text primary key,
    nama_defect text not null,
    kategori public.kategori_defect_inspectra not null,
    proses_default public.tipe_proses_inspectra,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now()
);

create table public.m_part_defect (
    id uuid primary key default gen_random_uuid(),
    uniq_no text not null references public.m_part(uniq_no) on delete cascade,
    id_defect text not null references public.m_defect(id_defect) on delete restrict,
    urutan int not null default 1 check (urutan > 0),
    wajib_check boolean not null default true,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    unique (uniq_no, id_defect)
);

-- Referensi slot dari Drive QControl: slot minimum 8.
create table public.m_slot_waktu (
    id uuid primary key default gen_random_uuid(),
    kode_slot text not null unique,
    tipe_proses public.tipe_proses_inspectra not null,
    nama_shift text not null default 'SHIFT_1',
    label_waktu text not null,
    urutan int not null check (urutan > 0),
    aktif boolean not null default true
);

-- =========================================================
-- E-CHECKSHEET
-- =========================================================

create table public.e_sesi_checksheet (
    id uuid primary key default gen_random_uuid(),
    kode_sesi text unique,
    tipe_proses public.tipe_proses_inspectra not null check (tipe_proses in ('PRESS', 'SEWING', 'CUTTING')),
    tanggal_pemeriksaan date not null default current_date,
    nama_shift text not null default 'SHIFT_1',
    nama_operator text,
    nama_line text,
    device_id text,
    app_version text,
    total_diperiksa int not null default 0 check (total_diperiksa >= 0),
    total_ok int not null default 0 check (total_ok >= 0),
    total_ng int not null default 0 check (total_ng >= 0),
    rasio_ng_global numeric(8, 3) not null default 0,
    status text not null default 'TERKIRIM',
    dibuat_pada timestamptz not null default now(),
    constraint chk_sesi_total_valid check (total_diperiksa = total_ok + total_ng)
);

create table public.e_item_checksheet (
    id uuid primary key default gen_random_uuid(),
    id_sesi uuid not null references public.e_sesi_checksheet(id) on delete cascade,
    uniq_no text not null references public.m_part(uniq_no) on delete restrict,
    jumlah_diperiksa int not null default 0 check (jumlah_diperiksa >= 0),
    jumlah_ok int not null default 0 check (jumlah_ok >= 0),
    jumlah_ng int not null default 0 check (jumlah_ng >= 0),
    rasio_ng numeric(8, 3) not null default 0,
    catatan text,
    dibuat_pada timestamptz not null default now(),
    constraint chk_item_total_valid check (jumlah_diperiksa = jumlah_ok + jumlah_ng)
);

create table public.e_defect_checksheet (
    id uuid primary key default gen_random_uuid(),
    id_item uuid not null references public.e_item_checksheet(id) on delete cascade,
    id_defect text not null references public.m_defect(id_defect) on delete restrict,
    nama_defect_snapshot text not null,
    kategori public.kategori_defect_inspectra not null,
    jumlah int not null default 0 check (jumlah >= 0),
    dibuat_pada timestamptz not null default now(),
    unique (id_item, id_defect)
);

-- Detail slot supaya aturan Daily NG Press bisa divalidasi:
-- total NG harus sama dengan jumlah detail slot.
create table public.e_defect_slot_checksheet (
    id uuid primary key default gen_random_uuid(),
    id_defect_checksheet uuid not null references public.e_defect_checksheet(id) on delete cascade,
    slot_waktu_id uuid references public.m_slot_waktu(id) on delete restrict,
    jumlah int not null default 0 check (jumlah >= 0),
    dibuat_pada timestamptz not null default now()
);

-- Detail Cutting dari Daily Report Cutting.
create table public.e_detail_cutting (
    id uuid primary key default gen_random_uuid(),
    id_item uuid not null references public.e_item_checksheet(id) on delete cascade,
    no_lot_roll text,
    no_roll text,
    size_cutting_cm text,
    qty_ok int check (qty_ok is null or qty_ok >= 0),
    qty_ng int check (qty_ng is null or qty_ng >= 0),
    waste numeric(12, 3),
    pic text,
    catatan text,
    dibuat_pada timestamptz not null default now()
);

-- =========================================================
-- VIEWS
-- =========================================================

create or replace view public.v_checksheet_part_defect as
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
                'urutan', pd.urutan
            )
            order by pd.urutan, d.nama_defect
        ) filter (where d.id_defect is not null),
        '[]'::jsonb
    ) as daftar_defect
from public.m_part p
left join public.m_part_defect pd
    on pd.uniq_no = p.uniq_no
    and pd.aktif = true
left join public.m_defect d
    on d.id_defect = pd.id_defect
    and d.aktif = true
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

create or replace view public.v_master_part_detail as
select
    p.id,
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas::text as komoditas,
    p.total_item_per_kanban,
    p.sample_item_per_kanban,
    p.sample_cycle_note,
    p.lokasi_gambar,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'id', pm.id,
                'label_material', pm.label_material,
                'nama_material', m.nama_material,
                'supplier', coalesce(s.nama_supplier, m.supplier_manual),
                'spec_ringkas', m.spec_ringkas,
                'spec_asli', ms.spec_asli,
                'satuan', m.satuan::text,
                'lebar_value', ms.lebar_value,
                'panjang_value', ms.panjang_value,
                'tebal_value', ms.tebal_value,
                'berat_value', ms.berat_value,
                'qty_value', ms.qty_value,
                'warna', ms.warna,
                'grade', ms.grade
            )
        ) filter (where pm.id is not null),
        '[]'::jsonb
    ) as daftar_material
from public.m_part p
left join public.m_part_material pm on pm.uniq_no = p.uniq_no and pm.aktif = true
left join public.m_material m on m.id = pm.material_id and m.aktif = true
left join public.m_supplier s on s.id = m.supplier_id and s.aktif = true
left join public.m_material_spec ms on ms.id = pm.material_spec_id and ms.aktif = true
where p.aktif = true
group by
    p.id,
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    p.total_item_per_kanban,
    p.sample_item_per_kanban,
    p.sample_cycle_note,
    p.lokasi_gambar;

-- =========================================================
-- INDEX
-- =========================================================

create index if not exists idx_m_part_komoditas_aktif on public.m_part(komoditas, uniq_no) where aktif = true;
create index if not exists idx_m_material_search on public.m_material(nama_normalisasi) where aktif = true;
create index if not exists idx_m_part_material_uniq on public.m_part_material(uniq_no) where aktif = true;
create index if not exists idx_m_part_defect_uniq on public.m_part_defect(uniq_no) where aktif = true;
create index if not exists idx_e_sesi_tanggal_tipe on public.e_sesi_checksheet(tanggal_pemeriksaan desc, tipe_proses);
create index if not exists idx_e_item_sesi on public.e_item_checksheet(id_sesi);
create index if not exists idx_e_defect_item on public.e_defect_checksheet(id_item);

select pg_notify('pgrst', 'reload schema');
