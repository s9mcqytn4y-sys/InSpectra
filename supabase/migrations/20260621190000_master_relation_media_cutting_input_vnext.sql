-- =========================================================
-- INSPECTRA vNEXT SCHEMA
-- Fokus:
-- 1. Master data relation hardening
-- 2. Supplier-material matching
-- 3. Part composition, material composition, child part
-- 4. Material defect mapping untuk checksheet & cutting
-- 5. Cutting size + roll dimension reference
-- 6. Media/image part lifecycle
-- 7. Safe CRUD guard + audit log
-- 8. Server-authoritative cache revision
-- 9. RPC submit fail-safe untuk Press/Sewing dan Cutting
-- =========================================================

begin;

create extension if not exists pgcrypto;

-- =========================================================
-- 0. BASE COMPATIBILITY COLUMNS
-- =========================================================

alter table if exists public.m_part
    add column if not exists kode_internal text,
    add column if not exists sumber_data text not null default 'MANUAL',
    add column if not exists status_kelengkapan text not null default 'BELUM_DICEK',
    add column if not exists butuh_review boolean not null default false,
    add column if not exists catatan_review text,
    add column if not exists dibuat_oleh text,
    add column if not exists diperbarui_oleh text;

alter table if exists public.m_material
    add column if not exists nama_normalisasi text,
    add column if not exists jenis_material text,
    add column if not exists kategori_material text,
    add column if not exists default_satuan text,
    add column if not exists sumber_data text not null default 'MANUAL',
    add column if not exists status_kelengkapan text not null default 'BELUM_DICEK',
    add column if not exists butuh_review boolean not null default false,
    add column if not exists catatan_review text,
    add column if not exists dibuat_oleh text,
    add column if not exists diperbarui_oleh text;

alter table if exists public.m_supplier
    add column if not exists nama_normalisasi text,
    add column if not exists kategori_supplier text,
    add column if not exists sumber_data text not null default 'MANUAL',
    add column if not exists dibuat_oleh text,
    add column if not exists diperbarui_oleh text;

alter table if exists public.m_material_spec
    add column if not exists material_spec_code text,
    add column if not exists lebar_cm numeric(12, 3),
    add column if not exists panjang_roll_cm numeric(14, 3),
    add column if not exists tebal_mm numeric(12, 3),
    add column if not exists berat_gsm numeric(12, 3),
    add column if not exists gramasi_gsm numeric(12, 3),
    add column if not exists qty_default numeric(12, 3),
    add column if not exists satuan_qty text,
    add column if not exists sumber_data text not null default 'MANUAL',
    add column if not exists is_default boolean not null default false;

-- Konversi kompatibilitas dari kolom lama *_value.
update public.m_material_spec
set
    lebar_cm = coalesce(lebar_cm, lebar_value * 100),        -- Excel memakai meter untuk roll: 1.8 => 180 cm
    panjang_roll_cm = coalesce(panjang_roll_cm, panjang_value * 100),
    tebal_mm = coalesce(tebal_mm, tebal_value),
    berat_gsm = coalesce(berat_gsm, berat_value),
    gramasi_gsm = coalesce(gramasi_gsm, berat_value),
    qty_default = coalesce(qty_default, qty_value)
where aktif = true;

update public.m_material
set nama_normalisasi = upper(trim(regexp_replace(coalesce(nama_material, ''), '\s+', ' ', 'g')))
where nama_normalisasi is null;

update public.m_supplier
set nama_normalisasi = upper(trim(regexp_replace(coalesce(nama_supplier, ''), '\s+', ' ', 'g')))
where nama_normalisasi is null;

-- =========================================================
-- 1. DATA REVISION / CACHE SERVER-AUTHORITATIVE
-- Client boleh cache GET data 5-15 menit, tetapi wajib bandingkan revision.
-- =========================================================

create table if not exists public.m_data_revision (
    kode text primary key,
    versi bigint not null default 1,
    deskripsi text,
    diperbarui_pada timestamptz not null default now()
);

insert into public.m_data_revision (kode, versi, deskripsi)
values
    ('MASTER_DATA', 1, 'Part, material, supplier, defect, media'),
    ('CHECKSHEET_REFERENCE', 1, 'View picker part dan defect checksheet'),
    ('CUTTING_REFERENCE', 1, 'Material, ukuran cutting, dan defect cutting')
on conflict (kode) do nothing;

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

create or replace function public.f_touch_master_revision()
returns trigger
language plpgsql
as $$
begin
    perform public.f_touch_data_revision('MASTER_DATA');
    perform public.f_touch_data_revision('CHECKSHEET_REFERENCE');
    perform public.f_touch_data_revision('CUTTING_REFERENCE');
    return coalesce(new, old);
end;
$$;

drop trigger if exists trg_revision_part on public.m_part;
create trigger trg_revision_part
after insert or update or delete on public.m_part
for each row execute function public.f_touch_master_revision();

drop trigger if exists trg_revision_material on public.m_material;
create trigger trg_revision_material
after insert or update or delete on public.m_material
for each row execute function public.f_touch_master_revision();

drop trigger if exists trg_revision_supplier on public.m_supplier;
create trigger trg_revision_supplier
after insert or update or delete on public.m_supplier
for each row execute function public.f_touch_master_revision();

drop trigger if exists trg_revision_defect on public.m_defect;
create trigger trg_revision_defect
after insert or update or delete on public.m_defect
for each row execute function public.f_touch_master_revision();

-- =========================================================
-- 2. SUPPLIER - MATERIAL MATCHING
-- Satu material bisa punya supplier utama dan supplier alternatif.
-- =========================================================

