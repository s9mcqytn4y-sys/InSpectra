# InSpectra

InSpectra adalah aplikasi Android tablet untuk sistem informasi Quality Control PT. Primaraya Graha Nusantara.

Tujuan produk: menyediakan workspace QC yang rapi, operasional, dan bisa berkembang menjadi standar SaaS internal untuk input Lembar Periksa, Data Induk, riwayat inspeksi, analytics, dan follow-up perbaikan kualitas.

## Stack

- Kotlin 2.x.
- Android Gradle Plugin 8.x.
- Jetpack Compose.
- Material 3.
- Ktor Client.
- kotlinx.serialization.
- DataStore Preferences untuk draft lokal.
- Supabase PostgREST sebagai backend saat ini.
- Gradle wrapper.
- ADB untuk tablet/emulator lokal.

## Struktur

```text
app/src/main/java/com/primaraya/inspectra
+-- core
|   +-- common
|   +-- data
|   +-- draft
|   +-- network
|   +-- ui
+-- fitur
|   +-- checksheet
|   |   +-- data
|   |   +-- domain
|   |   +-- ui
|   +-- masterdata
|   |   +-- data
|   |   +-- domain
|   |   +-- ui
|   +-- splash
+-- MainActivity.kt
```

## Konfigurasi Lokal

Credential Supabase disimpan di `local.properties`. File ini tidak boleh di-commit.

Contoh:

```properties
sdk.dir=C\:\\Users\\Acer\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://project-ref.supabase.co
SUPABASE_KEY=replace-with-local-key
```

Gradle membaca nilai tersebut di `app/build.gradle.kts` dan meneruskannya ke:

- `BuildConfig.SUPABASE_URL`.
- `BuildConfig.SUPABASE_KEY`.

Jangan hardcode credential di Kotlin, XML, dokumentasi, screenshot, atau Logcat.

## Menjalankan Project

PowerShell:

```powershell
cd C:\Users\Acer\AndroidStudioProjects\InSpectra
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew build --stacktrace --warning-mode all --no-daemon --no-parallel
```

Install ke device:

```powershell
adb devices -l
.\gradlew installDebug --stacktrace --no-daemon --no-parallel
```

Ambil Logcat aplikasi:

```powershell
adb logcat -c
adb logcat | Select-String "com.primaraya.inspectra|AndroidRuntime|FATAL EXCEPTION|InspectraNetwork"
```

## Verifikasi

Minimal sebelum menyatakan patch selesai:

```powershell
git status --short --branch
git diff --check
.\gradlew test --stacktrace
.\gradlew build --stacktrace --warning-mode all
```

Jika patch menyentuh runtime Android:

```powershell
adb devices -l
.\gradlew installDebug --stacktrace
```

## Supabase

Integrasi backend saat ini menggunakan Supabase PostgREST.

Komponen utama:

- `core/network/InspectraHttpClient.kt`: konfigurasi Ktor, timeout, retry, JSON, dan header Supabase.
- `core/data/SupabasePgRestDriver.kt`: driver PostgREST generik.
- `fitur/masterdata/data/SupabaseMasterDataRepository.kt`: data induk.
- `fitur/checksheet/data/SupabaseChecksheetRepository.kt`: submit lembar periksa.
- `fitur/cutting/data/SupabaseCuttingRepository.kt`: batch Cutting berbasis material dan lot/roll.

Prinsip:

- Repository adalah batas akses data remote.
- ViewModel tidak menyusun URL Supabase.
- Composable tidak memanggil repository atau API langsung.
- Response error harus dipetakan menjadi pesan yang ramah pengguna.
- Header sensitif wajib disamarkan di Logcat.
- Android hanya boleh memakai publishable/anon key dengan RLS yang diaudit; jangan gunakan service-role key.

### Migration Produksi

Migration Next-Phase bersifat additive dan berada di
`supabase/migrations/20260621000003_cutting_data_induk_dan_seed_partlist.sql`.
Workbook sumber tidak disimpan di repository; seed SQL statis dapat diaudit.

Jalankan hanya setelah project Supabase telah ditautkan dan `SUPABASE_DB_URL`
disediakan oleh lingkungan yang aman:

```powershell
$env:INSPECTRA_IZINKAN_PRODUKSI='YA'
powershell -ExecutionPolicy Bypass -File .\scripts\dorong_supabase.ps1
```

Skrip menolak eksekusi tanpa flag, membuat backup schema `public` pada folder
`cadangan/` yang di-ignore Git, memeriksa operasi destruktif pada migration
baru, mendorong migration, lalu menjalankan query integritas dan view.

## Standar UI

InSpectra harus terasa seperti aplikasi SaaS operasional:

- Navigasi stabil.
- Layout padat dan mudah discan.
- Surface bersih, border jelas, elevation rendah.
- Bahasa Indonesia untuk semua UI end-user.
- State kosong dan error harus jujur.
- Tidak ada dummy production data.
- Aksi disabled harus punya alasan yang jelas.

Lihat `DESIGN.md` untuk pedoman desain lebih lengkap.

## Troubleshooting

`Cannot create an instance of ViewModel`:

- Cek apakah ViewModel memakai constructor `Application`.
- Jika memakai `AndroidViewModel`, Compose `viewModel()` perlu factory yang bisa membuat instance dengan `Application`.
- Jika ada dependency tambahan, sediakan `ViewModelProvider.Factory`.

`Skipped frames` atau UI terasa berat:

- Pastikan pekerjaan IO/network tidak berjalan di main thread.
- Hindari parsing besar di Composable.
- Gunakan `LazyColumn`/`LazyVerticalGrid` dengan key stabil.
- Batasi shadow/elevation berlebihan.

Supabase gagal:

- Pastikan `SUPABASE_URL` dan `SUPABASE_KEY` ada di `local.properties`.
- Cek RLS/policy Supabase.
- Cek response PostgREST di Logcat yang sudah disanitasi.

## Catatan Keamanan

- Jangan commit secret.
- Jangan log token, API key, password, atau NIP.
- Jangan menyimpan JWT permanen kecuali ada fitur remember-me yang eksplisit.
- Jangan memasukkan data produksi palsu ke UI.
