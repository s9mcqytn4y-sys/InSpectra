-- Migration: Attendance Ratio & Employee Master Module
-- This migration creates tables for employee management and attendance tracking.

BEGIN;

-- 1. Enum Types (Industrial Standard)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipe_pekerja_inspectra') THEN
        CREATE TYPE public.tipe_pekerja_inspectra AS ENUM ('KARYAWAN', 'PKL');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'keterangan_hadir_inspectra') THEN
        CREATE TYPE public.keterangan_hadir_inspectra AS ENUM ('HADIR', 'SAKIT', 'TELAT', 'CUTI', 'IZIN_PULANG', 'IZIN_TELAT');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lembur_non_main_job_inspectra') THEN
        CREATE TYPE public.lembur_non_main_job_inspectra AS ENUM ('TIDAK_ADA', 'BANTU_LINE_LAIN', '5S', 'REWORK_REPAIR', 'SETTING_MESIN');
    END IF;
END $$;

-- 2. Master Table: m_karyawan
CREATE TABLE IF NOT EXISTS public.m_karyawan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama_lengkap TEXT NOT NULL,
    tipe_pekerja public.tipe_pekerja_inspectra NOT NULL,
    no_reg TEXT UNIQUE,
    line_process public.tipe_proses_inspectra NOT NULL, -- Reusing existing TipeProses enum if compatible
    aktif BOOLEAN NOT NULL DEFAULT true,
    dibuat_pada TIMESTAMPTZ NOT NULL DEFAULT now(),
    diperbarui_pada TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- DOMAIN RULE: If KARYAWAN, no_reg must be NOT NULL. If PKL, no_reg must be NULL.
    CONSTRAINT chk_karyawan_no_reg CHECK (
        (tipe_pekerja = 'KARYAWAN' AND no_reg IS NOT NULL) OR
        (tipe_pekerja = 'PKL' AND no_reg IS NULL)
    )
);

-- 3. Transaction Table: t_rasio_kehadiran
CREATE TABLE IF NOT EXISTS public.t_rasio_kehadiran (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tanggal DATE NOT NULL DEFAULT CURRENT_DATE,
    line_process public.tipe_proses_inspectra NOT NULL,
    karyawan_id UUID NOT NULL REFERENCES public.m_karyawan(id) ON DELETE CASCADE,
    keterangan public.keterangan_hadir_inspectra NOT NULL DEFAULT 'HADIR',
    jam_lembur_aktual NUMERIC(4,1) DEFAULT 0 CHECK (jam_lembur_aktual >= 0),
    lembur_non_main_job public.lembur_non_main_job_inspectra NOT NULL DEFAULT 'TIDAK_ADA',
    dibuat_pada TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE(tanggal, karyawan_id)
);

-- 4. RPC for Batch Attendance Submission
CREATE OR REPLACE FUNCTION public.submit_attendance_batch(payload jsonb)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_item jsonb;
BEGIN
    FOR v_item IN SELECT * FROM jsonb_array_elements(payload->'attendance_list')
    LOOP
        INSERT INTO public.t_rasio_kehadiran (
            tanggal,
            line_process,
            karyawan_id,
            keterangan,
            jam_lembur_aktual,
            lembur_non_main_job
        ) VALUES (
            (payload->>'tanggal')::date,
            (payload->>'line_process')::public.tipe_proses_inspectra,
            (v_item->>'karyawan_id')::uuid,
            (v_item->>'keterangan')::public.keterangan_hadir_inspectra,
            COALESCE((v_item->>'jam_lembur_aktual')::numeric, 0),
            COALESCE((v_item->>'lembur_non_main_job')::public.lembur_non_main_job_inspectra, 'TIDAK_ADA')
        )
        ON CONFLICT (tanggal, karyawan_id) DO UPDATE SET
            keterangan = EXCLUDED.keterangan,
            jam_lembur_aktual = EXCLUDED.jam_lembur_aktual,
            lembur_non_main_job = EXCLUDED.lembur_non_main_job;
    END LOOP;
END;
$$;

GRANT EXECUTE ON FUNCTION public.submit_attendance_batch(jsonb) TO anon, authenticated;
GRANT ALL ON public.m_karyawan TO anon, authenticated, service_role;
GRANT ALL ON public.t_rasio_kehadiran TO anon, authenticated, service_role;

-- 5. Seed Data (Dummy)
INSERT INTO public.m_karyawan (nama_lengkap, tipe_pekerja, no_reg, line_process, aktif) VALUES
('Budi Santoso', 'KARYAWAN', 'REG001', 'PRESS', true),
('Siti Aminah', 'KARYAWAN', 'REG002', 'PRESS', true),
('Ahmad Fauzi', 'PKL', NULL, 'PRESS', true),
('Dewi Lestari', 'KARYAWAN', 'REG003', 'SEWING', true),
('Eko Prasetyo', 'KARYAWAN', 'REG004', 'SEWING', true),
('Rina Wijaya', 'PKL', NULL, 'SEWING', true)
ON CONFLICT DO NOTHING;

COMMIT;
