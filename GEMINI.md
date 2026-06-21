# GEMINI.md

Panduan untuk Gemini atau agent lain yang bekerja di repository InSpectra.

## Ringkasan

InSpectra adalah aplikasi Android Kotlin untuk Quality Control. Aplikasi memakai Jetpack Compose, Material 3, Ktor Client, dan Supabase PostgREST.

Target utama adalah aplikasi tablet yang stabil, rapi, dan siap tumbuh menjadi workspace SaaS internal QC.

## Instruksi Utama

- Baca `AGENTS.md`, `README.md`, dan `DESIGN.md` sebelum mengubah kode.
- Gunakan current working directory sebagai root repository.
- Jangan membaca atau menampilkan credential dari `local.properties`.
- Jangan hardcode Supabase URL atau key.
- Fase aktif bersifat online-only: jangan menambah draft lokal, mode offline, DataStore draft,
  atau antrean sinkronisasi tanpa persetujuan eksplisit.
- Jangan menjalankan health check atau retry jaringan dari Splash. Splash hanya inisialisasi visual.
- Workbook sumber tidak boleh di-commit atau dibaca runtime. Ekstrak data menjadi migration SQL
  statis yang dapat diaudit; hanya buat relasi jika master part dan identitas material lengkap.
- Jangan mengubah file generated, build output, atau personal IDE state.
- Jangan melakukan hard reset.
- Jangan mengganti stack utama tanpa alasan teknis yang kuat.

## Cara Kerja

1. Mulai dengan orientasi:

```powershell
git status --short --branch
git log --oneline -5
rg --files
```

2. Baca file terkait sebelum patch.
3. Buat perubahan kecil dan terarah.
4. Jalankan verifikasi relevan.
5. Laporkan hasil nyata, bukan asumsi.

## Arsitektur

Pola yang harus diikuti:

- Compose Screen merender state dan mengirim intent.
- ViewModel memegang state, effect, dan orchestration.
- Repository memegang akses data.
- Driver network/database berada di `core`.
- Model domain berada di package `domain`.
- Pesan UI end-user memakai Bahasa Indonesia.

Hindari:

- API call langsung dari Composable.
- Mutable state global tanpa alasan.
- Network client baru di setiap repository/screen.
- Dummy data di dashboard atau chart.
- Copywriting campur aduk Inggris-Indonesia di UI.

## Debug Prioritas

Saat ada Logcat:

- Cari `FATAL EXCEPTION`.
- Cari `Caused by`.
- Cari frame package `com.primaraya.inspectra`.
- Pisahkan noise emulator/system dari bug aplikasi.

Contoh interpretasi:

- `NoSuchMethodException ViewModel.<init> [class android.app.Application]`: factory ViewModel salah atau constructor tidak cocok.
- `Skipped frames`: pekerjaan berat di main thread atau komposisi awal terlalu berat.
- `401/403`: credential, RLS, atau header Supabase.

## Verifikasi

Gunakan command:

```powershell
git diff --check
.\gradlew test --stacktrace
.\gradlew build --stacktrace --warning-mode all
```

Untuk device:

```powershell
adb devices -l
.\gradlew installDebug --stacktrace
```

Jika verifikasi gagal, baca error, patch akar masalah terkecil, lalu ulangi command relevan.

## Format Laporan

Gunakan format singkat:

1. Goal status.
2. Summary of changes.
3. Files changed.
4. Database/schema changes.
5. Endpoint/API changes.
6. UI/UX changes.
7. Tests added.
8. Validation command results.
9. Remaining risks.
10. Next recommended scope.
