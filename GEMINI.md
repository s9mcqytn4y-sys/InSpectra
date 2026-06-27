Berikut adalah revisi dan penguatan secara kritis untuk dokumen `GEMINI.md`. Dokumen ini telah diperketat agar instruksinya bersifat operasional, minim ambiguitas, dan langsung selaras dengan best-practice Android modern tahun 2026.

```markdown
# GEMINI.md - INSPECTRA ELITE MANDATE

Dokumen instruksi dan batasan ketat untuk AI Agent (Gemini) atau engineer yang melakukan modifikasi kode pada repositori InSpectra.

## 1. Arsitektur & Pola State Management (UDF/MVI)

Setiap layar atau modul wajib menerapkan *Unidirectional Data Flow* (UDF) murni dengan ketentuan teknis berikut:

### UI State Kontrak:
* Wajib dikapsulasi dalam satu `data class` tunggal yang dianotasi dengan `@Immutable` atau `@Stable`.
* **Dilarang keras** menggunakan standar `List<T>` atau `Map<K, V>` di dalam properti State. Wajib menggunakan koleksi dari `kotlinx.collections.immutable` (misal: `ImmutableList<T>`, `PersistentList<T>`) untuk menjamin kepastian *skipping* saat recomposition.

### Aliran Data ViewModel:
* State diekspos ke UI menggunakan `StateFlow` melalui operator `.stateIn` dengan kebijakan pembatalan `SharingStarted.WhileSubscribed(5_000)`.
* Ketergantungan threading wajib menggunakan abstraksi `CoroutineDispatchersProvider` yang di-inject (via Hilt/Koin), bukan memanggil `Dispatchers.IO` atau `Dispatchers.Default` secara langsung, demi kelayakan *Unit Testing*.
* Interaksi pengguna (*User Intent*) dan efek samping (*Side Effect/Event*) wajib menggunakan struktur `sealed interface`.

---

## 2. Praktik Refactoring & Jetpack Compose Polish

### Boy Scout Rule & Pembersihan Kode:
* Hapus semua *unused imports*, *dead code*, fungsi yang tidak lagi dipanggil, serta baris kosong yang berlebih sebelum mengajukan perubahan.
* Semua string teks literal wajib diekstraksi ke `strings.xml`. Dimensi, padding, dan bentuk border wajib merujuk pada token Material 3 yang didefinisikan pada tema lokal.

### Performa Rendering & List:
* Setiap komponen di dalam `LazyColumn`, `LazyRow`, atau `LazyVerticalGrid` wajib menyertakan parameter `key` yang stabil dan unik (jangan gunakan indeks array).
* Gunakan `derivedStateOf` untuk mengamati perubahan state formulir atau kalkulasi matematika dinamis guna menghindari kalkulasi berulang pada setiap frame (*jank/frame drop*).

---

## 3. Integrasi Supabase & Offline-First Data

### Keamanan Migrasi Database:
* Untuk mencegah error struktural Postgres (`SQLSTATE 42P16`), setiap skrip migrasi yang memperbarui struktur view wajib menggunakan klausa drop yang aman secara sekuensial:
  ```sql
  DROP VIEW IF EXISTS nama_view_anda CASCADE;
  CREATE OR REPLACE VIEW nama_view_anda AS ...

```

* Pisahkan secara tegas berkas skrip DDL (definisi skema tabel/view) dan berkas DML (data pembenihan/*seed data*).

### Single Source of Truth (SSOT):

* Arsitektur data wajib mengutamakan *Offline-First*. Komponen UI hanya diperbolehkan membaca data dari database lokal (Room/DataStore). Supabase bertindak sebagai repositori remote yang disinkronkan secara asinkron di latar belakang melalui *Worker* atau *Coroutine Scope* terisolasi.

---

## 4. Optimasi Build & Emulator Lingkungan Kerja

### Manajemen Build & Caching:

* Pastikan seluruh konfigurasi custom task pada `build.gradle.kts` mendukung Gradle Build Cache dan Configuration Cache (hindari penggunaan *runtime project evaluation*).
* Tambahkan konfigurasi `keepDebugSymbols` pada blok `android.packaging` jika menggunakan pustaka grafis eksternal tingkat rendah (seperti `libandroidx.graphics.path.so`) untuk mempercepat proses kompilasi tanpa mengorbankan pembacaan jejak tumpukan galat (*stacktrace*).

### Debugging & Logcat:

* Jangan mencetak log secara sembarangan. Gunakan tag log yang terstruktur dan pastikan tidak ada data personal operator, token JWT Supabase, atau payload data inspeksi yang bocor ke dalam sistem logcat Android.

---

## 5. Protokol Verifikasi Mandat

Sebelum perubahan kode dianggap selesai dan siap untuk dibuatkan *Pull Request*, jalankan rangkaian pemeriksaan berikut pada terminal lokal:

```powershell
# Jalankan kompilasi penuh, pemeriksaan warning, dan pengujian unit sekaligus
.\gradlew clean build testDebugUnitTest --stacktrace --warning-mode all

# Pastikan tidak ada spasi sisa di akhir baris atau konflik git tersembunyi
git diff --check

```

Jika proses di atas menghasilkan error, warning arsitektur, atau kegagalan pengujian, perubahan kode dianggap **tidak sah** dan wajib diperbaiki ulang dengan merujuk pada pedoman dokumen ini.

```

```