create table if not exists public.m_material_supplier (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.m_material(id) on delete cascade,
    supplier_id uuid not null references public.m_supplier(id) on delete restrict,
    supplier_part_no text,
    supplier_material_name text,
    harga_referensi numeric(16, 2),
    mata_uang text not null default 'IDR',
    lead_time_hari int,
    is_preferred boolean not null default false,
    aktif boolean not null default true,
    sumber_data text not null default 'MANUAL',
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    unique (material_id, supplier_id)
);

create index if not exists idx_material_supplier_material
on public.m_material_supplier(material_id)
where aktif = true;

create index if not exists idx_material_supplier_supplier
on public.m_material_supplier(supplier_id)
where aktif = true;

-- Backfill dari kolom lama supplier_id bila ada.
insert into public.m_material_supplier (
    material_id,
    supplier_id,
    is_preferred,
    sumber_data
)
select
    m.id,
    m.supplier_id,
    true,
    'BACKFILL_MATERIAL_SUPPLIER'
from public.m_material m
where m.supplier_id is not null
on conflict (material_id, supplier_id) do update set
    is_preferred = true,
    aktif = true,
    diperbarui_pada = now();

-- =========================================================
-- 3. MATERIAL COMPOSITION
-- Contoh:
-- Protector = Recycle Felt + Spunbond + Laminasi LDPE + child material lain.
-- =========================================================

create table if not exists public.m_material_komposisi (
    id uuid primary key default gen_random_uuid(),
    parent_material_id uuid not null references public.m_material(id) on delete cascade,
    child_material_id uuid not null references public.m_material(id) on delete restrict,
    child_material_spec_id uuid references public.m_material_spec(id) on delete restrict,
    peran_material text not null default 'KOMPONEN',
    urutan int not null default 1 check (urutan > 0),
    qty numeric(14, 4),
    satuan text,
    persentase numeric(7, 3) check (persentase is null or (persentase >= 0 and persentase <= 100)),
    wajib boolean not null default true,
    aktif boolean not null default true,
    sumber_data text not null default 'MANUAL',
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    check (parent_material_id <> child_material_id),
    unique (parent_material_id, child_material_id, coalesce(child_material_spec_id, '00000000-0000-0000-0000-000000000000'::uuid), peran_material)
);

create index if not exists idx_material_komposisi_parent
on public.m_material_komposisi(parent_material_id)
where aktif = true;

create index if not exists idx_material_komposisi_child
on public.m_material_komposisi(child_material_id)
where aktif = true;

-- =========================================================
-- 4. PART COMPOSITION & CHILD PART
-- =========================================================

alter table if exists public.m_part_material
    add column if not exists material_supplier_id uuid references public.m_material_supplier(id) on delete set null,
    add column if not exists peran_material text not null default 'UTAMA',
    add column if not exists usage_area text,
    add column if not exists qty_per_kanban numeric(14, 4),
    add column if not exists panjang_kebutuhan_cm numeric(14, 3),
    add column if not exists lebar_kebutuhan_cm numeric(14, 3),
    add column if not exists sumber_data text not null default 'MANUAL',
    add column if not exists catatan text;

create table if not exists public.m_part_child (
    id uuid primary key default gen_random_uuid(),
    parent_uniq_no text not null references public.m_part(uniq_no) on delete cascade,
    child_uniq_no text not null references public.m_part(uniq_no) on delete restrict,
    peran_child text not null default 'KOMPONEN',
    qty numeric(14, 4) not null default 1 check (qty > 0),
    urutan int not null default 1 check (urutan > 0),
    aktif boolean not null default true,
    sumber_data text not null default 'MANUAL',
    catatan text,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    check (parent_uniq_no <> child_uniq_no),
    unique (parent_uniq_no, child_uniq_no, peran_child)
);

create index if not exists idx_part_child_parent
on public.m_part_child(parent_uniq_no)
where aktif = true;

create index if not exists idx_part_child_child
on public.m_part_child(child_uniq_no)
where aktif = true;

-- =========================================================
-- 5. DEFECT MAPPING
-- Material defect otomatis turun ke part yang memakai material tersebut.
-- Bisa di-override per part.
-- =========================================================

alter table if exists public.m_defect
    add column if not exists kode_display text,
    add column if not exists satuan_input text not null default 'PCS',
    add column if not exists metode_pengukuran text not null default 'COUNT',
    add column if not exists proses_scope text not null default 'ALL',
    add column if not exists warna_badge text,
    add column if not exists icon_key text;

alter table if exists public.m_material_defect
    add column if not exists proses_scope text not null default 'ALL',
    add column if not exists satuan_input text,
    add column if not exists metode_pengukuran text,
    add column if not exists severity int,
    add column if not exists sumber_data text not null default 'MANUAL',
    add column if not exists catatan text;

alter table if exists public.m_part_defect
    add column if not exists proses_scope text not null default 'ALL',
    add column if not exists satuan_input text,
    add column if not exists metode_pengukuran text,
    add column if not exists sumber_data text not null default 'MANUAL',
    add column if not exists catatan text;

create table if not exists public.m_part_defect_override (
    id uuid primary key default gen_random_uuid(),
    uniq_no text not null references public.m_part(uniq_no) on delete cascade,
    id_defect text not null references public.m_defect(id_defect) on delete restrict,
    mode_override text not null check (mode_override in ('INCLUDE', 'EXCLUDE')),
    alasan text,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    unique (uniq_no, id_defect)
);

