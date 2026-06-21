-- Next-phase MVP InSpectra.
-- Sumber seed: Google Drive Part list.xlsx, diperbarui 2025-11-26.
-- Workbook tidak disimpan di repository. Komoditas yang tidak ada pada sumber
-- dicatat sebagai PASS_THROUGH agar tidak masuk checksheet aktif tanpa validasi.

begin;

create table if not exists public.m_material_defect (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.m_material(id) on delete cascade,
    id_defect text not null references public.m_defect(id_defect) on delete restrict,
    urutan int not null default 1 check (urutan > 0),
    wajib_check boolean not null default true,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    unique (material_id, id_defect)
);

create table if not exists public.m_cutting_size_reference (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.m_material(id) on delete cascade,
    ukuran_cutting_cm numeric(12, 3) not null check (ukuran_cutting_cm > 0),
    urutan int not null default 1 check (urutan > 0),
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    unique (material_id, ukuran_cutting_cm)
);

create table if not exists public.e_cutting_batch (
    id uuid primary key default gen_random_uuid(),
    id_sesi uuid not null references public.e_sesi_checksheet(id) on delete cascade,
    material_id uuid not null references public.m_material(id) on delete restrict,
    nama_material_snapshot text not null,
    spec_material_snapshot text,
    no_lot_roll text,
    no_roll text,
    ukuran_cutting_cm numeric(12, 3) not null check (ukuran_cutting_cm > 0),
    qty_layer_ok int not null default 0 check (qty_layer_ok >= 0),
    qty_layer_ng int not null default 0 check (qty_layer_ng >= 0),
    waste_panjang_cm numeric(12, 3) not null default 0 check (waste_panjang_cm >= 0),
    nama_operator text,
    catatan text,
    dibuat_pada timestamptz not null default now(),
    constraint chk_cutting_batch_layer check (qty_layer_ok + qty_layer_ng > 0)
);

create table if not exists public.e_cutting_defect_detail (
    id uuid primary key default gen_random_uuid(),
    id_cutting_batch uuid not null references public.e_cutting_batch(id) on delete cascade,
    id_defect text not null references public.m_defect(id_defect) on delete restrict,
    nama_defect_snapshot text not null,
    slot_waktu_id uuid references public.m_slot_waktu(id) on delete restrict,
    jumlah_layer_terdampak int not null check (jumlah_layer_terdampak > 0),
    panjang_defect_cm numeric(12, 3) check (panjang_defect_cm is null or panjang_defect_cm > 0),
    dibuat_pada timestamptz not null default now()
);

create index if not exists idx_m_material_defect_material on public.m_material_defect(material_id) where aktif = true;
create index if not exists idx_e_cutting_batch_sesi on public.e_cutting_batch(id_sesi);
create index if not exists idx_e_cutting_defect_batch on public.e_cutting_defect_detail(id_cutting_batch);

insert into public.m_slot_waktu (kode_slot, tipe_proses, nama_shift, label_waktu, urutan)
values
    ('CUTTING_SHIFT_1_SLOT_1', 'CUTTING', 'SHIFT_1', '08.00 - 09.00', 1),
    ('CUTTING_SHIFT_1_SLOT_2', 'CUTTING', 'SHIFT_1', '09.00 - 10.00', 2),
    ('CUTTING_SHIFT_1_SLOT_3', 'CUTTING', 'SHIFT_1', '10.00 - 11.00', 3),
    ('CUTTING_SHIFT_1_SLOT_4', 'CUTTING', 'SHIFT_1', '11.00 - 12.00', 4),
    ('CUTTING_SHIFT_1_SLOT_5', 'CUTTING', 'SHIFT_1', '13.00 - 14.00', 5),
    ('CUTTING_SHIFT_1_SLOT_6', 'CUTTING', 'SHIFT_1', '14.00 - 15.00', 6),
    ('CUTTING_SHIFT_1_SLOT_7', 'CUTTING', 'SHIFT_1', '15.00 - 16.00', 7),
    ('CUTTING_SHIFT_1_SLOT_8', 'CUTTING', 'SHIFT_1', '16.00 - 17.00', 8)
on conflict (kode_slot) do update set
    label_waktu = excluded.label_waktu,
    urutan = excluded.urutan,
    aktif = true;

create or replace view public.v_cutting_material_option as
select
    m.id as material_id,
    m.nama_material,
    coalesce(m.spec_ringkas, '') as spec_ringkas,
    m.satuan::text as satuan,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'id', ukuran.id,
                'ukuran_cutting_cm', ukuran.ukuran_cutting_cm,
                'urutan', ukuran.urutan
            ) order by ukuran.urutan, ukuran.ukuran_cutting_cm
        ) filter (where ukuran.id is not null),
        '[]'::jsonb
    ) as daftar_ukuran_cutting
from public.m_material m
left join public.m_cutting_size_reference ukuran
    on ukuran.material_id = m.id and ukuran.aktif = true
where m.aktif = true
group by m.id, m.nama_material, m.spec_ringkas, m.satuan;

