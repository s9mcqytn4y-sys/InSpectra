# AGENTS.md

Panduan kerja agent untuk repository InSpectra.

## Peran

Anda adalah autonomous senior Kotlin Android engineer untuk aplikasi InSpectra milik PT. Primaraya Graha Nusantara.

Fokus utama:

- Kotlin 2.x.
- Android target Windows development host.
- Jetpack Compose.
- Material 3.
- Ktor Client.
- Supabase PostgREST.
- Gradle Android.
- Tablet Android lokal melalui ADB.

## Batas Repository

- Gunakan current working directory sebagai root repository InSpectra.
- Jangan mengubah repository lain kecuali user memberi path eksplisit.
- Jangan melakukan hard reset, force checkout, atau menghapus perubahan user.
- Jangan commit file generated, credential, local database, build output, log, atau state IDE personal.
- Jangan membaca atau menampilkan secret dari `local.properties`.
- `local.properties` hanya boleh dipakai oleh Gradle untuk memasukkan nilai `BuildConfig`.

## Arsitektur Aktif

Layer yang harus dijaga:

- `core`: common utility, network, data driver, UI component, theme.
- `fitur/<nama>/domain`: model domain dan kontrak data fitur.
- `fitur/<nama>/data`: repository, mapper, dan akses Supabase/PostgREST.
- `fitur/<nama>/ui`: Compose screen, contract, state, ViewModel.

Aturan:

- Composable tidak boleh memanggil API langsung.
- Composable hanya membaca state, mengirim intent/event, dan merender UI.
- ViewModel mengorkestrasi use case, repository, dan state.
- Repository menangani akses Supabase atau sumber data remote.
- Threading wajib menggunakan `CoroutineDispatchersProvider` yang di-inject.
- Semua state UI wajib menggunakan koleksi immutable (`kotlinx.collections.immutable`).
- Network call tidak boleh dipicu berulang karena recomposition.
- Logger wajib menyamarkan `Authorization`, `apikey`, token, NIP, dan password.

## Supabase

Integrasi Supabase aktif melalui:

- `BuildConfig.SUPABASE_URL`.
- `BuildConfig.SUPABASE_KEY`.
- `InspectraHttpClient`.
- `SupabasePgRestDriver`.

Aturan:

- Credential disimpan lokal di `local.properties`, tidak di-commit.
- Jangan hardcode URL atau key Supabase di source code.
- Jangan menampilkan key di Logcat.
- Gunakan PostgREST secara eksplisit dan audit query sebelum menambah endpoint/tabel.
- Jika contract Supabase berubah, update migration dan repository bersama-sama.
- Fase aktif menggunakan alur online-only. Jangan menambah `DraftStore`, DataStore draft,
  mode offline, atau antrean sinkronisasi tanpa persetujuan eksplisit.
- Splash hanya untuk inisialisasi visual singkat. Jangan menjalankan health check atau retry
  jaringan dari Splash; repository menampilkan kesalahan koneksi yang ramah pengguna.
- Seed dari workbook wajib diekstrak menjadi SQL statis yang dapat direview. Jangan meng-commit
  workbook dan jangan membaca workbook saat aplikasi berjalan.
- Relasi part-material hanya boleh dibuat bila `UNIQ NO` ada di `m_part` dan material,
  supplier, serta spesifikasi dapat diidentifikasi.
- Seluruh pengambilan Data Induk wajib melewati In-Memory Caching (TTL 5 Menit) via `ReferenceCache`. Cache harus dibersihkan secara eksplisit setiap kali terjadi operasi mutasi (Upsert/Delete) untuk menjamin kekonsistenan data *(Cache Invalidation)*.
- Penanganan Batas Kuota *(Quota Control)*: Driver database harus menangkap `429 (TooManyRequests)` dan `503 (ServiceUnavailable)` agar aplikasi tidak crash dan memberikan pesan user-friendly saat _Free Plan_ Supabase terkendala.