-- Set defect cutting berbasis panjang CM untuk material/cutting.
update public.m_defect
set
    satuan_input = case
        when upper(coalesce(proses_default, '')) = 'CUTTING' then 'CM'
        when upper(coalesce(nama_defect, '')) like '%LAMINAT%' then 'CM'
        when upper(coalesce(nama_defect, '')) like '%SOBEK%' then 'CM'
        else satuan_input
    end,
    metode_pengukuran = case
        when upper(coalesce(proses_default, '')) = 'CUTTING' then 'LENGTH_CM'
        when upper(coalesce(nama_defect, '')) like '%LAMINAT%' then 'LENGTH_CM'
        when upper(coalesce(nama_defect, '')) like '%SOBEK%' then 'LENGTH_CM'
        else metode_pengukuran
    end
where aktif = true;

-- =========================================================
-- 6. CUTTING SIZE & ROLL REFERENCE
-- Satu material bisa punya banyak ukuran cutting.
-- Manual input user bisa di-promote sebagai reference setelah submit.
-- =========================================================

create table if not exists public.m_cutting_size_reference (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.m_material(id) on delete cascade,
    material_spec_id uuid references public.m_material_spec(id) on delete set null,
    size_cutting_cm numeric(12, 3) check (size_cutting_cm > 0),
    ukuran_cutting_cm numeric(12, 3),
    lebar_roll_cm numeric(12, 3),
    panjang_roll_cm numeric(14, 3),
    berat_gsm numeric(12, 3),
    tebal_mm numeric(12, 3),
    label_ukuran text,
    urutan int not null default 1 check (urutan > 0),
    is_default boolean not null default false,
    pemakaian_count int not null default 0,
    sumber_data text not null default 'MANUAL',
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now()
);

-- Backfill roll dimension dari m_material_spec.
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
    sumber_data,
    urutan,
    is_default
)
select
    ms.material_id,
    ms.id,
    null,
    null,
    coalesce(ms.lebar_cm, ms.lebar_value * 100),
    coalesce(ms.panjang_roll_cm, ms.panjang_value * 100),
    coalesce(ms.berat_gsm, ms.berat_value),
    coalesce(ms.tebal_mm, ms.tebal_value),
    concat_ws(
        ' · ',
        nullif(ms.spec_asli, ''),
        case when coalesce(ms.lebar_cm, ms.lebar_value * 100) is not null then 'Lebar ' || round(coalesce(ms.lebar_cm, ms.lebar_value * 100), 1)::text || ' cm' end,
        case when coalesce(ms.panjang_roll_cm, ms.panjang_value * 100) is not null then 'Roll ' || round(coalesce(ms.panjang_roll_cm, ms.panjang_value * 100), 1)::text || ' cm' end
    ),
    'BACKFILL_MATERIAL_SPEC',
    99,
    false
from public.m_material_spec ms
where ms.aktif = true
on conflict do nothing;

create unique index if not exists ux_cutting_size_ref_material_size
on public.m_cutting_size_reference(material_id, coalesce(material_spec_id, '00000000-0000-0000-0000-000000000000'::uuid), size_cutting_cm)
where aktif = true;

create index if not exists idx_cutting_size_ref_material
on public.m_cutting_size_reference(material_id, urutan)
where aktif = true;

-- =========================================================
-- 7. PART IMAGE / MEDIA SYSTEM
-- Storage path convention:
-- bucket: part-images
-- path  : part/{uniq_no}/{uuid}.webp
-- local temp Android: context.cacheDir/inspectra/images/{uuid}.jpg|webp
-- =========================================================

do $$
begin
    if exists (select 1 from information_schema.schemata where schema_name = 'storage') then
        insert into storage.buckets (
            id,
            name,
            public,
            file_size_limit,
            allowed_mime_types
        )
        values (
            'part-images',
            'part-images',
            true,
            3145728,
            array['image/jpeg', 'image/png', 'image/webp']
        )
        on conflict (id) do update set
            public = excluded.public,
            file_size_limit = excluded.file_size_limit,
            allowed_mime_types = excluded.allowed_mime_types;
    end if;
end $$;

create table if not exists public.m_media_asset (
    id uuid primary key default gen_random_uuid(),
    bucket_id text not null default 'part-images',
    storage_path text not null,
    public_url text,
    mime_type text,
    size_bytes bigint,
    width_px int,
    height_px int,
    checksum_sha256 text,
    status text not null default 'AKTIF' check (status in ('AKTIF','DIHAPUS','DIGANTI','GAGAL_UPLOAD')),
    sumber text not null default 'USER_UPLOAD',
    dibuat_oleh text,
    dibuat_pada timestamptz not null default now(),
    unique (bucket_id, storage_path)
);

create table if not exists public.m_part_image (
    id uuid primary key default gen_random_uuid(),
    uniq_no text not null references public.m_part(uniq_no) on delete cascade,
    media_id uuid references public.m_media_asset(id) on delete set null,
    is_primary boolean not null default true,
    urutan int not null default 1,
    aktif boolean not null default true,
    catatan text,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now()
);

create unique index if not exists ux_part_image_primary
on public.m_part_image(uniq_no)
where aktif = true and is_primary = true;

create table if not exists public.m_default_image (
    kode text primary key,
    label text not null,
    icon_key text,
    bucket_id text,
    storage_path text,
    public_url text,
    aktif boolean not null default true
);

insert into public.m_default_image (kode, label, icon_key)
values
    ('PART_DEFAULT', 'Gambar part belum tersedia', 'inventory_2'),
    ('MATERIAL_DEFAULT', 'Gambar material belum tersedia', 'layers'),
    ('DEFECT_DEFAULT', 'Gambar defect belum tersedia', 'report_problem')
on conflict (kode) do update set
    label = excluded.label,
    icon_key = excluded.icon_key,
    aktif = true;

