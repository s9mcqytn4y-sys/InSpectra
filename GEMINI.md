# GEMINI.md - INSPECTRA ELITE MANDATE

Panduan untuk Gemini atau agent lain yang bekerja di repository InSpectra.

## Mandat Arsitektur & Performa (Elite Directive)

Setiap perubahan wajib mematuhi aturan berikut demi stabilitas dan performa maksimal (terutama di emulator):

### 1. Refactoring & Clean Up
- **Boy Scout Rule**: Tinggalkan file lebih bersih (hapus import tak terpakai, dead code, baris kosong berlebih).
- **Hardcoded Values**: Ekstrak string ke `strings.xml` dan dimensi ke Material 3 tokens.
- **Deprecations**: Ganti API lama yang dideprecated dengan alternatif terbaru yang stabil.

### 2. MVI & UDF (Unidirectional Data Flow)
- **UI State**: Wajib satu data class tunggal dengan anotasi `@Immutable`.
- **Koleksi Immutable**: **DILARANG** menggunakan `List` atau `Map` standar di dalam State. Gunakan `kotlinx.collections.immutable` (`ImmutableList`, `PersistentList`) untuk mencegah rekomposisi paksa.
- **UI Intent & Effect**: Gunakan sealed interface/class untuk interaksi user dan side effects.
- **ViewModel**: Ekspos state via `StateFlow` dengan `WhileSubscribed(5000)`. Wajib inject `DispatchersProvider` untuk unit testing.

### 3. Jetpack Compose & Material 3
- **Design System**: Bergantung penuh pada `MaterialTheme.colorScheme` & `typography`. Jangan hardcode warna hex.
- **Optimasi List**: Setiap item dalam `LazyColumn`/`LazyRow` wajib memiliki `key` yang unik dan stabil.
- **Validasi Input**: Gunakan `derivedStateOf` untuk kalkulasi berat atau validasi form agar tidak terjadi frame drop (jank).

### 4. Supabase & Database
- **Migration Safety**: Gunakan `DROP VIEW IF EXISTS ... CASCADE;` sebelum `CREATE OR REPLACE VIEW` untuk mencegah error `SQLSTATE 42P16`.
- **Separation of Concerns**: Pisahkan file DDL (schema) dan DML (seed data).
- **Offline-First**: Prioritaskan Room sebagai SSOT (Single Source of Truth). Sync Supabase berjalan asinkron.

### 5. Build & Emulator Optimization
- **Gradle Caching**: Pastikan task custom kompatibel dengan gradle cache.
- **NDK Stripping**: Gunakan `keepDebugSymbols` untuk library grafis tertentu (misal `libandroidx.graphics.path.so`).
- **Isolated Logcat**: Filter logs berdasarkan package (`package:mine`) dan fokus pada Main Thread jank.

---

## Verifikasi Mandat

Gunakan command berikut untuk memastikan integritas:

```powershell
.\gradlew build --stacktrace --warning-mode all
git diff --check
```

Jika verifikasi gagal atau ada warning, segera perbaiki sesuai mandat di atas.
