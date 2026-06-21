-- Referensi ukuran Cutting bersumber dari FM-QA-026 rev. 2024-01-10.
-- Grain sumber adalah UNIQ NO/part; referensi ini sengaja tidak disatukan
-- dengan m_cutting_size_reference yang ber-grain material.

begin;

create table if not exists public.m_part_cutting_size_reference (
    id uuid primary key default gen_random_uuid(),
    uniq_no text not null references public.m_part(uniq_no) on delete restrict,
    size_cutting_cm numeric(12, 3) not null check (size_cutting_cm > 0),
    urutan int not null default 1 check (urutan > 0),
    nama_material_sumber text,
    part_no_sumber text,
    project_sumber text,
    sumber_data text not null,
    aktif boolean not null default true,
    dibuat_pada timestamptz not null default now(),
    diperbarui_pada timestamptz not null default now(),
    unique (uniq_no, size_cutting_cm)
);

create index if not exists idx_m_part_cutting_size_reference_part
    on public.m_part_cutting_size_reference(uniq_no, urutan)
    where aktif = true;

alter table public.e_cutting_batch
    add column if not exists uniq_no_part text references public.m_part(uniq_no) on delete restrict,
    add column if not exists nama_part_snapshot text,
    add column if not exists part_size_reference_id uuid references public.m_part_cutting_size_reference(id) on delete set null,
    add column if not exists ukuran_manual boolean not null default true;

create index if not exists idx_e_cutting_batch_part_reference
    on public.e_cutting_batch(uniq_no_part, part_size_reference_id);