-- =========================================================
-- 8. E-CUTTING EXTENSION UNTUK FLOW BARU
-- OK tetap layer count; NG dan waste berbasis cm.
-- =========================================================

alter table if exists public.e_cutting_batch
    add column if not exists nama_material_snapshot text,
    add column if not exists spec_material_snapshot text,
    add column if not exists size_reference_id uuid references public.m_cutting_size_reference(id) on delete set null,
    add column if not exists nomor_lot_roll text,
    add column if not exists panjang_ok_cm numeric(14, 3),
    add column if not exists panjang_ng_cm numeric(14, 3) not null default 0,
    add column if not exists panjang_waste_cm numeric(14, 3),
    add column if not exists panjang_roll_awal_cm numeric(14, 3),
    add column if not exists ukuran_manual boolean not null default false,
    add column if not exists jumlah_potong_ok int,
    add column if not exists sumber_input text not null default 'ANDROID';

update public.e_cutting_batch
set
    nomor_lot_roll = coalesce(nomor_lot_roll, no_lot_roll),
    panjang_waste_cm = coalesce(panjang_waste_cm, waste_panjang_cm),
    panjang_ok_cm = coalesce(panjang_ok_cm, coalesce(size_cutting_cm, ukuran_cutting_cm, 0) * qty_layer_ok),
    panjang_ng_cm = coalesce(panjang_ng_cm, coalesce(size_cutting_cm, ukuran_cutting_cm, 0) * qty_layer_ng)
where true;

alter table if exists public.e_cutting_defect_detail
    add column if not exists jumlah_layer_terdampak int,
    add column if not exists persentase_dari_ng numeric(8, 3),
    add column if not exists sumber_input text not null default 'ANDROID';

update public.e_cutting_defect_detail
set jumlah_layer_terdampak = coalesce(jumlah_layer_terdampak, jumlah_layer)
where jumlah_layer_terdampak is null;

-- =========================================================
-- 9. AUDIT & SAFE CRUD
-- =========================================================

create table if not exists public.audit_master_data (
    id uuid primary key default gen_random_uuid(),
    tabel text not null,
    aksi text not null check (aksi in ('CREATE','UPDATE','DELETE','RESTORE','RELATE','UNRELATE')),
    entity_key text,
    before_data jsonb,
    after_data jsonb,
    alasan text,
    actor text,
    dibuat_pada timestamptz not null default now()
);

create table if not exists public.m_crud_guard_rule (
    kode text primary key,
    label text not null,
    perlu_konfirmasi boolean not null default true,
    perlu_alasan boolean not null default false,
    severity text not null default 'MEDIUM' check (severity in ('LOW','MEDIUM','HIGH')),
    aktif boolean not null default true
);

insert into public.m_crud_guard_rule (kode, label, perlu_konfirmasi, perlu_alasan, severity)
values
    ('DELETE_PART', 'Menghapus/nonaktifkan part akan mempengaruhi daftar checksheet.', true, true, 'HIGH'),
    ('DELETE_MATERIAL', 'Menghapus/nonaktifkan material akan mempengaruhi komposisi part dan cutting.', true, true, 'HIGH'),
    ('DELETE_DEFECT', 'Menghapus/nonaktifkan defect akan mengubah pilihan defect pada checksheet.', true, true, 'HIGH'),
    ('CHANGE_PART_PROCESS', 'Mengubah komoditas/proses part akan memindahkan part di checksheet.', true, true, 'HIGH'),
    ('CHANGE_MATERIAL_DEFECT', 'Mengubah defect material akan mempengaruhi part yang memakai material tersebut.', true, false, 'MEDIUM')
on conflict (kode) do update set
    label = excluded.label,
    perlu_konfirmasi = excluded.perlu_konfirmasi,
    perlu_alasan = excluded.perlu_alasan,
    severity = excluded.severity,
    aktif = true;

-- =========================================================
-- 10. VIEWS FOR UI
-- =========================================================

create or replace view public.v_part_image_aktif as
select
    p.uniq_no,
    coalesce(asset.public_url, def.public_url) as image_url,
    coalesce(asset.storage_path, def.storage_path) as storage_path,
    coalesce(asset.mime_type, 'image/webp') as mime_type,
    case when asset.id is null then true else false end as menggunakan_default
from public.m_part p
left join public.m_part_image pi
    on pi.uniq_no = p.uniq_no
   and pi.aktif = true
   and pi.is_primary = true
left join public.m_media_asset asset
    on asset.id = pi.media_id
   and asset.status = 'AKTIF'
left join public.m_default_image def
    on def.kode = 'PART_DEFAULT'
   and def.aktif = true;

create or replace view public.v_material_detail_komplet as
select
    m.id as material_id,
    m.nama_material,
    m.nama_normalisasi,
    m.jenis_material,
    m.kategori_material,
    coalesce(m.satuan, m.default_satuan, ms.satuan, 'UNKNOWN') as satuan,
    m.spec,
    m.spec_ringkas,
    ms.id as material_spec_id,
    ms.spec_asli,
    coalesce(ms.lebar_cm, ms.lebar_value * 100) as lebar_cm,
    coalesce(ms.panjang_roll_cm, ms.panjang_value * 100) as panjang_roll_cm,
    coalesce(ms.tebal_mm, ms.tebal_value) as tebal_mm,
    coalesce(ms.berat_gsm, ms.berat_value) as berat_gsm,
    coalesce(ms.gramasi_gsm, ms.berat_value) as gramasi_gsm,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'supplier_id', s.id,
                'nama_supplier', s.nama_supplier,
                'is_preferred', msup.is_preferred,
                'supplier_part_no', msup.supplier_part_no
            )
        ) filter (where s.id is not null),
        '[]'::jsonb
    ) as daftar_supplier,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'id_defect', d.id_defect,
                'nama_defect', d.nama_defect,
                'kategori', d.kategori::text,
                'satuan_input', coalesce(md.satuan_input, d.satuan_input),
                'metode_pengukuran', coalesce(md.metode_pengukuran, d.metode_pengukuran),
                'proses_scope', md.proses_scope
            )
        ) filter (where d.id_defect is not null),
        '[]'::jsonb
    ) as daftar_defect
