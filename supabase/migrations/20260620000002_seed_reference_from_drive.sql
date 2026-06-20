-- =========================================================
-- DEFECT DEFAULT
-- =========================================================

insert into public.m_defect (id_defect, nama_defect, kategori, proses_default)
values
    ('DENT', 'Dent', 'PROSES', 'PRESS'),
    ('DIMENSI_OUT_STD', 'Dimensi out standard', 'PROSES', 'PRESS'),
    ('GALER', 'Galer', 'PROSES', 'PRESS'),
    ('HOLE_TA', 'Hole T/A', 'PROSES', 'PRESS'),
    ('OVERCUTTING', 'Overcutting', 'PROSES', 'CUTTING'),
    ('TERBALIK', 'Terbalik', 'PROSES', 'PRESS'),
    ('KOTOR', 'Kotor', 'MATERIAL', null),
    ('SALAH_MATERIAL', 'Salah material', 'MATERIAL', null),
    ('CACAT_MATERIAL', 'Cacat material', 'MATERIAL', null),
    ('SEWING_MIRING', 'Sewing miring', 'PROSES', 'SEWING'),
    ('SEWING_LONCAT', 'Sewing loncat', 'PROSES', 'SEWING'),
    ('SEWING_PUTUS', 'Sewing putus', 'PROSES', 'SEWING')
on conflict (id_defect) do update set
    nama_defect = excluded.nama_defect,
    kategori = excluded.kategori,
    proses_default = excluded.proses_default,
    aktif = true;

-- =========================================================
-- SLOT WAKTU MINIMUM 8
-- =========================================================

insert into public.m_slot_waktu (kode_slot, tipe_proses, nama_shift, label_waktu, urutan)
values
    ('PRESS_SHIFT_1_SLOT_1', 'PRESS', 'SHIFT_1', '08.00 - 09.00', 1),
    ('PRESS_SHIFT_1_SLOT_2', 'PRESS', 'SHIFT_1', '09.00 - 10.00', 2),
    ('PRESS_SHIFT_1_SLOT_3', 'PRESS', 'SHIFT_1', '10.00 - 11.00', 3),
    ('PRESS_SHIFT_1_SLOT_4', 'PRESS', 'SHIFT_1', '11.00 - 12.00', 4),
    ('PRESS_SHIFT_1_SLOT_5', 'PRESS', 'SHIFT_1', '13.00 - 14.00', 5),
    ('PRESS_SHIFT_1_SLOT_6', 'PRESS', 'SHIFT_1', '14.00 - 15.00', 6),
    ('PRESS_SHIFT_1_SLOT_7', 'PRESS', 'SHIFT_1', '15.00 - 16.00', 7),
    ('PRESS_SHIFT_1_SLOT_8', 'PRESS', 'SHIFT_1', '16.00 - 17.00', 8),
    ('SEWING_SHIFT_1_SLOT_1', 'SEWING', 'SHIFT_1', '08.00 - 09.00', 1),
    ('SEWING_SHIFT_1_SLOT_2', 'SEWING', 'SHIFT_1', '09.00 - 10.00', 2),
    ('SEWING_SHIFT_1_SLOT_3', 'SEWING', 'SHIFT_1', '10.00 - 11.00', 3),
    ('SEWING_SHIFT_1_SLOT_4', 'SEWING', 'SHIFT_1', '11.00 - 12.00', 4),
    ('SEWING_SHIFT_1_SLOT_5', 'SEWING', 'SHIFT_1', '13.00 - 14.00', 5),
    ('SEWING_SHIFT_1_SLOT_6', 'SEWING', 'SHIFT_1', '14.00 - 15.00', 6),
    ('SEWING_SHIFT_1_SLOT_7', 'SEWING', 'SHIFT_1', '15.00 - 16.00', 7),
    ('SEWING_SHIFT_1_SLOT_8', 'SEWING', 'SHIFT_1', '16.00 - 17.00', 8)
on conflict (kode_slot) do update set
    label_waktu = excluded.label_waktu,
    urutan = excluded.urutan,
    aktif = true;

-- =========================================================
-- MATERIAL
-- =========================================================