-- 47 baris ukuran numerik pada sumber. CL7, CR6, dan BJ0 belum ada di m_part;
-- join berikut menolak ketiganya dengan aman. FJ2, FJ3, FJ6, RM7, serta ukuran
-- 48 cm tanpa UNIQ NO tidak dimasukkan karena bukan referensi part numerik valid.
with sumber(uniq_no, size_cutting_cm, nama_material_sumber, part_no_sumber, project_sumber, urutan) as (
    values
        ('BT136', 76.0, '115cm Fujiseat Hardfelt (9Y8)', '11101-A1211', '560B', 1),
        ('BT137', 76.0, '115cm Fujiseat Hardfelt (9Y8)', '11102-A1211', '560B', 2),
        ('BT144', 76.0, '115cm Fujiseat Hardfelt (9Y8)', '12101-A1211', '560B', 3),
        ('B50', 50.0, 'Carpet STKD-19', 'RIM01-5601B', '560B', 4),
        ('CL7', 81.5, 'Carpet STKD-19', '71997-X7H00', '560B', 5),
        ('CR6', 81.5, 'Carpet STKD-19', '72996-X7H00', '560B', 6),
        ('BT138', 39.0, 'ESTER CANVAS - STRAP', '71781-X7H01', '560B', 7),
        ('BT139', 27.0, 'ESTER CANVAS - STRAP', '71781-X7H02', '560B', 8),
        ('BT140', 40.0, 'ESTER CANVAS - STRAP', '71781-X7H03', '560B', 9),
        ('BT141', 24.5, 'ESTER CANVAS - STRAP', '71781-X7H04', '560B', 10),
        ('CH8', 31.0, 'ESTER CANVAS - STRAP', '71518-X7H01', '560B', 11),
        ('P56', 29.0, 'ESTER CANVAS - STRAP', 'RIM01-5602B', '560B', 12),
        ('B35', 109.0, 'PROTECTOR', '71695-VT070', '560B', 13),
        ('B63', 95.0, 'PROTECTOR', '71695-VT080', '560B', 14),
        ('B51', 82.0, 'PROTECTOR', '71695-VT090', '560B', 15),
        ('B55', 82.0, 'PROTECTOR', '71695-VT110', '560B', 16),
        ('B70', 82.0, 'PROTECTOR', '71695-VT100', '560B', 17),
        ('B72', 82.0, 'PROTECTOR', '71695-VT120', '560B', 18),
        ('EQ5', 36.0, 'BLACK SPUNBOND 50 GSM', '79117-0K060-A', '650/660A', 19),
        ('CB9', 63.0, 'Carpet CB-III', '58815-KK010', '650/660A', 20),
        ('FE7', 60.0, 'ESTER CANVAS - STRAP', '71781-X7A35-A', '650/660A', 21),
        ('FE8', 55.0, 'ESTER CANVAS - STRAP', '71781-X7A36', '650/660A', 22),
        ('BY4', 77.0, 'Hardfelt (9Y6)', '12115-A1012', '650/660A', 23),
        ('DN5', 77.0, 'Hardfelt (9Y6)', '11115-A1012', '650/660A', 24),
        ('FP7', 68.5, 'Hardfelt (9Y6)', '11127-A1042', '650/660A', 25),
        ('FP8', 68.5, 'Hardfelt (9Y6)', '12127-A1042', '650/660A', 26),
        ('FP9', 45.0, 'Hardfelt (9Y6)', '13115-A1042', '650/660A', 27),
        ('FQ0', 68.5, 'Hardfelt (9Y6)', '14137-A1044', '650/660A', 28),
        ('JY8', 68.5, 'Hardfelt (9Y6)', '12127-A2899', '650/660A', 29),
        ('JZ2', 68.5, 'Hardfelt (9Y6)', '11127-A2899', '650/660A', 30),
        ('JS5', 70.0, 'Hardfelt (9Y6)', '11123-A1062', '650/660A', 31),
        ('CB3', 55.0, 'SILENCER 1000 GSM (MP1)', '58612-A1016', '650/660A', 32),
        ('ER2', 55.0, 'SILENCER 1000 GSM (MP1)', '58612-A1020', '650/660A', 33),
        ('BT262', 40.0, 'ESTER CANVAS - STRAP', '71782-X7U06-A', 'D03', 34),
        ('BT263', 34.0, 'ESTER CANVAS - STRAP', '71782-X7U07-A', 'D03', 35),
        ('BT264', 34.0, 'ESTER CANVAS - STRAP', '71782-X7U08-A', 'D03', 36),
        ('RM5', 31.0, 'ESTER CANVAS - STRAP', '71518-X7U19', 'D03', 37),
        ('FJ0', 76.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V01', 'D14N', 39),
        ('FJ1', 55.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V02', 'D14N', 40),
        ('FJ4', 76.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V05', 'D14N', 43),
        ('FJ5', 76.0, 'Fujiseat Hardfelt (9Y8)', '71075-F1V06', 'D14N', 44),
        ('BM7', 76.0, 'Fujiseat Hardfelt (9Y8)', '71651-BZ020', 'D25', 46),
        ('BM8', 76.0, 'Fujiseat Hardfelt (9Y8)', '71652-BZ020', 'D25', 47),
        ('CL1', 65.0, 'CLAF', '7997A-X7U05', 'D74', 48),
        ('CL2', 85.0, 'CLAF', '7997A-X7U04', 'D74', 49),
        ('BJ0', 38.5, 'Carpet black 200GSM+Latex 50GSM (MWSB-87)', '71831-BZ150-J', 'DCWA / D72A', 50),
        ('BJ1', 50.0, 'PROTECTOR', '79977-BZO20', 'D26A', 51)
)
insert into public.m_part_cutting_size_reference (
    uniq_no,
    size_cutting_cm,
    nama_material_sumber,
    part_no_sumber,
    project_sumber,
    urutan,
    sumber_data
)
select
    sumber.uniq_no,
    sumber.size_cutting_cm,
    sumber.nama_material_sumber,
    sumber.part_no_sumber,
    sumber.project_sumber,
    sumber.urutan,
    'FM-QA-026 rev. 2024-01-10'
from sumber
join public.m_part part on part.uniq_no = sumber.uniq_no
on conflict (uniq_no, size_cutting_cm) do nothing;

create or replace view public.v_cutting_part_size_option as
select
    part.uniq_no,
    part.part_no,
    part.nama_part,
    part.model,
    part.komoditas::text as komoditas,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'id', referensi.id,
                'ukuran_cutting_cm', referensi.size_cutting_cm,
                'urutan', referensi.urutan
            ) order by referensi.urutan, referensi.size_cutting_cm
        ) filter (where referensi.id is not null),
        '[]'::jsonb
    ) as daftar_ukuran_cutting
from public.m_part part
join public.m_part_cutting_size_reference referensi
    on referensi.uniq_no = part.uniq_no
   and referensi.aktif = true