from public.m_material m
left join public.m_material_spec ms
    on ms.material_id = m.id
   and ms.aktif = true
left join public.m_material_supplier msup
    on msup.material_id = m.id
   and msup.aktif = true
left join public.m_supplier s
    on s.id = msup.supplier_id
   and s.aktif = true
left join public.m_material_defect md
    on md.material_id = m.id
   and md.aktif = true
left join public.m_defect d
    on d.id_defect = md.id_defect
   and d.aktif = true
where m.aktif = true
group by
    m.id,
    m.nama_material,
    m.nama_normalisasi,
    m.jenis_material,
    m.kategori_material,
    m.satuan,
    m.default_satuan,
    m.spec,
    m.spec_ringkas,
    ms.id,
    ms.satuan,
    ms.spec_asli,
    ms.lebar_cm,
    ms.lebar_value,
    ms.panjang_roll_cm,
    ms.panjang_value,
    ms.tebal_mm,
    ms.tebal_value,
    ms.berat_gsm,
    ms.berat_value,
    ms.gramasi_gsm;

create or replace view public.v_part_detail_komplet as
select
    p.id,
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas::text as komoditas,
    img.image_url,
    img.menggunakan_default,
    p.total_item_per_kanban,
    p.sample_item_per_kanban,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'material_id', m.id,
                'nama_material', m.nama_material,
                'label_material', pm.label_material,
                'peran_material', pm.peran_material,
                'urutan', pm.urutan,
                'qty_per_part', pm.qty_per_part,
                'material_spec_id', pm.material_spec_id
            )
        ) filter (where m.id is not null),
        '[]'::jsonb
    ) as daftar_material,
    coalesce(
        jsonb_agg(
            distinct jsonb_build_object(
                'child_uniq_no', pc.child_uniq_no,
                'peran_child', pc.peran_child,
                'qty', pc.qty,
                'urutan', pc.urutan
            )
        ) filter (where pc.id is not null),
        '[]'::jsonb
    ) as daftar_child_part
from public.m_part p
left join public.v_part_image_aktif img
    on img.uniq_no = p.uniq_no
left join public.m_part_material pm
    on pm.uniq_no = p.uniq_no
   and pm.aktif = true
left join public.m_material m
    on m.id = pm.material_id
   and m.aktif = true
left join public.m_part_child pc
    on pc.parent_uniq_no = p.uniq_no
   and pc.aktif = true
where p.aktif = true
group by
    p.id,
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    img.image_url,
    img.menggunakan_default,
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
    img.image_url,
    img.menggunakan_default,
    count(distinct pm.material_id)::int as jumlah_material,
    count(distinct d.id_defect)::int as jumlah_defect,
    case
        when count(distinct pm.material_id) = 0 then 'TANPA_MATERIAL'
        when count(distinct d.id_defect) = 0 then 'TANPA_DEFECT'
        else 'SIAP_INPUT'
    end as status_input
from public.m_part p
left join public.v_part_image_aktif img
    on img.uniq_no = p.uniq_no
left join public.m_part_material pm
    on pm.uniq_no = p.uniq_no
   and pm.aktif = true
left join public.m_material_defect md
    on md.material_id = pm.material_id
   and md.aktif = true
left join public.m_part_defect pd
    on pd.uniq_no = p.uniq_no
   and pd.aktif = true
left join public.m_defect d
    on d.id_defect = coalesce(md.id_defect, pd.id_defect)
   and d.aktif = true
where p.aktif = true
  and p.komoditas in ('PRESS','SEWING')
group by
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    img.image_url,
    img.menggunakan_default;

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
    where pd.aktif = true
      and d.aktif = true

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
defect_filtered as (
    select dp.*
    from defect_part dp
    left join public.m_part_defect_override ov
        on ov.uniq_no = dp.uniq_no
       and ov.id_defect = dp.id_defect
       and ov.aktif = true
       and ov.mode_override = 'EXCLUDE'
    where ov.id is null
),
defect_unik as (
    select
        uniq_no,
        id_defect,
        min(urutan) as urutan,
        min(sumber) as sumber,
        min(satuan_input) as satuan_input,
        min(metode_pengukuran) as metode_pengukuran
    from defect_filtered
    group by uniq_no, id_defect
)
select
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas::text as komoditas,
    img.image_url as lokasi_gambar,
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
left join public.v_part_image_aktif img
    on img.uniq_no = p.uniq_no
left join defect_unik du
    on du.uniq_no = p.uniq_no
left join public.m_defect d
    on d.id_defect = du.id_defect
   and d.aktif = true
where p.aktif = true
  and p.komoditas in ('PRESS','SEWING')
group by
    p.uniq_no,
    p.part_no,
    p.nama_part,
    p.model,
    p.customer,
    p.komoditas,
    img.image_url,
    p.total_item_per_kanban,
    p.sample_item_per_kanban;

create or replace view public.v_cutting_material_option as
select
    m.id as material_id,
    m.nama_material,
    coalesce(m.spec_ringkas, m.spec, '') as spec_ringkas,
    coalesce(m.satuan::text, m.default_satuan, 'UNKNOWN') as satuan,
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
            order by ukuran.urutan, ukuran.size_cutting_cm
        ) filter (where ukuran.id is not null and ukuran.size_cutting_cm is not null),
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
   and md.proses_scope in ('ALL','CUTTING')