insert into public.m_material (nama_material, supplier_manual, spec_ringkas, satuan)
values
    ('Hardfelt', null, '9Y6', 'PCS'),
    ('Carpet CB-III', null, null, 'PCS'),
    ('Black Spunbond', null, '50 GSM', 'PCS'),
    ('EPDM', null, null, 'PCS'),
    ('Silencer', null, '1000 GSM', 'PCS'),
    ('Felt Before Laminating', null, null, 'PCS'),
    ('Fujiseat Hard Felt', null, '9Y8', 'PCS')
on conflict (nama_normalisasi, supplier_id, spec_ringkas) do nothing;

-- =========================================================
-- PART DARI PART LIST + FM-QA-010 / FM-QA-010B
-- =========================================================

insert into public.m_part (
    uniq_no,
    part_no,
    nama_part,
    komoditas,
    total_item_per_kanban,
    sample_item_per_kanban,
    sample_cycle_note
)
values
    ('FP7', '11127-A1042', 'FELT FR RH', 'SEWING', 50, 8, '3 SEC/SAMPLE'),
    ('FP8', '12127-A1042', 'FELT FR LH', 'SEWING', 50, 8, '3 SEC/SAMPLE'),
    ('BY4', '12115-A1012', 'FELT FR BACK LH (LOW-G)', 'SEWING', 100, 20, '3 SEC/SAMPLE'),
    ('CB9', '58815-KK010', 'CARPET CONSOLE BOX', 'PRESS', 150, 20, '3 SEC/SAMPLE'),
    ('DN5', '11115-A1012', 'FELT FR BACK RH (LOW-G)', 'SEWING', 100, 20, null),
    ('EQ5', '79117-0K060-A', 'CARPET NO 1 SEAT CUSHION', 'PRESS', null, null, null),
    ('FP9', '13115-A1042', 'FELT 40%', 'SEWING', null, null, null),
    ('FQ0', '14137-A1044', 'FELT 60%', 'SEWING', null, null, null),
    ('MM1', '58611-A1011', 'INSULATION SHEET A1', 'PRESS', 50, null, null),
    ('ML0', '58611-A1012', 'INSULATION SHEET A2', 'PRESS', 50, null, null),
    ('ML1', '58611-A1013', 'INSULATION SHEET A3', 'PRESS', 50, null, null),
    ('FT7', '58611-A1014', 'INSULATION SHEET A4', 'PRESS', 50, null, null),
    ('FT8', '58611-A1015', 'INSULATION SHEET A5', 'PRESS', 50, null, null),
    ('CB3', '58612-A1016', 'FELT', 'SEWING', null, null, null),
    ('ER2', '58612-A1020', 'FELT', 'SEWING', null, null, null)
on conflict (uniq_no) do update set
    part_no = excluded.part_no,
    nama_part = excluded.nama_part,
    komoditas = excluded.komoditas,
    total_item_per_kanban = excluded.total_item_per_kanban,
    sample_item_per_kanban = excluded.sample_item_per_kanban,
    sample_cycle_note = excluded.sample_cycle_note,
    aktif = true,
    diperbarui_pada = now();

-- =========================================================
-- RELASI PART-MATERIAL
-- =========================================================

insert into public.m_part_material (uniq_no, material_id, label_material)
select p.uniq_no, m.id, m.nama_material
from public.m_part p
join public.m_material m on
    (p.uniq_no in ('FP7','FP8','BY4','DN5','FP9','FQ0') and m.nama_material = 'Hardfelt')
    or (p.uniq_no = 'CB9' and m.nama_material = 'Carpet CB-III')
    or (p.uniq_no = 'EQ5' and m.nama_material = 'Black Spunbond')
    or (p.uniq_no in ('MM1','ML0','ML1','FT7','FT8') and m.nama_material = 'EPDM')
    or (p.uniq_no in ('CB3','ER2') and m.nama_material = 'Silencer')
on conflict do nothing;

-- =========================================================
-- TEMPLATE DEFECT PER PART
-- =========================================================

insert into public.m_part_defect (uniq_no, id_defect, urutan)
select p.uniq_no, d.id_defect,
       row_number() over (partition by p.uniq_no order by d.nama_defect)::int
from public.m_part p
join public.m_defect d on
    d.aktif = true
    and (
        d.proses_default = p.komoditas
        or d.kategori = 'MATERIAL'
    )
where p.aktif = true
  and p.komoditas in ('PRESS', 'SEWING', 'CUTTING')
on conflict (uniq_no, id_defect) do update set
    aktif = true;

select pg_notify('pgrst', 'reload schema');