where part.aktif = true
group by part.uniq_no, part.part_no, part.nama_part, part.model, part.komoditas;

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
    total_layer int;
    ukuran_cm numeric;
    part_uniq_no text;
    part_size_reference_uuid uuid;
    referensi_part_uniq_no text;
begin
    total_layer := coalesce(nullif(payload ->> 'total_diperiksa', '')::int, 0);
    ukuran_cm := coalesce(
        nullif(payload ->> 'size_cutting_cm', '')::numeric,
        nullif(payload ->> 'ukuran_cutting_cm', '')::numeric
    );
    part_uniq_no := nullif(payload ->> 'uniq_no_part', '');
    part_size_reference_uuid := nullif(payload ->> 'part_size_reference_id', '')::uuid;

    if ukuran_cm is null or ukuran_cm <= 0 then
        raise exception 'Ukuran cutting harus lebih besar dari nol.';
    end if;

    if part_uniq_no is not null and not exists (
        select 1 from public.m_part where uniq_no = part_uniq_no
    ) then
        raise exception 'Part acuan ukuran tidak ditemukan.';
    end if;

    if part_size_reference_uuid is not null then
        select referensi.uniq_no
        into referensi_part_uniq_no
        from public.m_part_cutting_size_reference referensi
        where referensi.id = part_size_reference_uuid
          and referensi.aktif = true;

        if referensi_part_uniq_no is null then
            raise exception 'Referensi ukuran Cutting tidak ditemukan.';
        end if;

        if part_uniq_no is distinct from referensi_part_uniq_no then
            raise exception 'Referensi ukuran tidak sesuai dengan part acuan.';
        end if;
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
        total_layer,
        coalesce(nullif(payload ->> 'total_ok', '')::int, 0),
        coalesce(nullif(payload ->> 'total_ng', '')::int, 0),
        coalesce(nullif(payload ->> 'rasio_ng_global', '')::numeric, 0),
        'TERKIRIM'
    )
    returning id into sesi_id;

    insert into public.e_cutting_batch (
        id_sesi,
        material_id,
        nama_material_snapshot,
        spec_material_snapshot,
        uniq_no_part,
        nama_part_snapshot,
        part_size_reference_id,
        ukuran_manual,
        no_lot_roll,
        no_roll,
        size_cutting_cm,
        ukuran_cutting_cm,
        qty_layer_ok,
        qty_layer_ng,
        waste_panjang_cm,
        nama_operator,
        catatan
    )
    values (
        sesi_id,
        (payload ->> 'material_id')::uuid,
        payload ->> 'nama_material_snapshot',
        nullif(payload ->> 'spec_material_snapshot', ''),
        part_uniq_no,
        nullif(payload ->> 'nama_part_snapshot', ''),
        part_size_reference_uuid,
        part_size_reference_uuid is null,
        nullif(payload ->> 'no_lot_roll', ''),
        nullif(payload ->> 'no_roll', ''),
        ukuran_cm,
        ukuran_cm,
        coalesce(nullif(payload ->> 'qty_layer_ok', '')::int, 0),
        coalesce(nullif(payload ->> 'qty_layer_ng', '')::int, 0),
        coalesce(nullif(payload ->> 'waste_panjang_cm', '')::numeric, 0),
        nullif(payload ->> 'nama_operator', ''),
        nullif(payload ->> 'catatan', '')
    )
    returning id into batch_id;

    for defect_item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftar_defect', '[]'::jsonb))
    loop
        if coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0) > 0 then
            insert into public.e_cutting_defect_detail (
                id_cutting_batch,
                id_defect,
                nama_defect_snapshot,
                slot_waktu_id,
                jumlah_layer,
                panjang_defect_cm
            )
            values (
                batch_id,
                defect_item ->> 'id_defect',
                coalesce(defect_item ->> 'nama_defect_snapshot', '-'),
                nullif(defect_item ->> 'slot_waktu_id', '')::uuid,
                coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0),
                nullif(defect_item ->> 'panjang_defect_cm', '')::numeric
            );
        end if;
    end loop;

    return sesi_id;
end;
$$;

grant select on public.m_part_cutting_size_reference to anon, authenticated;
grant select on public.v_cutting_part_size_option to anon, authenticated;
grant execute on function public.rpc_submit_cutting_batch(jsonb) to anon, authenticated;

select pg_notify('pgrst', 'reload schema');

commit;
