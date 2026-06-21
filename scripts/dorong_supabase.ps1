[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

if ($env:INSPECTRA_IZINKAN_PRODUKSI -ne 'YA') {
    throw 'Set INSPECTRA_IZINKAN_PRODUKSI=YA sebelum menjalankan migration production.'
}

if ([string]::IsNullOrWhiteSpace($env:SUPABASE_DB_URL)) {
    throw 'SUPABASE_DB_URL harus tersedia sebagai environment variable untuk backup dan verifikasi production.'
}

$supabase = Get-Command supabase -ErrorAction Stop
$psql = Get-Command psql -ErrorAction Stop
$root = Split-Path -Parent $PSScriptRoot
$migration = Join-Path $root 'supabase\migrations\20260621000003_cutting_data_induk_dan_seed_partlist.sql'

if (-not (Test-Path -LiteralPath $migration)) {
    throw "Migration tidak ditemukan: $migration"
}

$isiMigration = Get-Content -LiteralPath $migration -Raw
if ($isiMigration -match '(?im)^\s*(drop|truncate)\s+') {
    throw 'Migration next-phase mengandung operasi destruktif dan tidak boleh didorong ke production.'
}

$folderCadangan = Join-Path $root 'cadangan'
New-Item -ItemType Directory -Path $folderCadangan -Force | Out-Null
$stempelWaktu = Get-Date -Format 'yyyyMMdd-HHmmss'
$fileCadangan = Join-Path $folderCadangan "supabase-public-$stempelWaktu.sql"

Write-Host 'Memeriksa status migration Supabase...'
& $supabase.Source migration list

Write-Host 'Membuat backup schema public...'
& $supabase.Source db dump --db-url $env:SUPABASE_DB_URL --schema public -f $fileCadangan
if (-not (Test-Path -LiteralPath $fileCadangan) -or (Get-Item -LiteralPath $fileCadangan).Length -eq 0) {
    throw 'Backup production tidak terbentuk atau kosong. Migration dibatalkan.'
}

Write-Host 'Mendorong migration additive ke project Supabase yang sudah terhubung...'
& $supabase.Source db push

$queries = @(
    "select count(*) as jumlah_part from public.m_part;",
    "select count(*) as jumlah_material from public.m_material;",
    "select count(*) as jumlah_relasi_part_material from public.m_part_material where aktif = true;",
    "select count(*) as relasi_part_material_tidak_valid from public.m_part_material relasi left join public.m_part part on part.uniq_no = relasi.uniq_no left join public.m_material material on material.id = relasi.material_id where relasi.aktif = true and (part.uniq_no is null or material.id is null);",
    "select count(*) as material_duplikat from (select nama_normalisasi, supplier_id, spec_ringkas from public.m_material where aktif = true group by nama_normalisasi, supplier_id, spec_ringkas having count(*) > 1) duplikat;",
    "select count(*) as part_dengan_template_defect from public.v_checksheet_part_defect where jsonb_array_length(daftar_defect) > 0;",
    "select count(*) as jumlah_pilihan_cutting from public.v_cutting_material_option;",
    "select count(*) as jumlah_ringkasan_cutting from public.v_cutting_daily_summary;",
    "select count(*) as relasi_material_defect from public.m_material_defect where aktif = true;"
)

foreach ($query in $queries) {
    & $psql.Source $env:SUPABASE_DB_URL -v ON_ERROR_STOP=1 -c $query
}

Write-Host "Migration dan verifikasi selesai. Backup lokal: $fileCadangan"