left join public.m_defect d
    on d.id_defect = md.id_defect
   and d.aktif = true
where m.aktif = true
group by
    m.id,
    m.nama_material,
    m.spec_ringkas,
    m.spec,
    m.satuan,
    m.default_satuan;

create or replace view public.v_cutting_daily_summary as
with batch_normalized as (
    select
        b.*,
        coalesce(b.size_cutting_cm, b.ukuran_cutting_cm) as ukuran_cm,
        coalesce(b.panjang_ok_cm, coalesce(b.size_cutting_cm, b.ukuran_cutting_cm, 0) * b.qty_layer_ok) as ok_cm,
        coalesce(b.panjang_ng_cm, coalesce(b.size_cutting_cm, b.ukuran_cutting_cm, 0) * b.qty_layer_ng) as ng_cm,
        coalesce(b.panjang_waste_cm, b.waste_panjang_cm, 0) as waste_cm
    from public.e_cutting_batch b
)
select
    s.id as id_sesi,
    s.tanggal_pemeriksaan,
    s.nama_shift,
    s.nama_line,
    b.material_id,
    max(coalesce(b.nama_material_snapshot, m.nama_material)) as nama_material,
    count(b.id)::int as total_batch,
    coalesce(sum(b.qty_layer_ok), 0)::int as total_layer_ok,
    coalesce(sum(b.qty_layer_ng), 0)::int as total_layer_ng,
    coalesce(sum(b.ok_cm), 0) as total_panjang_ok_cm,
    coalesce(sum(b.ng_cm), 0) as total_panjang_ng_cm,
    coalesce(sum(b.waste_cm), 0) as total_waste_cm,
    case
        when coalesce(sum(b.ok_cm + b.ng_cm + b.waste_cm), 0) = 0 then 0
        else round(sum(b.ng_cm)::numeric / sum(b.ok_cm + b.ng_cm + b.waste_cm)::numeric * 100, 3)
    end as rasio_ng_panjang,
    case
        when coalesce(sum(b.ok_cm + b.ng_cm + b.waste_cm), 0) = 0 then 0
        else round(sum(b.waste_cm)::numeric / sum(b.ok_cm + b.ng_cm + b.waste_cm)::numeric * 100, 3)
    end as rasio_waste_panjang
from public.e_sesi_checksheet s
left join batch_normalized b
    on b.id_sesi = s.id
left join public.m_material m
    on m.id = b.material_id
where s.tipe_proses = 'CUTTING'
group by
    s.id,
    s.tanggal_pemeriksaan,
    s.nama_shift,
    s.nama_line,
    b.material_id;

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

-- =========================================================
-- 11. RPC SUBMIT CHECKSHEET PRESS/SEWING
-- Recreate supaya 404 hilang setelah migration + cache reload.
-- =========================================================

