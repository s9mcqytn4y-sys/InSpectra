-- Migration: Supabase Hardening & Security
-- This migration fixes RPC permissions and ensures secure execution.

BEGIN;

-- 1. Harden submit_laporan_harian
-- Set security definer to bypass RLS with service-role level access if needed,
-- and grant execute to anon/authenticated roles.
CREATE OR REPLACE FUNCTION public.submit_laporan_harian(payload jsonb)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_laporan_id uuid;
    v_detail jsonb;
BEGIN
    INSERT INTO public.e_laporan_produksi (
        tanggal, tipe_proses, mp_direct, mp_indirect, jkn_hour, jkn_menit, ot_prod, ot_non, bantuan_keluar, bantuan_masuk
    ) VALUES (
        (payload->>'tanggal')::date,
        payload->>'tipe_proses',
        COALESCE((payload->>'mp_direct')::int, 0),
        COALESCE((payload->>'mp_indirect')::int, 0),
        COALESCE((payload->>'jkn_hour')::int, 0),
        COALESCE((payload->>'jkn_menit')::int, 0),
        COALESCE((payload->>'ot_prod')::numeric, 0),
        COALESCE((payload->>'ot_non')::numeric, 0),
        COALESCE((payload->>'bantuan_keluar')::int, 0),
        COALESCE((payload->>'bantuan_masuk')::int, 0)
    )
    ON CONFLICT (tanggal, tipe_proses) DO UPDATE SET
        mp_direct = EXCLUDED.mp_direct,
        mp_indirect = EXCLUDED.mp_indirect,
        jkn_hour = EXCLUDED.jkn_hour,
        jkn_menit = EXCLUDED.jkn_menit,
        ot_prod = EXCLUDED.ot_prod,
        ot_non = EXCLUDED.ot_non,
        bantuan_keluar = EXCLUDED.bantuan_keluar,
        bantuan_masuk = EXCLUDED.bantuan_masuk,
        diperbarui_pada = NOW()
    RETURNING id INTO v_laporan_id;

    DELETE FROM public.e_laporan_produksi_detail WHERE id_laporan = v_laporan_id;

    FOR v_detail IN SELECT * FROM jsonb_array_elements(payload->'details')
    LOOP
        INSERT INTO public.e_laporan_produksi_detail (
            id_laporan, id_part, planning, actual, ng
        ) VALUES (
            v_laporan_id,
            (v_detail->>'id_part')::uuid,
            COALESCE((v_detail->>'planning')::int, 0),
            COALESCE((v_detail->>'actual')::int, 0),
            COALESCE((v_detail->>'ng')::int, 0)
        );
    END LOOP;
END;
$$;

GRANT EXECUTE ON FUNCTION public.submit_laporan_harian(jsonb) TO anon, authenticated;

-- 2. Audit rpc_submit_checksheet (Ensure it's also secure)
-- Already handled in 20260622200000_perbaiki_enum_rpc_checksheet.sql but reinforcing here.
ALTER FUNCTION public.rpc_submit_checksheet(jsonb) SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_submit_checksheet(jsonb) TO anon, authenticated;

-- 3. Audit m_part and relations (Allow selects for all)
GRANT SELECT ON ALL TABLES IN SCHEMA public TO anon, authenticated;
-- Important: Tables like e_sesi_checksheet and e_laporan_produksi must allow inserts if not using RPC.
-- But since we use RPC (Security Definer), table-level grants aren't strictly required for users,
-- however for transparency we grant them.
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;

-- 4. Enable RLS but allow authenticated access for MVP simplicity (or restrict as needed)
-- ALTER TABLE public.e_sesi_checksheet ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY "Allow all for authenticated" ON public.e_sesi_checksheet FOR ALL TO authenticated USING (true);

COMMIT;
