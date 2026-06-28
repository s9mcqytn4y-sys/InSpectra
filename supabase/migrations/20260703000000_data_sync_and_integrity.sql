-- Migration: Data Sync and Integrity for Laporan & Checksheet
-- This migration creates views to synchronize checksheet data into Laporan Produksi.

BEGIN;

-- 1. View for Daily Part Summary from E-Checksheet
-- This view aggregates results from E-Checksheet by date, process, and part.
CREATE OR REPLACE VIEW public.v_checksheet_daily_part_summary AS
SELECT
    s.tanggal_pemeriksaan as tanggal,
    s.tipe_proses::text,
    i.uniq_no,
    SUM(i.jumlah_diperiksa) as checksheet_diperiksa,
    SUM(i.jumlah_ok) as checksheet_ok,
    SUM(i.jumlah_ng) as checksheet_ng
FROM public.e_sesi_checksheet s
JOIN public.e_item_checksheet i ON i.id_sesi = s.id
GROUP BY s.tanggal_pemeriksaan, s.tipe_proses, i.uniq_no;

-- 2. View for Consolidated Laporan Produksi (The "Sync" View)
-- This view merges actual report inputs with checksheet data to highlight discrepancies or provide sync.
CREATE OR REPLACE VIEW public.v_laporan_produksi_sync AS
SELECT
    l.id as laporan_id,
    l.tanggal,
    l.tipe_proses::text,
    p.id as part_id,
    p.uniq_no,
    p.nama_part,
    ld.planning,
    ld.actual,
    ld.ng as actual_ng_input,
    COALESCE(cs.checksheet_ng, 0) as checksheet_ng,
    -- Rule: If checksheet has data, it's the source of truth for NG
    GREATEST(ld.ng, COALESCE(cs.checksheet_ng, 0)) as sync_ng,
    (ld.actual - GREATEST(ld.ng, COALESCE(cs.checksheet_ng, 0))) as sync_ok
FROM public.e_laporan_produksi l
JOIN public.e_laporan_produksi_detail ld ON ld.id_laporan = l.id
JOIN public.m_part p ON p.id = ld.id_part
LEFT JOIN public.v_checksheet_daily_part_summary cs
    ON cs.tanggal = l.tanggal
    AND cs.tipe_proses = l.tipe_proses::text
    AND cs.uniq_no = p.uniq_no;

-- 3. Enhance Laporan Summary View
CREATE OR REPLACE VIEW public.v_laporan_produksi_summary AS
SELECT
    tanggal,
    tipe_proses,
    SUM(planning) as total_planning,
    SUM(actual) as total_actual,
    SUM(sync_ng) as total_ng,
    CASE
        WHEN SUM(actual) > 0 THEN ROUND((SUM(sync_ng)::numeric / SUM(actual)::numeric) * 100, 2)
        ELSE 0
    END as rasio_ng
FROM public.v_laporan_produksi_sync
GROUP BY tanggal, tipe_proses;

COMMIT;
