# InSpectra

InSpectra adalah aplikasi Android tablet untuk sistem informasi Quality Control manufacturing. Dirancang khusus untuk efisiensi input data operasional di lapangan dengan arsitektur modern dan performa tinggi.

Tujuan produk: menyediakan workspace QC yang rapi, operasional, dan siap tumbuh menjadi standar SaaS internal untuk input Lembar Periksa, Data Induk, riwayat inspeksi, analytics, dan tindak lanjut perbaikan kualitas.

## Fitur Unggulan vNext

- **Checksheet Picker Flow**: Alur pemilihan part yang dioptimalkan dengan pencarian dan status kesiapan input. Mengurangi beban UI dan mempercepat proses inspeksi.
- **Cutting Management**: Pencatatan batch pemotongan berbasis material dan lot/roll dengan kalkulasi real-time rasio NG dan waste.
- **Server-Authoritative Cache**: Sistem sinkronisasi data cerdas berbasis `m_data_revision`. Aplikasi hanya melakukan fetch data dari network jika terdapat pembaruan di sisi server.
- **Media Integration**: Integrasi gambar part dan defect untuk visualisasi temuan yang lebih akurat di lapangan.
- **Responsive Tablet UI**: Layout adaptif yang memanfaatkan layar lebar tablet (grid 2-kolom dan side-by-side forms).

## Stack Teknologi

- **Kotlin 2.0 / Jetpack Compose**: UI deklaratif yang reaktif dan modern.
- **Material 3 Adaptive**: Layout yang responsif terhadap berbagai ukuran layar.
- **Ktor Client**: Networking yang ringan dan efisien.
- **Supabase PostgREST**: Backend serverless dengan integrasi database PostgreSQL yang kuat.
- **Coil**: Pemuatan gambar asinkron dengan caching efisien.
- **Kotlinx Serialization**: Serialisasi data JSON yang type-safe.

## Struktur Project

```text
app/src/main/java/com/primaraya/inspectra
+-- core
|   +-- common    (Result handling, AsyncData)
|   +-- data      (Driver database, Repository bootstrap)
|   +-- network   (Konfigurasi HTTP Client)
|   +-- ui        (Komponen global, Theme, Adaptive Layout)
+-- fitur
|   +-- checksheet (Lembar periksa Press/Sewing)
|   +-- cutting    (Batch cutting material)
|   +-- masterdata (Manajemen Data Induk)
|   +-- splash     (Inisialisasi visual)
+-- MainActivity.kt (Navigasi utama)
```

## Persiapan & Pengembangan

### Konfigurasi Lokal
Credential Supabase disimpan di `local.properties` (jangan di-commit):
```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-key
```

### Menjalankan Project
Gunakan PowerShell untuk build dan install:
```powershell
# Build APK
.\gradlew :app:assembleDebug

# Install ke Device/Emulator
.\gradlew :app:installDebug
```

### Verifikasi Mandiri
Sebelum melakukan push, pastikan build dan test berjalan lancar:
```powershell
.\gradlew test
.\gradlew build --warning-mode all
```

## Backend (Supabase)

Aplikasi ini menggunakan skema database **vNext** yang telah dimatangkan untuk hubungan master data yang kompleks.
Seluruh RPC submit (`rpc_submit_checksheet`, `rpc_submit_cutting_batch`) telah dioptimalkan untuk integritas data dan performa.

Migration file terbaru tersedia di `supabase/migrations/`.

---
© 2024 PT. Primaraya Graha Nusantara. All Rights Reserved.