create or replace view public.v_cutting_daily_summary as
select
    sesi.id as id_sesi,
    sesi.tanggal_pemeriksaan,
    sesi.nama_shift,
    sesi.nama_line,
    count(batch.id) as total_batch,
    coalesce(sum(batch.qty_layer_ok), 0) as total_layer_ok,
    coalesce(sum(batch.qty_layer_ng), 0) as total_layer_ng,
    coalesce(sum(batch.ukuran_cutting_cm * batch.qty_layer_ok), 0) as total_panjang_ok_cm,
    coalesce(sum(batch.ukuran_cutting_cm * batch.qty_layer_ng), 0) as total_panjang_ng_cm,
    coalesce(sum(batch.waste_panjang_cm), 0) as total_waste_cm,
    case when coalesce(sum(batch.qty_layer_ok + batch.qty_layer_ng), 0) = 0 then 0
         else round(
            sum(batch.qty_layer_ng)::numeric /
            sum(batch.qty_layer_ok + batch.qty_layer_ng)::numeric * 100,
            3
         ) end as rasio_ng_layer,
    case when coalesce(sum(batch.ukuran_cutting_cm * (batch.qty_layer_ok + batch.qty_layer_ng) + batch.waste_panjang_cm), 0) = 0 then 0
         else round(
            sum(batch.waste_panjang_cm) /
            sum(batch.ukuran_cutting_cm * (batch.qty_layer_ok + batch.qty_layer_ng) + batch.waste_panjang_cm) * 100,
            3
         ) end as rasio_waste_panjang
from public.e_sesi_checksheet sesi
left join public.e_cutting_batch batch on batch.id_sesi = sesi.id
where sesi.tipe_proses = 'CUTTING'
group by sesi.id, sesi.tanggal_pemeriksaan, sesi.nama_shift, sesi.nama_line;

create or replace view public.v_checksheet_part_defect as
with defect_part as (
    select pd.uniq_no, pd.id_defect, pd.urutan
    from public.m_part_defect pd
    where pd.aktif = true
    union all
    select pm.uniq_no, md.id_defect, md.urutan
    from public.m_part_material pm
    join public.m_material_defect md
        on md.material_id = pm.material_id and md.aktif = true
    where pm.aktif = true
), defect_part_unik as (
    select uniq_no, id_defect, min(urutan) as urutan
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
                'urutan', du.urutan
            ) order by du.urutan, d.nama_defect
        ) filter (where d.id_defect is not null),
        '[]'::jsonb
    ) as daftar_defect
from public.m_part p
left join defect_part_unik du on du.uniq_no = p.uniq_no
left join public.m_defect d on d.id_defect = du.id_defect and d.aktif = true
where p.aktif = true and p.komoditas in ('PRESS', 'SEWING', 'CUTTING')
group by
    p.uniq_no, p.part_no, p.nama_part, p.model, p.customer, p.komoditas,
    p.lokasi_gambar, p.total_item_per_kanban, p.sample_item_per_kanban;

-- Sumber transformasi statis: PARTLIST.xlsx lokal, dibaca 2026-06-21.
-- BTI: 78 master part. Sheet1: 92 relasi material; 86 relasi memiliki master part BTI.
-- Enam relasi BY18/BY19 tidak masuk m_part_material karena master part belum tersedia,
-- tetapi material, pemasok, spesifikasi, dan defect materialnya tetap dipetakan.
-- Nilai BELUM DITEMUKAN POTENSI bukan defect operasional dan sengaja tidak ditulis ke m_defect.
-- Satuan Cain dipertahankan pada keterangan spesifikasi dan dipetakan ke UNKNOWN.
-- Workbook tidak disimpan atau diimpor saat runtime.

create temporary table seed_partlist_part (
    uniq_no text primary key,
    part_no text,
    nama_part text not null,
    model text,
    customer text,
    komoditas text not null,
    lokasi_gambar text
) on commit drop;

