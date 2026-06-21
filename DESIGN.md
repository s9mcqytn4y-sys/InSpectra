# DESIGN.md

Pedoman desain InSpectra.

## Tujuan Desain

InSpectra harus terasa seperti workspace SaaS operasional untuk Quality Control manufacturing:

- Cepat dibaca.
- Padat tetapi tidak sesak.
- Stabil pada tablet.
- Aman untuk input data berulang.
- Jujur saat data kosong, offline, atau gagal dimuat.

Desain bukan landing page. Layar pertama setelah splash harus membantu pengguna bekerja.

## Prinsip Produk

Alur utama:

```text
Splash -> Dashboard -> Lembar Periksa -> Draft Lokal -> Kirim -> Riwayat -> Analytics -> Action Plan
```

Prinsip manufacturing yang didukung:

- Jidoka: masalah kualitas terlihat jelas.
- Andon: status abnormal diberi penekanan visual.
- Kaizen/PDCA: data mengarah ke tindakan.
- Genchi Genbutsu: pengguna bisa drill down ke part, material, pos, waktu, dan sumber defect.
- Poka-yoke: input invalid dicegah sebelum dikirim.

## Bahasa UI

Gunakan Bahasa Indonesia untuk UI end-user.

Istilah standar:

| Hindari | Gunakan |
| --- | --- |
| E-Checksheet | Lembar Periksa |
| Master Data | Data Induk |
| Part | Part |
| Defect | Defect atau Temuan NG |
| Submit | Kirim |
| Save | Simpan |
| Delete | Hapus |
| Validation | Validasi |
| Error | Gagal |
| Loading | Memuat |
| Empty | Belum ada data |
| Size | Ukuran |
| Waste | Sisa Material |
| PIC | Operator |

Nama teknis seperti Kotlin, Supabase, PostgREST, Material 3, dan API boleh tetap dipakai di dokumentasi teknis.

## Warna

Gunakan token dari `core/ui/theme/Color.kt`.

Palet operasional:

- Primary: biru tua untuk struktur, navigasi, dan heading.
- Secondary: kuning/oranye sebagai aksen tindakan utama.
- Background: abu sangat terang.
- Surface: putih.
- Error: merah.
- Success: hijau.
- Text primary: abu gelap.
- Text secondary: abu slate.

Aturan:

- Jangan membuat layar didominasi satu warna.
- Jangan overuse aksen kuning/oranye.
- Hindari gradient dekoratif.
- Hindari shadow besar.
- Gunakan border dan tonal elevation rendah.

## Layout SaaS

Dashboard:

- Tampilkan status sistem dan aksi utama.
- Hindari kartu besar yang hanya dekoratif.
- Modul harus menjelaskan pekerjaan yang bisa dilakukan.
- Kartu harus compact dan konsisten.

Form Lembar Periksa:

- Ringkasan total harus selalu mudah terlihat.
- Part/defect disusun untuk input cepat.
- Tombol kirim berada di area konsisten.
- Validasi kuantitas NG harus terlihat dekat input.
- Detail slot harus jelas saat total slot tidak sama dengan total NG.

Data Induk:

- Tab harus stabil.
- Search harus jelas.
- Empty state tidak boleh menampilkan data palsu.
- Dialog tambah/edit harus fokus dan tidak terlalu tinggi pada tablet.

## Komponen

Gunakan:

- `Scaffold` untuk screen utama.
- `TopAppBar` atau custom topbar yang stabil.
- `NavigationRail` atau sidebar bila scope navigasi bertambah.
- `Card` atau `ElevatedCard` hanya untuk item atau panel kerja.
- `AssistChip` untuk metadata ringkas.
- `OutlinedTextField` untuk form.
- `AlertDialog` untuk konfirmasi.
- `Snackbar` atau flash compact untuk feedback.

Hindari:

- Card di dalam card tanpa kebutuhan.
- Text panjang di tombol sempit.
- Icon tanpa `contentDescription` bila interaktif.
- Hardcoded color baru di banyak file.
- UI yang berubah ukuran saat state loading/error.

## Tipografi

Gunakan Material 3 typography.

Aturan:

- Heading besar hanya untuk judul layar.
- Kartu menggunakan `titleMedium` atau `titleLarge` seperlunya.
- Label metadata gunakan `labelMedium` atau `labelSmall`.
- Jangan gunakan letter spacing negatif.
- Jangan scale font berdasarkan viewport.
- Pastikan teks tidak clipped di tablet kecil.

## State

Setiap screen harus punya state:

- Loading.
- Success.
- Empty.
- Error.
- Offline/cache bila tersedia.
- Saving/sending.

Empty state harus menjelaskan kondisi nyata:

- "Belum ada data part aktif."
- "Data belum tersedia dari Supabase."
- "Data riwayat belum cukup untuk membuat Pareto."

Jangan tampilkan dummy production data.

## Accessibility

- Semua tombol icon interaktif wajib punya `contentDescription`.
- Touch target minimal 48 dp.
- Kontras teks harus cukup.
- Jangan hanya mengandalkan warna untuk error; tambahkan teks.
- Loading yang lama harus punya teks status.

## Performance

- Gunakan `LazyColumn` dan `LazyVerticalGrid` untuk list besar.
- Berikan key stabil pada lazy item.
- Hindari parsing data besar di Composable.
- Hindari membuat repository/client di body Composable kecuali memakai `remember` dan tidak ada DI.
- Jaga pekerjaan network dan IO tetap di coroutine ViewModel/repository.

## Standar Polish

Sebelum menyebut UI polished:

- Tidak ada overlap.
- Tidak ada text clipped.
- Loading, empty, error, dan success state terlihat benar.
- Tombol disabled punya alasan atau konteks jelas.
- Warna konsisten dengan theme.
- Logcat bebas crash aplikasi.
- Tidak ada credential di log.

