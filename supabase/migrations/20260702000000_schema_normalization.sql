-- Migration: Schema Normalization & Audit
-- This migration normalizes naming conventions for tables and views.

BEGIN;

-- 1. Ensure Table Names follow strict m_ (master) and e_ (entry) prefixes
-- Laporan Produksi was created as laporan_produksi, should be e_laporan_produksi for consistency with e_sesi_checksheet
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'laporan_produksi' AND table_schema = 'public') THEN
        ALTER TABLE public.laporan_produksi RENAME TO e_laporan_produksi;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'laporan_produksi_detail' AND table_schema = 'public') THEN
        ALTER TABLE public.laporan_produksi_detail RENAME TO e_laporan_produksi_detail;
    END IF;
END $$;

-- 2. Audit and normalize View Names (Ensure all start with v_)
-- v_checksheet_part_picker already follows v_
-- v_checksheet_part_defect already follows v_

-- 3. Re-create submit_laporan_harian with renamed tables
CREATE OR REPLACE FUNCTION public.submit_laporan_harian(payload jsonb)
RETURNS void AS $$
DECLARE
    v_laporan_id uuid;
    v_detail jsonb;
BEGIN
    INSERT INTO public.e_laporan_produksi (
        tanggal, tipe_proses, mp_direct, mp_indirect, jkn_hour, jkn_menit, ot_prod, ot_non, bantuan_keluar, bantuan_masuk
    ) VALUES (
        (payload->>'tanggal')::date,
        payload->>'tipe_proses',
        (payload->>'mp_direct')::int,
        (payload->>'mp_indirect')::int,
        (payload->>'jkn_hour')::int,
        (payload->>'jkn_menit')::int,
        (payload->>'ot_prod')::numeric,
        (payload->>'ot_non')::numeric,
        (payload->>'bantuan_keluar')::int,
        (payload->>'bantuan_masuk')::int
    )
    ON CONFLICT (tanggal, tipe_proses) DO UPDATE SET
        mp_direct = EXCLUDED.mp_direct,
        mp_indirect = EXCLUDED.mp_indirect,
        jkn_hour = EXCLUDED.jkn_hour,
        jkn_menit = EXCLUDED.jkn_menit,
        ot_prod = EXCLUDED.ot_prod,
        ot_non = EXCLUDED.ot_non,
        bantuan_keluar = EXCLUDED.bantuan_keluar,
        bantuan_masuk = EXCLUDED.bantuan_masuk
    RETURNING id INTO v_laporan_id;

    DELETE FROM public.e_laporan_produksi_detail WHERE id_laporan = v_laporan_id;

    FOR v_detail IN SELECT * FROM jsonb_array_elements(payload->'details')
    LOOP
        INSERT INTO public.e_laporan_produksi_detail (
            id_laporan, id_part, planning, actual, ng
        ) VALUES (
            v_laporan_id,
            (v_detail->>'id_part')::uuid,
            (v_detail->>'planning')::int,
            (v_detail->>'actual')::int,
            (v_detail->>'ng')::int
        );
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 4. Audit e_sesi_checksheet and related (They already use e_)

COMMIT;