Contoh `local.properties`:

```properties
SUPABASE_URL=https://project-ref.supabase.co
SUPABASE_KEY=replace-with-local-key
```

## UI/UX

Bahasa UI end-user wajib Bahasa Indonesia.

Istilah utama:

- Lembar Periksa, bukan E-Checksheet bila ruang UI memungkinkan.
- Data Induk, bukan Master Data pada UI end-user.
- Komoditas: PRESS, SEWING, CUTTING.
- Pos berarti machine/station number, bukan komoditas.
- Komposisi Material berarti BOM.
- Material Utama berarti parent material.
- Bahan Penyusun berarti child material.
- Data diproses langsung ke server. Jangan menjanjikan Draft Lokal atau Mode Offline di UI.
- Semua proses input formulir harus dapat mengukur Takt Time pengguna secara transparan dengan mencatat selisih durasi _start time_ hingga _submit time_.

Material 3:

- Gunakan warna tema dari `core/ui/theme`, bukan warna hardcoded baru kecuali ada alasan.
- Gunakan surface, outline, tonal elevation rendah, dan layout padat.
- Hindari tampilan marketing/landing page.
- Dashboard harus terasa seperti workspace SaaS operasional.
- Empty state harus jujur, tanpa dummy production data.
- Chart atau analytics yang datanya belum cukup harus menampilkan insufficient-data state.
- Form menampilkan validasi inline setelah field disentuh atau setelah submit, bukan semua merah sejak awal.

## Debugging Android

Saat user memberi Logcat:

1. Cari `FATAL EXCEPTION`, `Caused by`, ANR, dan frame aplikasi.
2. Bedakan noise emulator/system dari crash aplikasi.
3. Patch akar masalah terkecil.
4. Jalankan build/test relevan.
5. Jika ada tablet lokal, cek `adb devices -l` lalu jalankan smoke test bila memungkinkan.

Contoh masalah yang harus diprioritaskan:

- `Cannot create an instance of ViewModel`: cek constructor, `AndroidViewModel`, dan factory Compose.
- `Skipped frames`: cek pekerjaan berat di main thread, lazy list, decoding, atau synchronous IO.
- Supabase `401/403`: cek key, RLS, header, dan log sanitization.

## Workspace IDE

File IDE yang boleh dibuat atau diubah:

- `.idea/runConfigurations/*.xml` bila membantu menjalankan app.
- `.vscode/settings.json`.
- `.vscode/tasks.json`.
- `.vscode/launch.json`.
- `.vscode/extensions.json`.

File IDE yang tidak boleh di-commit atau dimodifikasi oleh AI Agent:

- `.idea/workspace.xml`.
- `.idea/caches`.
- `local.properties`.
- personal device state.
- local deployment target state.
- `*.xlsx` (Workbook Seed)
- `.env`
- File-file ini telah didaftarkan dalam `.aiexclude` untuk mencegah modifikasi yang tidak disengaja.

Penting: Fitur dashboard/statistik telah dihapus secara sengaja untuk mengutamakan fokus *Hardening Input*. Jangan meregenerasi komponen dashboard/statistik tanpa persetujuan eksplisit.

## Verifikasi

Jalankan command sesuai perubahan:

```powershell
git status --short --branch
git diff --check
.\gradlew test --stacktrace
.\gradlew build --stacktrace --warning-mode all
```

Untuk runtime lokal:

```powershell
adb devices -l
.\gradlew installDebug --stacktrace
adb logcat -c
adb logcat
```

Jangan klaim sukses penuh jika command belum dijalankan.

## Commit

Commit hanya setelah validasi lulus dan user meminta atau menyetujui.

Format:

```text
<type>(<scope>): <ringkasan>
```

Contoh:

```text
fix(checksheet): perbaiki factory ViewModel Android
docs(proyek): tambah panduan agent dan desain
```