insert into seed_partlist_part (uniq_no, part_no, nama_part, model, customer, komoditas, lokasi_gambar)
values
    ('2529', '58815-KK010-00', 'CARPET CONSOLE BOX (EXP)', 'EXP', 'BONECOM TRICOM', 'PRESS', null),
    ('3Z0', 'CUTER-D6765', 'CUTTER KERTAS SDI 6765', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('4Q7', 'JB001-A0075', 'MASKER JILBAB', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('9Y6', '11119-A9310-A', 'FELT AFTER LAMINATING', '660A/650', 'BONECOM TRICOM', 'MATERIAL', null),
    ('A47', 'K9905-K0001', 'SPUNDBOND 75 GSM BLACK', 'IMV', 'BONECOM TRICOM', 'CONSUMABLE', null),
    ('B35', '71695-VT070-C', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null),
    ('B50', 'RIM01-5601B', 'FELT CLOTH 560B', '560B', 'RAVALIA INTI MANDIRI', 'PRESS', null),
    ('B51', '71695-VT090-B', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null),
    ('B55', '71695-VT110-D', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null),
    ('B63', '71695-VT080', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null),
    ('B70', '71695-VT100-B', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null),
    ('B72', '71695-VT120-D', 'PROTECTOR, RR SEAT BACK', '560B', 'BONECOM TRICOM', 'SEWING', null),
    ('BJ1', '79977-BZ020', 'FELT SEAT BACK', 'D26A', 'BONECOM TRICOM', 'SEWING', null),
    ('BM7', '71651-BZ020', 'PAD RR SEAT BACK RH', 'GSK', 'BONECOM TRICOM', 'PRESS', null),
    ('BM8', '71652-BZ020', 'PAD RR SEAT BACK LH', 'GSK', 'BONECOM TRICOM', 'PRESS', null),
    ('BT136', '11101-A1211-00', 'FELT FRONT BACK RH', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT137', '11102-A1211-00', 'FELT FRONT BACK RH', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT138', '71781-X7H01-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT139', '71781-X7H02-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT140', '71781-X7H03-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT141', '71781-X7H04-00', 'STRAP, SEAT COVER', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT144', '12101-A1211-00', 'FELT FRONT BACK LH', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT18', '72996-X7H00', 'CARPET RR SEAT NO. 2 RH', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT19', '71997-X7H00', 'CARPET RR SEAT NO. 2 LH', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT200', '13119-A1212', 'FELT BENCH 6:4', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT201', '13120-A1212', 'FELT 60%', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT202', '13121-A1212', 'FELT 40%', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT203', '13122-A1212', 'FELT 24*188', '560B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT262', '71782-X7U06', 'STRAP SEAT COVER', 'D03B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT263', '71782-X7U07', 'STRAP SEAT COVER', 'D03B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT264', '71782-X7U08', 'STRAP SEAT COVER', 'D03B', 'BONECOM TRICOM', 'PRESS', null),
    ('BT332', '78253-X7U00', 'PAD RR SEAT CTR ARMREST (ARTHA)', 'D03B', 'BONECOM TRICOM', 'PASS_THROUGH', null),
    ('BY4', '12115-A1012', 'FELT FRONT BACK LH (LOW - G)', '660A', 'BONECOM TRICOM', 'PRESS', null),
    ('CB3', '58612-A1016', 'PAD FR DOOR SILINCER', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('CB9', '58815-KK010-00', 'CARPET CONSOLE BOX', '660A', 'BONECOM TRICOM', 'PRESS', null),
    ('CH8', '71518-X7H01', 'CLOTH SEAT CUSH, UNDER', '560B', 'RAJAWALI MITRA PRATAMA', 'PRESS', null),
    ('CH9', '57902-T0024', 'QUEENSCORD', '660', 'BONECOM TRICOM', 'CONSUMABLE', null),
    ('CL1', '7997A-X7V05', 'CLAF 1', 'D74', 'BONECOM TRICOM', 'PRESS', null),
    ('CL2', '7997A-X7V04', 'CLAF 2', 'D74', 'BONECOM TRICOM', 'PRESS', null),
    ('DN5', '11115-A1012', 'FELT FRONT BACK RH ( LOW- G', '660A', 'BONECOM TRICOM', 'PRESS', null),
    ('EE5', 'HD004-W2200', 'PLASTIC HD', '800A', 'BONECOM TRICOM', 'CONSUMABLE', null),
    ('EE8', 'PMWHT-STSG3', 'PAINT MARKING SHACHIHATA WHITE STSG 3', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('EE9', 'THSOL-00331', 'SOLVEN SHACHIHATA 5DL-3-31', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('EQ5', '79117-0K060-A', 'CARPET NO. 1 SEAT CUSHION', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('ER2', '67812-X7A07', 'PAD FR DOOR SILINCER', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('FE7', '71781-X7A35-A', 'STRAP SEAT COVER', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('FE8', '71781-X7A36', 'STRAP SEAT COVER', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('FJ0', '71075-F1V01', 'PAD SETTEN RH', 'D14N', 'BONECOM TRICOM', 'PRESS', null),
    ('FJ1', '71075-F1V02', 'PAD SETTEN LH', 'D14N', 'BONECOM TRICOM', 'PRESS', null),
    ('FJ2', '71075-F1V03', 'USHIRO RH', 'D14N', 'BONECOM TRICOM', 'PRESS', null),
    ('FJ3', '71075-F1V04', 'USHIRO LH', 'D14N', 'BONECOM TRICOM', 'PRESS', null),
    ('FJ4', '71075-F1V05', 'BOTTOM RH', 'D14N', 'BONECOM TRICOM', 'PRESS', null),
    ('FJ5', '71075-F1V06', 'BOTTOM LH', 'D14N', 'BONECOM TRICOM', 'PRESS', null),
    ('FJ6', '71075-F1V07', 'KOTAK USHIRO', 'D14N', 'BONECOM TRICOM', 'PRESS', null),
    ('FP7', '11127-A1042', 'FELT FR RH', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('FP8', '12127-A1042', 'FELT FR LH', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('FP9', '13115-A1042', 'FELT ( 40%)', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('FQ0', '14137-A1044', 'FELT 60%', '650A', 'BONECOM TRICOM', 'PRESS', null),
    ('FS2', 'AD003-REC01', 'APRON DADA RECLEANING', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('FS3', 'LAK02-BLACK', 'LAKBAN KAIN 2" BLACK', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('ID-121', '58815-RAW00', 'CARPET CBIII CONSOLE BOX', '660A/650', 'BONECOM TRICOM', 'MATERIAL', null),
    ('JP6', '79976-X1V86-00', 'FELT', '230/231B', 'BONECOM TRICOM', 'PRESS', null),
    ('JQ9', '67811-X1V82', 'PAD FR DOOR SILINCER T5 D 200GR', '230/231B', 'BONECOM TRICOM', 'PRESS', null),
    ('JR0', '67812-X1V59', 'PAD RR DOOR SILINCER T5 D 200GR', '230/231B', 'BONECOM TRICOM', 'PRESS', null),
    ('JR1', '67812-X1V60', 'PAD FR DOOR SILINCER T5 D 200GR', '230/231B', 'BONECOM TRICOM', 'PRESS', null),
    ('JS5', '11123-A1062', 'FELT', '230/231B', 'BONECOM TRICOM', 'PRESS', null),
    ('JY8', '12127-A2899', 'FELT LH T2.5 4009 + LDPE 200gr', '230/231B', 'BONECOM TRICOM', 'PRESS', null),
    ('JZ2', '11127-A2899', 'FELT LH T2.5 4009 + LDPE 200gr', '230/231B', 'BONECOM TRICOM', 'PRESS', null),
    ('ML0', '58611-A1012', 'INSULATION SHEET NO. 2', '660A/650', 'BONECOM TRICOM', 'PRESS', null),
    ('ML1', '58611-A1013', 'INSULATION SHEET NO. 3', 'FMC', 'BONECOM TRICOM', 'PRESS', null),
    ('MM1', '58611-A1011', 'INSULATION SHEET NO. 1', '660A/650', 'BONECOM TRICOM', 'PRESS', null),
    ('MN9', '58611-A1015', 'INSULATION SHEET NO. 5/FT8', 'EXP', 'BONECOM TRICOM', 'PRESS', null),
    ('MO2', '58611-A1014', 'INSULATION SHEET NO. 4/FT7', 'EXP', 'BONECOM TRICOM', 'PRESS', null),
    ('P56', 'RIM01-5602B', 'STRAP 560B', '560B', 'RAVALIA INTI MANDIRI', 'PRESS', null),
    ('RM5', '71518-X7U19', 'CLOTH FR SEAT CUSH UNDER', 'D03B', 'RAJAWALI MITRA PRATAMA', 'PRESS', null),
    ('Y20', 'AT002-A0068', 'APRON TANGAN BIRU', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('Y21', 'AD002-A0069', 'APRON DADA BIRU', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null),
    ('Y22', 'AK001-A0070', 'APRON KAKI', 'CONS', 'RAVALIA INTI MANDIRI', 'CONSUMABLE', null)
;

create temporary table seed_partlist_relasi (
    baris_sumber int primary key,
    uniq_no text not null,
    nama_material text not null,
    nama_supplier text,
    defect_material text,
    lebar text,
    panjang text,
    tebal text,
    berat text,
    qty text,
    satuan_sumber text,
    spec_asli text
) on commit drop;

insert into seed_partlist_relasi (
    baris_sumber, uniq_no, nama_material, nama_supplier, defect_material,
    lebar, panjang, tebal, berat, qty, satuan_sumber, spec_asli
)
values
    (11, 'BJ1', 'Recycle Felt GWPS 2mm 375 Gsm', 'PT. LANI TEDUH (BTI)', 'SOBEK | BRUDUL | TIPIS', '1,80', '50,00', '2,00', '375', null, 'Roll', '1,8 X 50'),
    (12, 'BJ1', 'PS Polyester Non Woven Spunbond 100 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'SPUNBOUND TIDAK MEREKAT | SPUNBOUND KOTOR | SPUNDBOUND TERLIPAT | SPUNDBOND HARDEN', '1,50', '50,00', null, '100', null, 'Roll', '1,5 X 50'),
    (13, 'BJ1', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '0,66', '50,00', null, '200', null, 'Roll', '0,66 X 50'),
    (14, 'BJ1', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '1,14', '50,00', null, '200', null, 'Roll', '1,14 X 50'),
    (15, 'BJ1', 'Carpet Assy Neddle D26', 'PT. FAM', null, null, null, null, null, '1', 'Pcs', '1'),
    (16, 'BT200', 'FELT BENCH 6:4', null, null, null, null, null, null, null, null, null),
    (17, 'B35', 'Recycle Felt GWPS 2mm 375 Gsm', 'PT. LANI TEDUH (BTI)', 'SOBEK | BRUDUL | TIPIS', '1,80', '50,00', '2,00', '375', null, 'Roll', '1,8 X 50'),
    (18, 'B35', 'PS Polyester Non Woven Spunbond 100 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'SPUNBOUND TIDAK MEREKAT | SPUNBOUND KOTOR | SPUNDBOUND TERLIPAT | SPUNDBOND HARDEN', '1,50', '50,00', null, '100', null, 'Roll', '1,5 X 50'),
    (19, 'B35', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '0,66', '50,00', null, '200', null, 'Roll', '0,66 X 50'),
    (20, 'B35', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '1,14', '50,00', null, '200', null, 'Roll', '1,14 X 50'),
    (21, 'B35', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (22, 'B35', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', null, null, null, null, null, '1', 'Pcs', '1'),
    (23, 'B35', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (24, 'BT202', 'FELT 40%', null, null, null, null, null, null, null, null, null),
    (25, 'B63', 'Recycle Felt GWPS 2mm 375 Gsm', 'PT. LANI TEDUH (BTI)', 'SOBEK | BRUDUL | TIPIS', '1,80', '50,00', '2,00', '375', null, 'Roll', '1,8 X 50'),
    (26, 'B63', 'PS Polyester Non Woven Spunbond 100 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'SPUNBOUND TIDAK MEREKAT | SPUNBOUND KOTOR | SPUNDBOUND TERLIPAT | SPUNDBOND HARDEN', '1,50', '50,00', null, '100', null, 'Roll', '1,5 X 50'),
    (27, 'B63', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '0,66', '50,00', null, '200', null, 'Roll', '0,66 X 50'),
    (28, 'B63', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '1,14', '50,00', null, '200', null, 'Roll', '1,14 X 50'),
    (29, 'B63', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (30, 'B63', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', null, null, null, null, null, '1', 'Pcs', '1'),
    (31, 'B63', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (32, 'BT203', 'FELT 24*188', null, null, null, null, null, null, null, null, null),
    (33, 'B55', 'Recycle Felt GWPS 2mm 375 Gsm', 'PT. LANI TEDUH (BTI)', 'SOBEK | BRUDUL | TIPIS', '1,80', '50,00', '2,00', '375', null, 'Roll', '1,8 X 50'),
    (34, 'B55', 'PS Polyester Non Woven Spunbond 100 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'SPUNBOUND TIDAK MEREKAT | SPUNBOUND KOTOR | SPUNDBOUND TERLIPAT | SPUNDBOND HARDEN', '1,50', '50,00', null, '100', null, 'Roll', '1,5 X 50'),
    (35, 'B55', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '0,66', '50,00', null, '200', null, 'Roll', '0,66 X 50'),
    (36, 'B55', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '1,14', '50,00', null, '200', null, 'Roll', '1,14 X 50'),
    (37, 'B55', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (38, 'B55', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', null, null, null, null, null, '1', 'Pcs', '1'),
    (39, 'B55', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (40, 'FJ1', 'PAD SETTEN LH', null, null, null, null, null, null, null, null, null),
    (41, 'B72', 'Recycle Felt GWPS 2mm 375 Gsm', 'PT. LANI TEDUH (BTI)', 'SOBEK | BRUDUL | TIPIS', '1,80', '50,00', '2,00', '375', null, 'Roll', '1,8 X 50'),
    (42, 'B72', 'PS Polyester Non Woven Spunbond 100 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'SPUNBOUND TIDAK MEREKAT | SPUNBOUND KOTOR | SPUNDBOUND TERLIPAT | SPUNDBOND HARDEN', '1,50', '50,00', null, '100', null, 'Roll', '1,5 X 50'),
    (43, 'B72', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '0,66', '50,00', null, '200', null, 'Roll', '0,66 X 50'),
    (44, 'B72', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '1,14', '50,00', null, '200', null, 'Roll', '1,14 X 50'),
    (45, 'B72', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (46, 'B72', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', null, null, null, null, null, '1', 'Pcs', '1'),
    (47, 'B72', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (48, 'FJ0', 'PAD SETTEN RH', null, null, null, null, null, null, null, null, null),
    (49, 'B51', 'Recycle Felt GWPS 2mm 375 Gsm', 'PT. LANI TEDUH (BTI)', 'SOBEK | BRUDUL | TIPIS', '1,80', '50,00', '2,00', '375', null, 'Roll', '1,8 X 50'),
    (50, 'B51', 'PS Polyester Non Woven Spunbond 100 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'SPUNBOUND TIDAK MEREKAT | SPUNBOUND KOTOR | SPUNDBOUND TERLIPAT | SPUNDBOND HARDEN', '1,50', '50,00', null, '100', null, 'Roll', '1,5 X 50'),
    (51, 'B51', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '0,66', '50,00', null, '200', null, 'Roll', '0,66 X 50'),
    (52, 'B51', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '1,14', '50,00', null, '200', null, 'Roll', '1,14 X 50'),
    (53, 'B51', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (54, 'B51', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', null, null, null, null, null, '1', 'Pcs', '1'),
    (55, 'B51', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (56, 'BT136', 'FELT FRONT BACK RH', null, null, null, null, null, null, null, null, null),
    (57, 'B70', 'Recycle Felt GWPS 2mm 375 Gsm', 'PT. LANI TEDUH (BTI)', 'SOBEK | BRUDUL | TIPIS', '1,80', '50,00', '2,00', '375', null, 'Roll', '1,8 X 50'),
    (58, 'B70', 'PS Polyester Non Woven Spunbond 100 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'SPUNBOUND TIDAK MEREKAT | SPUNBOUND KOTOR | SPUNDBOUND TERLIPAT | SPUNDBOND HARDEN', '1,50', '50,00', null, '100', null, 'Roll', '1,5 X 50'),
    (59, 'B70', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '0,66', '50,00', null, '200', null, 'Roll', '0,66 X 50'),
    (60, 'B70', 'Laminasi LDPE 200 Gsm', 'PT. ARTHA LANGGENG MULYA (BTI)', 'LAMINATING BOLONG | LAMINATING TIDAK MATANG', '1,14', '50,00', null, '200', null, 'Roll', '1,14 X 50'),
    (61, 'B70', 'Indication Tag Tafeta Felt PE , PET', 'PT. NATIONAL LABEL', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (62, 'B70', 'Hook seat Cover 72752-X1V08', 'PT. INDAH VARIA EKA SELARAS', null, null, null, null, null, '1', 'Pcs', '1'),
    (63, 'B70', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (64, 'BT137', 'FELT FRONT BACK RH', null, null, null, null, null, null, null, null, null),
    (65, 'BY18', 'Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', 'SOBEK | BRUDUL | TIPIS | BERJAMUR | GALER | DENT | TERLIPAT | BELANG', '1,45', '40,00', null, null, null, 'Roll', '1,45 X 40'),
    (66, 'BY18', 'Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (67, 'BY18', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (68, 'BT144', 'FELT FRONT BACK LH', null, null, null, null, null, null, null, null, null),
    (69, 'BY19', 'Carpet STKD19 Black', 'PT. HERCULON INDONESIA (BTI)', 'SOBEK | BRUDUL | TIPIS | BERJAMUR | GALER | DENT | TERLIPAT | BELANG', '1,45', '40,00', null, null, null, 'Roll', '1,45 X 40'),
    (70, 'BY19', 'Plate Seat Cover', 'PT. INDAH VARIA EKA SELARAS', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Pcs', '1'),
    (71, 'BY19', 'Benang Black #60', 'PT. IEM', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '1', 'Cones', '1'),
    (72, 'BM7', 'PAD RR SEAT BACK RH', null, null, null, null, null, null, null, null, null),
    (73, 'BM8', 'PAD RR SEAT BACK LH', null, null, null, null, null, null, null, null, null),
    (74, 'FP7', 'FELT FR RH', null, null, null, null, null, null, null, null, null),
    (75, 'FP8', 'FELT FR LH', null, null, null, null, null, null, null, null, null),
    (76, 'FP9', 'FELT ( 40%)', null, null, null, null, null, null, null, null, null),
    (77, 'FQ0', 'FELT 60%', null, null, null, null, null, null, null, null, null),
    (78, 'DN5', 'FELT FRONT BACK RH ( LOW- G', null, null, null, null, null, null, null, null, null),
    (79, 'BY4', 'FELT FRONT BACK LH (LOW - G)', null, null, null, null, null, null, null, null, null),
    (80, 'JS5', 'FELT', null, null, null, null, null, null, null, null, null),
    (81, 'BT138', 'STRAP, SEAT COVER', null, null, null, null, null, null, null, null, null),
    (82, 'BT139', 'STRAP, SEAT COVER', null, null, null, null, null, null, null, null, null),
    (83, 'BT140', 'STRAP, SEAT COVER', null, null, null, null, null, null, null, null, null),
    (84, 'BT141', 'STRAP, SEAT COVER', null, null, null, null, null, null, null, null, null),
    (85, 'CH8', 'CLOTH SEAT CUSH, UNDER', null, null, null, null, null, null, null, null, null),
    (86, 'RM5', 'CLOTH FR SEAT CUSH UNDER', null, null, null, null, null, null, null, null, null),
    (87, 'BT262', 'STRAP SEAT COVER', null, null, null, null, null, null, null, null, null),
    (88, 'BT263', 'STRAP SEAT COVER', null, null, null, null, null, null, null, null, null),
    (89, 'BT264', 'STRAP SEAT COVER', null, null, null, null, null, null, null, null, null),
    (90, 'MM1', 'EPDM 45mm / 47mm', null, null, null, null, '45,00', null, null, null, null),
    (91, 'MM1', 'PS Polyester Non Woven Spunbond 80 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'BELUM DITEMUKAN POTENSI', '0,45', '300,00', null, '80', null, 'Roll', '0,45 X 300'),
    (92, 'MM1', 'Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '3', 'Cain', '2,5 KG'),
    (93, 'ML0', 'EPDM 45mm / 47mm', null, null, null, null, '45,00', null, null, null, null),
    (94, 'ML0', 'PS Polyester Non Woven Spunbond 80 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'BELUM DITEMUKAN POTENSI', '0,45', '300,00', null, '80', null, 'Roll', '0,45 X 300'),
    (95, 'ML0', 'Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '3', 'Cain', '2,5 KG'),
    (96, 'ML1', 'EPDM 45mm / 47mm', null, null, null, null, '45,00', null, null, null, null),
    (97, 'ML1', 'PS Polyester Non Woven Spunbond 80 Gsm White', 'PT. HASIL DAMAI TEXTILE (BTI)', 'BELUM DITEMUKAN POTENSI', '0,45', '300,00', null, '80', null, 'Roll', '0,45 X 300'),
    (98, 'ML1', 'Lem Fox 2,5 Kg', 'PT. RAJAWALI MITRA PRATAMA', 'BELUM DITEMUKAN POTENSI', null, null, null, null, '3', 'Cain', '2,5 KG'),
    (99, 'MO2', 'INSULATION SHEET NO. 4/FT7', null, null, null, null, null, null, null, null, null),
    (100, 'MN9', 'INSULATION SHEET NO. 5/FT8', null, null, null, null, null, null, null, null, null),
    (101, 'ER2', 'PAD FR DOOR SILINCER', null, null, null, null, null, null, null, null, null),
    (102, 'CB3', 'PAD FR DOOR SILINCER', null, null, null, null, null, null, null, null, null)
;

create temporary table seed_partlist_defect (
    id_defect text primary key,
    nama_defect text not null unique
) on commit drop;

insert into seed_partlist_defect (id_defect, nama_defect)
values
    ('MAT_39C57DF4588C', 'BELANG'),
    ('MAT_2479E0F6AA8F', 'BERJAMUR'),
    ('MAT_ED02389C7186', 'BRUDUL'),
    ('MAT_8564EFBCEDA7', 'DENT'),
    ('MAT_C8047FAFC37C', 'GALER'),
    ('MAT_7CF124228B09', 'LAMINATING BOLONG'),
    ('MAT_B9585E5E1E32', 'LAMINATING TIDAK MATANG'),
    ('MAT_AA9E820D6F83', 'SOBEK'),
    ('MAT_7F3BD7AFF726', 'SPUNBOUND KOTOR'),
    ('MAT_4A18D75AFE22', 'SPUNBOUND TIDAK MEREKAT'),
    ('MAT_BE07D4CBB4D5', 'SPUNDBOND HARDEN'),
    ('MAT_A9ABF94C4452', 'SPUNDBOUND TERLIPAT'),
    ('MAT_A51AF3ABD50E', 'TERLIPAT'),
    ('MAT_A4AF7F14E1D9', 'TIPIS')
;

insert into public.m_supplier (nama_supplier, aktif)
select distinct relasi.nama_supplier, true
from seed_partlist_relasi relasi
where nullif(trim(relasi.nama_supplier), '') is not null
on conflict (nama_supplier) do update set aktif = true;

insert into public.m_part (
    uniq_no, part_no, nama_part, model, customer, komoditas, lokasi_gambar, aktif
)
select
    part.uniq_no,
    nullif(trim(part.part_no), ''),
    part.nama_part,
    nullif(trim(part.model), ''),
    nullif(trim(part.customer), ''),
    part.komoditas::public.tipe_proses_inspectra,
    nullif(trim(part.lokasi_gambar), ''),
    true
from seed_partlist_part part
on conflict (uniq_no) do update set
    part_no = excluded.part_no,
    nama_part = excluded.nama_part,
    model = excluded.model,
    customer = excluded.customer,
    komoditas = excluded.komoditas,
    lokasi_gambar = excluded.lokasi_gambar,
    aktif = true,
    diperbarui_pada = now();

update public.m_material material
set
    satuan = case upper(trim(relasi.satuan_sumber))
        when 'ROLL' then 'ROLL'::public.satuan_inspectra
        when 'PCS' then 'PCS'::public.satuan_inspectra
        when 'CONES' then 'CONES'::public.satuan_inspectra
        else 'UNKNOWN'::public.satuan_inspectra
    end,
    aktif = true,
    diperbarui_pada = now()
from seed_partlist_relasi relasi
left join public.m_supplier supplier on supplier.nama_supplier = nullif(trim(relasi.nama_supplier), '')
where material.nama_normalisasi = upper(trim(regexp_replace(relasi.nama_material, '\s+', ' ', 'g')))
  and material.supplier_id is not distinct from supplier.id
  and material.spec_ringkas is not distinct from nullif(trim(relasi.spec_asli), '');

insert into public.m_material (nama_material, supplier_id, spec_ringkas, satuan, aktif)
select distinct
    trim(regexp_replace(relasi.nama_material, '\s+', ' ', 'g')),
    supplier.id,
    nullif(trim(relasi.spec_asli), ''),
    case upper(trim(relasi.satuan_sumber))
        when 'ROLL' then 'ROLL'::public.satuan_inspectra
        when 'PCS' then 'PCS'::public.satuan_inspectra
        when 'CONES' then 'CONES'::public.satuan_inspectra
        else 'UNKNOWN'::public.satuan_inspectra
    end,
    true
from seed_partlist_relasi relasi
left join public.m_supplier supplier on supplier.nama_supplier = nullif(trim(relasi.nama_supplier), '')
where not exists (
    select 1
    from public.m_material material
    where material.nama_normalisasi = upper(trim(regexp_replace(relasi.nama_material, '\s+', ' ', 'g')))
      and material.supplier_id is not distinct from supplier.id
      and material.spec_ringkas is not distinct from nullif(trim(relasi.spec_asli), '')
);

insert into public.m_material_spec (
    material_id, spec_asli, lebar_value, panjang_value, tebal_value, berat_value,
    qty_value, qty_unit, keterangan, aktif
)
select distinct
    material.id,
    nullif(trim(relasi.spec_asli), ''),
    nullif(replace(trim(relasi.lebar), ',', '.'), '')::numeric,
    nullif(replace(trim(relasi.panjang), ',', '.'), '')::numeric,
    nullif(replace(trim(relasi.tebal), ',', '.'), '')::numeric,
    nullif(replace(trim(relasi.berat), ',', '.'), '')::numeric,
    nullif(replace(trim(relasi.qty), ',', '.'), '')::numeric,
    case upper(trim(relasi.satuan_sumber))
        when 'ROLL' then 'ROLL'::public.satuan_inspectra
        when 'PCS' then 'PCS'::public.satuan_inspectra
        when 'CONES' then 'CONES'::public.satuan_inspectra
        else 'UNKNOWN'::public.satuan_inspectra
    end,
    case when nullif(trim(relasi.satuan_sumber), '') is not null
              and upper(trim(relasi.satuan_sumber)) not in ('ROLL', 'PCS', 'CONES')
         then 'Satuan sumber: ' || trim(relasi.satuan_sumber)
         else null end,
    true
from seed_partlist_relasi relasi
left join public.m_supplier supplier on supplier.nama_supplier = nullif(trim(relasi.nama_supplier), '')
join public.m_material material
    on material.nama_normalisasi = upper(trim(regexp_replace(relasi.nama_material, '\s+', ' ', 'g')))
   and material.supplier_id is not distinct from supplier.id
   and material.spec_ringkas is not distinct from nullif(trim(relasi.spec_asli), '')
where nullif(trim(relasi.spec_asli), '') is not null
on conflict (material_id, spec_asli) do update set
    lebar_value = excluded.lebar_value,
    panjang_value = excluded.panjang_value,
    tebal_value = excluded.tebal_value,
    berat_value = excluded.berat_value,
    qty_value = excluded.qty_value,
    qty_unit = excluded.qty_unit,
    keterangan = excluded.keterangan,
    aktif = true;

insert into public.m_defect (id_defect, nama_defect, kategori, aktif)
select id_defect, nama_defect, 'MATERIAL'::public.kategori_defect_inspectra, true
from seed_partlist_defect
on conflict (id_defect) do update set
    nama_defect = excluded.nama_defect,
    kategori = excluded.kategori,
    aktif = true;

-- SEBELUM INSERT BARU: Nonaktifkan relasi part-material lama untuk part yang ada di seed ini
-- agar tidak terjadi tumpang tindih antara relasi generic (seed lama) dan relasi presisi (seed baru).
update public.m_part_material
set aktif = false
where uniq_no in (select uniq_no from seed_partlist_part);

insert into public.m_part_material (
    uniq_no, material_id, material_spec_id, urutan, label_material, aktif
)
select
    relasi.uniq_no,
    material.id,
    spec.id,
    row_number() over (partition by relasi.uniq_no order by relasi.baris_sumber)::int,
    material.nama_material,
    true
from seed_partlist_relasi relasi
join seed_partlist_part part on part.uniq_no = relasi.uniq_no
left join public.m_supplier supplier on supplier.nama_supplier = nullif(trim(relasi.nama_supplier), '')
join public.m_material material
    on material.nama_normalisasi = upper(trim(regexp_replace(relasi.nama_material, '\s+', ' ', 'g')))
   and material.supplier_id is not distinct from supplier.id
   and material.spec_ringkas is not distinct from nullif(trim(relasi.spec_asli), '')
left join public.m_material_spec spec
    on spec.material_id = material.id
   and spec.spec_asli is not distinct from nullif(trim(relasi.spec_asli), '')
where not exists (
    select 1
    from public.m_part_material pemetaan
    where pemetaan.uniq_no = relasi.uniq_no
      and pemetaan.material_id = material.id
      and pemetaan.material_spec_id is not distinct from spec.id
);

insert into public.m_material_defect (material_id, id_defect, urutan, aktif)
select material_id, id_defect,
       row_number() over (partition by material_id order by nama_defect)::int,
       true
from (
    select distinct material.id as material_id, defect.id_defect, defect.nama_defect
    from seed_partlist_relasi relasi
    left join public.m_supplier supplier on supplier.nama_supplier = nullif(trim(relasi.nama_supplier), '')
    join public.m_material material
        on material.nama_normalisasi = upper(trim(regexp_replace(relasi.nama_material, '\s+', ' ', 'g')))
       and material.supplier_id is not distinct from supplier.id
       and material.spec_ringkas is not distinct from nullif(trim(relasi.spec_asli), '')
    cross join lateral regexp_split_to_table(coalesce(relasi.defect_material, ''), '\s*\|\s*') as sumber(nama_defect)
    join seed_partlist_defect defect on defect.nama_defect = trim(sumber.nama_defect)
) pemetaan_defect
on conflict (material_id, id_defect) do update set aktif = true;


select pg_notify('pgrst', 'reload schema');

commit;

