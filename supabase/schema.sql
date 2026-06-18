-- ==========================================
-- SKEMA SUPABASE: MASTER DATA & E-CHECKSHEET
-- ==========================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. MASTER PART
CREATE TABLE m_part (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  part_no text,
  uniq_no text UNIQUE NOT NULL,
  nama_part text NOT NULL,
  model text,
  customer text,
  komoditas text,
  lokasi_gambar text,
  created_at timestamptz DEFAULT now()
);

-- 2. MASTER MATERIAL
CREATE TABLE m_material (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  supplier text,
  nama_material text NOT NULL,
  spec text,
  satuan text,
  created_at timestamptz DEFAULT now()
);

-- 3. E-CHECKSHEET: SESI
CREATE TABLE e_sesi_checksheet (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  tipe_proses text NOT NULL,
  total_diperiksa int NOT NULL,
  total_ok int NOT NULL,
  total_ng int NOT NULL,
  rasio_ng_global numeric NOT NULL,
  created_at timestamptz DEFAULT now()
);

-- 4. E-CHECKSHEET: ITEM (DETAIL PART)
CREATE TABLE e_item_checksheet (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  id_sesi uuid REFERENCES e_sesi_checksheet(id) ON DELETE CASCADE,
  uniq_no text NOT NULL,
  jumlah_diperiksa int NOT NULL,
  jumlah_ok int NOT NULL,
  jumlah_ng int NOT NULL,
  rasio_ng numeric NOT NULL
);

-- 5. E-CHECKSHEET: DEFECT (DETAIL NG)
CREATE TABLE e_defect_checksheet (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  id_item uuid REFERENCES e_item_checksheet(id) ON DELETE CASCADE,
  id_defect text NOT NULL,
  nama_defect text NOT NULL,
  kategori text NOT NULL,
  jumlah int NOT NULL
);

-- SETUP SECURITY / RLS (Opsional: Jika Anda ingin open access di awal untuk Testing)
-- ALTER TABLE m_part ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY "Allow anonymous read" ON m_part FOR SELECT USING (true);
-- dst...
