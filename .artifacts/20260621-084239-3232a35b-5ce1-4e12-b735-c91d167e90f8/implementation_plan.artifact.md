# Finalize Migrations and Deploy App

Bereskan risiko migrasi, segarkan data seed, dorong ke main, jalankan migrasi Supabase, dan jalankan aplikasi.

## User Review Required

- **Environment Variables**: Untuk menjalankan `supabase db push` atau script `dorong_supabase.ps1`, variabel `SUPABASE_DB_URL` dan `INSPECTRA_IZINKAN_PRODUKSI=YA` harus sudah diset di terminal.
- **Deaktivasi Relasi Lama**: Saya akan menambahkan langkah dalam migrasi `20260621000003` untuk menonaktifkan relasi part-material lama yang bersifat generic agar tidak tumpang tindih dengan data presisi yang baru.

## Proposed Changes

### Database Migrations

#### [20260621000003_cutting_data_induk_dan_seed_partlist.sql](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/supabase/migrations/20260621000003_cutting_data_induk_dan_seed_partlist.sql)

- Menambahkan logika untuk menonaktifkan relasi generic lama sebelum memasukkan relasi baru yang lebih presisi.
- Memastikan `on conflict` atau `not exists` menangani pembaruan data dengan benar.

```diff
+ -- SEBELUM INSERT BARU: Nonaktifkan relasi part-material lama untuk part yang ada di seed ini
+ -- agar tidak terjadi tumpang tindih antara relasi generic (seed lama) dan relasi presisi (seed baru).
+ update public.m_part_material
+ set aktif = false
+ where uniq_no in (select uniq_no from seed_partlist_part);
```

---

### Scripts

#### [dorong_supabase.ps1](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/scripts/dorong_supabase.ps1)

- Verifikasi apakah script sudah siap dijalankan. (Tidak ada perubahan kode besar, hanya memastikan lingkungan siap).

---

### Git Operations

- `git add .`
- `git commit -m "feat: complete migrations and refresh seed data"`
- `git push origin main`

---

### App Deployment

- `.\gradlew installDebug` untuk menjalankan aplikasi di device/emulator.

## Verification Plan

### Automated Tests
- `.\gradlew test`: Menjalankan unit test untuk memastikan tidak ada regresi di kode aplikasi.

### Manual Verification
- **Database Audit**: Menjalankan query verifikasi yang ada di `dorong_supabase.ps1` setelah push untuk memastikan jumlah data sesuai dengan ekspektasi.
- **App Launch**: Memastikan aplikasi terbuka tanpa crash dan SplashScreen berhasil melewati health check.
- **Data Check**: Membuka layar Master Data untuk memverifikasi data part dan material yang baru muncul dengan benar.