create or replace function public.rpc_submit_checksheet(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    sesi_id uuid;
    item_id uuid;
    defect_row_id uuid;
    item jsonb;
    defect_item jsonb;
    slot_item jsonb;
    total_diperiksa int;
    total_ok int;
    total_ng int;
    tipe text;
begin
    tipe := payload ->> 'tipeProses';

    if tipe not in ('PRESS', 'SEWING') then
        raise exception 'RPC ini hanya untuk Press dan Sewing.';
    end if;

    total_diperiksa := coalesce(nullif(payload ->> 'totalDiperiksa', '')::int, 0);
    total_ok := coalesce(nullif(payload ->> 'totalOk', '')::int, 0);
    total_ng := coalesce(nullif(payload ->> 'totalNg', '')::int, 0);

    if total_diperiksa <= 0 then
        raise exception 'Jumlah pemeriksaan harus lebih besar dari nol.';
    end if;

    if total_diperiksa <> total_ok + total_ng then
        raise exception 'Total pemeriksaan tidak sesuai.';
    end if;

    insert into public.e_sesi_checksheet (
        kode_sesi,
        tipe_proses,
        tanggal_pemeriksaan,
        nama_shift,
        nama_operator,
        nama_line,
        device_id,
        app_version,
        total_diperiksa,
        total_ok,
        total_ng,
        rasio_ng_global,
        status
    )
    values (
        coalesce(
            payload ->> 'kodeSesi',
            'INS-' || extract(epoch from now())::bigint || '-' || substr(gen_random_uuid()::text, 1, 8)
        ),
        tipe,
        coalesce(nullif(payload ->> 'tanggalPemeriksaan', '')::date, current_date),
        coalesce(nullif(payload ->> 'namaShift', ''), 'SHIFT_1'),
        nullif(payload ->> 'namaOperator', ''),
        nullif(payload ->> 'namaLine', ''),
        nullif(payload ->> 'deviceId', ''),
        nullif(payload ->> 'appVersion', ''),
        total_diperiksa,
        total_ok,
        total_ng,
        case when total_diperiksa > 0 then round(total_ng::numeric / total_diperiksa::numeric * 100, 3) else 0 end,
        'TERKIRIM'
    )
    returning id into sesi_id;

    for item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftarPart', '[]'::jsonb))
    loop
        if not exists (
            select 1
            from public.m_part p
            where p.uniq_no = item ->> 'uniqNo'
              and p.aktif = true
              and p.komoditas = tipe
        ) then
            raise exception 'Part % tidak sesuai dengan proses %.', item ->> 'uniqNo', tipe;
        end if;

        if coalesce(nullif(item ->> 'jumlahDiperiksa', '')::int, 0) > 0
           or coalesce(nullif(item ->> 'jumlahNg', '')::int, 0) > 0
        then
            insert into public.e_item_checksheet (
                id_sesi,
                uniq_no,
                jumlah_diperiksa,
                jumlah_ok,
                jumlah_ng,
                rasio_ng,
                catatan
            )
            values (
                sesi_id,
                item ->> 'uniqNo',
                coalesce(nullif(item ->> 'jumlahDiperiksa', '')::int, 0),
                coalesce(nullif(item ->> 'jumlahOk', '')::int, 0),
                coalesce(nullif(item ->> 'jumlahNg', '')::int, 0),
                coalesce(nullif(item ->> 'rasioNg', '')::numeric, 0),
                nullif(item ->> 'catatan', '')
            )
            returning id into item_id;

            for defect_item in
                select * from jsonb_array_elements(coalesce(item -> 'daftarDefectNg', '[]'::jsonb))
            loop
                if coalesce(nullif(defect_item ->> 'jumlahNg', '')::int, 0) > 0 then
                    insert into public.e_defect_checksheet (
                        id_item,
                        id_defect,
                        nama_defect_snapshot,
                        kategori,
                        jumlah
                    )
                    values (
                        item_id,
                        defect_item ->> 'idDefect',
                        coalesce(defect_item ->> 'namaDefect', '-'),
                        coalesce(defect_item ->> 'kategori', 'PROSES'),
                        coalesce(nullif(defect_item ->> 'jumlahNg', '')::int, 0)
                    )
                    returning id into defect_row_id;

                    for slot_item in
                        select * from jsonb_array_elements(coalesce(defect_item -> 'detailSlot', '[]'::jsonb))
                    loop
                        if coalesce(nullif(slot_item ->> 'jumlah', '')::int, 0) > 0 then
                            insert into public.e_defect_slot_checksheet (
                                id_defect_checksheet,
                                slot_waktu_id,
                                jumlah
                            )
                            values (
                                defect_row_id,
                                nullif(slot_item ->> 'slotId', '')::uuid,
                                coalesce(nullif(slot_item ->> 'jumlah', '')::int, 0)
                            );
                        end if;
                    end loop;
                end if;
            end loop;
        end if;
    end loop;

    return sesi_id;
end;
$$;

grant execute on function public.rpc_submit_checksheet(jsonb) to anon, authenticated;

-- =========================================================
-- 12. RPC SUBMIT CUTTING
-- Supports old payload and new cm-based NG/waste payload.
-- =========================================================

create or replace function public.rpc_submit_cutting_batch(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    sesi_id uuid;
    batch_id uuid;
    defect_item jsonb;

    material_uuid uuid;
    size_ref_uuid uuid;
    ukuran_cm numeric;
    qty_layer_ok_value int;
    qty_layer_ng_value int;
    panjang_ok_value numeric;
    panjang_ng_value numeric;
    waste_value numeric;
    panjang_roll_value numeric;
begin
    material_uuid := (payload ->> 'material_id')::uuid;
    size_ref_uuid := nullif(payload ->> 'size_reference_id', '')::uuid;

    ukuran_cm := coalesce(
        nullif(payload ->> 'size_cutting_cm', '')::numeric,
        nullif(payload ->> 'ukuran_cutting_cm', '')::numeric
    );

    if ukuran_cm is null or ukuran_cm <= 0 then
        raise exception 'Ukuran cutting harus lebih besar dari nol.';
    end if;

    qty_layer_ok_value := coalesce(nullif(payload ->> 'qty_layer_ok', '')::int, 0);
    qty_layer_ng_value := coalesce(nullif(payload ->> 'qty_layer_ng', '')::int, 0);

    panjang_ok_value := coalesce(
        nullif(payload ->> 'panjang_ok_cm', '')::numeric,
        ukuran_cm * qty_layer_ok_value
    );

    panjang_ng_value := coalesce(
        nullif(payload ->> 'panjang_ng_cm', '')::numeric,
        ukuran_cm * qty_layer_ng_value
    );

    waste_value := coalesce(
        nullif(payload ->> 'waste_panjang_cm', '')::numeric,
        nullif(payload ->> 'panjang_waste_cm', '')::numeric,
        0
    );

    panjang_roll_value := nullif(payload ->> 'panjang_roll_awal_cm', '')::numeric;

    if qty_layer_ok_value < 0 or qty_layer_ng_value < 0 or panjang_ng_value < 0 or waste_value < 0 then
        raise exception 'Nilai OK, NG, dan waste tidak boleh negatif.';
    end if;

    if panjang_ok_value <= 0 and panjang_ng_value <= 0 then
        raise exception 'Minimal ada hasil potong OK atau NG.';
    end if;

    insert into public.e_sesi_checksheet (
        tipe_proses,
        tanggal_pemeriksaan,
        nama_shift,
        nama_operator,
        nama_line,
        total_diperiksa,
        total_ok,
        total_ng,
        rasio_ng_global,
        status
    )
    values (
        'CUTTING',
        coalesce(nullif(payload ->> 'tanggal_pemeriksaan', '')::date, current_date),
        coalesce(nullif(payload ->> 'nama_shift', ''), 'SHIFT_1'),
        nullif(payload ->> 'nama_operator', ''),
        nullif(payload ->> 'nama_line', ''),
        qty_layer_ok_value + qty_layer_ng_value,
        qty_layer_ok_value,
        qty_layer_ng_value,
        case
            when (panjang_ok_value + panjang_ng_value + waste_value) > 0
                then round(panjang_ng_value / (panjang_ok_value + panjang_ng_value + waste_value) * 100, 3)
            else 0
        end,
        'TERKIRIM'
    )
    returning id into sesi_id;

    insert into public.e_cutting_batch (
        id_sesi,
        material_id,
        size_reference_id,
        nama_material_snapshot,
        spec_material_snapshot,
        no_lot_roll,
        nomor_lot_roll,
        no_roll,
        size_cutting_cm,
        ukuran_cutting_cm,
        qty_layer_ok,
        qty_layer_ng,
        panjang_ok_cm,
        panjang_ng_cm,
        waste_panjang_cm,
        panjang_waste_cm,
        panjang_roll_awal_cm,
        ukuran_manual,
        nama_operator,
        catatan
    )
    values (
        sesi_id,
        material_uuid,
        size_ref_uuid,
        payload ->> 'nama_material_snapshot',
        nullif(payload ->> 'spec_material_snapshot', ''),
        nullif(payload ->> 'nomor_lot_roll', ''),
        nullif(payload ->> 'nomor_lot_roll', ''),
        nullif(payload ->> 'no_roll', ''),
        ukuran_cm,
        ukuran_cm,
        qty_layer_ok_value,
        qty_layer_ng_value,
        panjang_ok_value,
        panjang_ng_value,
        waste_value,
        waste_value,
        panjang_roll_value,
        size_ref_uuid is null,
        nullif(payload ->> 'nama_operator', ''),
        nullif(payload ->> 'catatan', '')
    )
    returning id into batch_id;

    -- Promote manual cutting size menjadi reference dropdown berikutnya.
    insert into public.m_cutting_size_reference (
        material_id,
        size_cutting_cm,
        ukuran_cutting_cm,
        panjang_roll_cm,
        label_ukuran,
        sumber_data,
        pemakaian_count,
        aktif
    )
    values (
        material_uuid,
        ukuran_cm,
        ukuran_cm,
        panjang_roll_value,
        round(ukuran_cm, 1)::text || ' cm',
        'FORM_HISTORY',
        1,
        true
    )
    on conflict do nothing;

    for defect_item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftar_defect', '[]'::jsonb))
    loop
        if coalesce(nullif(defect_item ->> 'panjang_defect_cm', '')::numeric, 0) > 0
           or coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0) > 0
        then
            insert into public.e_cutting_defect_detail (
                id_cutting_batch,
                id_defect,
                nama_defect_snapshot,
                slot_waktu_id,
                jumlah_layer,
                jumlah_layer_terdampak,
                panjang_defect_cm,
                persentase_dari_ng,
                catatan
            )
            values (
                batch_id,
                defect_item ->> 'id_defect',
                coalesce(defect_item ->> 'nama_defect_snapshot', '-'),
                nullif(defect_item ->> 'slot_waktu_id', '')::uuid,
                coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0),
                coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0),
                coalesce(nullif(defect_item ->> 'panjang_defect_cm', '')::numeric, 0),
                case
                    when panjang_ng_value > 0
                        then round(coalesce(nullif(defect_item ->> 'panjang_defect_cm', '')::numeric, 0) / panjang_ng_value * 100, 3)
                    else 0
                end,
                nullif(defect_item ->> 'catatan', '')
            );
        end if;
    end loop;

    return sesi_id;
