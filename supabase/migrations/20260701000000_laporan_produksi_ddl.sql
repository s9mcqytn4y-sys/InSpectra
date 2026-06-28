-- DDL Migration File: 20260701000000_laporan_produksi_ddl.sql
DROP VIEW IF EXISTS v_laporan_produksi_detail CASCADE;
DROP VIEW IF EXISTS v_laporan_produksi_summary CASCADE;

-- Tabel Induk (Header)
CREATE TABLE IF NOT EXISTS laporan_produksi (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tanggal DATE NOT NULL,
    tipe_proses TEXT NOT NULL,
    mp_direct INT DEFAULT 0,
    mp_indirect INT DEFAULT 0,
    jkn_hour INT DEFAULT 0,
    jkn_menit INT DEFAULT 0,
    ot_prod NUMERIC DEFAULT 0,
    ot_non NUMERIC DEFAULT 0,
    bantuan_keluar INT DEFAULT 0,
    bantuan_masuk INT DEFAULT 0,
    UNIQUE(tanggal, tipe_proses)
);

-- Tabel Transaksi (Detail)
CREATE TABLE IF NOT EXISTS laporan_produksi_detail (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_laporan UUID REFERENCES laporan_produksi(id) ON DELETE CASCADE,
    id_part UUID NOT NULL,
    planning INT DEFAULT 0,
    actual INT DEFAULT 0,
    ng INT DEFAULT 0
);

-- Prosedur Penyimpanan Atomik (RPC)
CREATE OR REPLACE FUNCTION submit_laporan_harian(payload jsonb)
RETURNS void AS $$
DECLARE
    v_laporan_id uuid;
    v_detail jsonb;
BEGIN
    -- Insert Header Laporan dengan UPSERT logic (ON CONFLICT) jika diperlukan, 
    -- atau insert normal.
    INSERT INTO laporan_produksi (
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

    -- Hapus detail lama jika ada (untuk upsert)
    DELETE FROM laporan_produksi_detail WHERE id_laporan = v_laporan_id;

    -- Insert barisan Part Detail
    FOR v_detail IN SELECT * FROM jsonb_array_elements(payload->'details')
    LOOP
        INSERT INTO laporan_produksi_detail (
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