end;
$$;

grant execute on function public.rpc_submit_cutting_batch(jsonb) to anon, authenticated;

-- =========================================================
-- 13. SAFE MASTER CRUD RPC - CORE GUARDS
-- UI tetap wajib confirmation dialog.
-- DB juga mencegah soft delete yang merusak input aktif.
-- =========================================================

create or replace function public.rpc_soft_delete_master(
    nama_tabel text,
    id_key text,
    alasan text default null,
    actor text default null
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
    affected int;
begin
    if nama_tabel not in ('m_part','m_material','m_supplier','m_defect') then
        raise exception 'Tabel master tidak didukung.';
    end if;

    if nama_tabel = 'm_part' and exists (
        select 1
        from public.e_item_checksheet
        where uniq_no = id_key
        limit 1
    ) then
        raise exception 'Part sudah memiliki transaksi. Gunakan nonaktif, bukan hapus permanen.';
    end if;

    if nama_tabel = 'm_material' and exists (
        select 1
        from public.e_cutting_batch
        where material_id = id_key::uuid
        limit 1
    ) then
        raise exception 'Material sudah memiliki transaksi cutting. Gunakan nonaktif, bukan hapus permanen.';
    end if;

    if nama_tabel = 'm_part' then
        update public.m_part set aktif = false, diperbarui_pada = now() where uniq_no = id_key;
    elsif nama_tabel = 'm_material' then
        update public.m_material set aktif = false, diperbarui_pada = now() where id = id_key::uuid;
    elsif nama_tabel = 'm_supplier' then
        update public.m_supplier set aktif = false, diperbarui_pada = now() where id = id_key::uuid;
    elsif nama_tabel = 'm_defect' then
        update public.m_defect set aktif = false, diperbarui_pada = now() where id_defect = id_key;
    end if;

    get diagnostics affected = row_count;

    insert into public.audit_master_data (
        tabel,
        aksi,
        entity_key,
        alasan,
        actor
    )
    values (
        nama_tabel,
        'DELETE',
        id_key,
        alasan,
        actor
    );

    perform public.f_touch_master_revision();

    return affected > 0;
end;
$$;

grant execute on function public.rpc_soft_delete_master(text, text, text, text) to anon, authenticated;

-- =========================================================
-- 14. POSTGREST CACHE REFRESH
-- =========================================================

select public.f_touch_data_revision('MASTER_DATA');
select public.f_touch_data_revision('CHECKSHEET_REFERENCE');
select public.f_touch_data_revision('CUTTING_REFERENCE');
select pg_notify('pgrst', 'reload schema');

commit;
