Pedoman desain UI/UX InSpectra.
Dokumen ini bersifat mengikat (*Elite Directive*) bagi seluruh Agent AI dan Pengembang di repositori InSpectra untuk menjaga stabilitas, performa, dan standarisasi antarmuka.

---

## 1. Tujuan Desain

InSpectra dirancang sebagai workspace SaaS operasional tangguh untuk lingkungan Quality Control (QC) manufacturing:
* **Kecepatan Baca Utama:** Informasi takt-time, status Andon, dan data defect harus dapat dicerna dalam waktu < 1.5 detik oleh Operator.
* **Padat & Efisien:** Layout memaksimalkan *screen real-estate* tanpa menimbulkan kesan sesak (*high data density*).
* **Adaptif & Stabil:** Layout wajib mempertahankan proporsi visual, keterbacaan teks, dan arsitektur kontainer baik dalam orientasi Portrait, Landscape, maupun multi-window pada Tablet dan Phone.
* **Resilien Terhadap Input Berulang:** Transisi fokus field, penutupan *IME Soft-Keyboard*, dan validasi form harus instan tanpa memicu pemblokiran *Main Thread*.
* **Jujur Terhadap State Sistem:** Menampilkan kondisi riil apa adanya saat data kosong, memuat, atau gagal (Tanpa komponen hiasan atau fitur spekulatif).

> **Elite Rule:** Aplikasi ini bukan landing page pemasaran. Layar pertama setelah Splash Screen wajib langsung menyajikan modul kerja aktif untuk membantu Operator mengeksekusi tugas.

---

## 2. Prinsip Produk & Arsitektur Pabrik

Alur Navigasi Utama Aplikasi:
```text
Splash -> Workspace -> Lembar Periksa -> Kirim (Antrean Background) -> Riwayat -> Laporan Produksi -> Tindakan Pemulihan
Prinsip Manufacturing (Lean Six Sigma) yang Wajib Didukung oleh UI:

Jidoka: Penyimpangan kualitas atau kegagalan sistem teriluminasi secara jelas melalui indikator visual instan.

Andon (Visual Management): Status abnormal diberi penekanan warna kontras tinggi yang dinamis berdasarkan tema (Light/Dark Mode).

Genchi Genbutsu (Drill-down Data): Navigasi antarmuka memungkinkan Operator melakukan inspeksi mendalam dari tingkat Part, Material, Pos, Waktu, hingga akar penyebab Defect.

Poka-yoke (Error Proofing): Input invalid (misal: Kuantitas NG melebihi kapasitas pemeriksaan atau string non-numerik) dicegah secara mutlak di tingkat UI sebelum masuk antrean kirim.

3. Standarisasi Bahasa UI
Seluruh elemen antarmuka yang menghadap langsung ke pengguna akhir (End-User Facing UI) mutlak menggunakan Bahasa Indonesia standar operasional pabrik yang konsisten.

Hindari (Istilah Asing / Tidak Baku)	Gunakan (Istilah Standar InSpectra)
E-Checksheet / Form Checksheet	Lembar Periksa
Master Data	Data Induk
Part List / Component	Part
Defect / Reject / Rework	Defect atau Temuan NG
Submit / Post / Upload	Kirim
Save	Simpan
Delete / Remove	Hapus
Validation / Checking	Validasi
Error / Exception Failure	Gagal
Loading	Memuat
Empty / No Data Placeholder	Belum ada data
Size / Dimension	Ukuran
Waste / Scrap	Sisa Material
PIC / User / Worker	Operator
Catatan: Nama teknis arsitektur seperti Kotlin, Supabase, PostgREST, Material 3, API, Coroutine, Ktor, dan Room DB diizinkan tetap digunakan terbatas pada dokumentasi internal kode.

4. Sistem Warna (Industrial Dark SaaS & Adaptive Theme)
Sistem pewarnaan wajib menggunakan token dinamis dari MaterialTheme.colorScheme yang bersumber dari core/ui/theme/Color.kt. Warna wajib beradaptasi penuh terhadap perubahan sistem Dark Mode dan Light Mode serta rotasi layar tanpa kehilangan kontras teks (Minimal memenuhi standar WCAG 2.1 AA dengan rasio kontras 4.5:1).

A. Kontainer & Latar Belakang
Background & Surface: Gunakan colorScheme.surfaceContainer atau surfaceContainerHigh dengan palet arang/batu bara (slate/charcoal/matte grey). Dilarang keras menggunakan warna hitam solid #000000 pekat atau putih murni #FFFFFF untuk menghindari kelelahan mata (eye-strain) di area gelap pabrik.

Pemisah Antarmuka (Border): Pemisahan hierarki kontainer wajib menggunakan border tipis murni berukuran 1.dp dengan warna token colorScheme.outlineVariant.

B. Aksen & Status Operasional
Primary Aksen & CTA: Gunakan aksen Amber (Kuning Kunyit) atau Bright Cobalt Blue secara selektif hanya untuk elemen interaktif utama, tombol aksi positif (Simpan/Kirim), atau status aktif.

Status Abnormal / Peringatan (Andon): Gunakan kontainer semi-transparan colorScheme.errorContainer dipadu dengan teks colorScheme.onErrorContainer dan icon Material yang tegas.

C. Aturan Larangan Keras Warna
DILARANG melakukan hardcode warna Hex (e.g. Color(0xFFFFFFFF)) langsung di dalam komponen teks atau kartu UI.

DILARANG mendominasi layar dengan satu blok warna solid yang terlalu terang.

DILARANG menggunakan bayangan (drop shadow) berlebih untuk membentuk kedalaman visual; gunakan variasi tonal elevation (2.dp hingga 4.dp) atau outline kontainer.

5. Anatomi Layout & Blueprint Komponen
A. Anatomi Komponen Kartu (Card Blueprint)
Seluruh kartu representasi data (Part Card, Material Card, Laporan Card, Defect Card) harus memiliki struktur seragam:

Kotlin
Surface(
    shape = RoundedCornerShape(24.dp), // Radius modern untuk workspace industri
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 2.dp
) { ... }
Teks di dalam kartu tidak boleh terpotong (clipped) saat layar dirotasi ke posisi horizontal (Landscape). Gunakan modifier weight secara proporsional dalam layout Row.

B. Form Lembar Periksa & Input Field
Ringkasan Selalu Terlihat: Ringkasan akumulasi total kuantitas wajib bersifat sticky atau disematkan di area yang tidak terpengaruh oleh scroll halaman.

Field Input Tangguh (Anti-Lag Field): OutlinedTextField wajib dikonfigurasi secara optimal. Sinkronisasi state text tidak boleh memicu lag pada pengetikan. Terapkan strategi optimasi aliran state (MVI) untuk mencegah keterlambatan respon IME soft-keyboard.

Validasi Lokasi Temuan: Pesan kegagalan validasi kuantitas NG harus disajikan secara presisi di dekat field input terkait.

C. Bar Notifikasi & Komponen Feedback Sistem
Pembersihan Efek Blur: Berdasarkan evaluasi beban kerja GPU, DILARANG menggunakan modifier blur dinamis (Modifier.blur atau RenderEffect) pada komponen notifikasi, snackbar, atau dialog. Efek blur terbukti memicu kegagalan frame-dropped (Davey/Jank) di Logcat emulator dan gawai industri.

Solusi Pengganti: Gunakan latar kontainer dengan tingkat opasitas solid atau semi-transparan statis yang dikombinasikan dengan kontras warna pembatas (Outline stroke) yang tinggi.

6. Manajemen State UI & Kebijakan Data Palsu
Setiap komponen layar wajib mengimplementasikan 5 keadaan State UI secara diskrit yang dikelola melalui paradigma Unidirectional Data Flow (MVI/UDF) dengan anotasi @Immutable:

Memuat (Loading State): Menampilkan kerangka list (AppListSkeleton) yang berukuran statis agar tidak memicu pergeseran layout (layout jank/shifting).

Berhasil (Success State): Menampilkan data riil dari Room Database lokal sebagai Single Source of Truth.

Belum Ada Data (Empty State): Menyajikan penjelasan kontekstual yang jujur mengenai ketiadaan data tanpa rekayasa.

Gagal (Error State): Menampilkan pesan kegagalan teknis yang informatif lengkap dengan tombol Aksi Ulang (Retry).

Menyimpan/Mengirim (Saving/Sending Task State): Mengunci interaksi UI (input disabled) disertai indikator progres yang jelas untuk mencegah penekanan tombol ganda oleh Operator.

Strikte Kebijakan Data Palsu (No Dummy Data Policy):
Dilarang keras menggunakan data tiruan, teks placeholder Lorem Ipsum, atau komponen visual marketing palsu. Jika data dari repositori back-end (Supabase/Room) bernilai kosong atau fungsinya belum diimplementasikan di sisi logika bisnis, sistem wajib menyembunyikannya atau menampilkan keadaan kosong (Empty State) apa adanya.

7. Aksesibilitas (Accessibility) & Tipografi
Touch Target & Ukuran Elemen: Elemen interaktif/tombol wajib memiliki area sentuh minimal 48.dp x 48.dp tanpa toleransi pengecualian untuk mencegah salah tekan di area lantai produksi.

Deskripsi Konten: Seluruh komponen IconButton atau icon interaktif wajib menyertakan nilai properti contentDescription yang bermakna (Bahasa Indonesia) demi mendukung pembaca layar (Screen Reader).

Tipografi Terpantau: Menggunakan hierarki Material 3 Typography secara ketat.

Judul Kontainer / Kartu: titleMedium atau titleLarge.

Informasi Metadata / Label Kecil: labelMedium atau labelSmall.

DILARANG menggunakan letter-spacing bernilai negatif.

DILARANG melakukan kalkulasi manual skala font berdasarkan lebar resolusi layar (viewport scaling).

8. Optimalisasi Performa UI (Anti-Jank Mandate)
Untuk membersihkan masalah Choreographer Skipped Frames dan peringatan JNI Critical Lock di Logcat, ikuti aturan mutlak berikut:

Penggunaan Kunci Stabil (Lazy-List Stability): Seluruh perulangan dalam LazyColumn atau LazyVerticalGrid WAJIB menyertakan parameter key yang unik dan stabil (e.g., key = { it.id }). Jangan pernah melewatkan parameter key ini karena berakibat pada rekomposisi massal seluruh item yang memicu ANR (Application Not Responding).

Alokasi Di Luar Komposisi: Dilarang melakukan komputasi berat, manipulasi string kompleks, sorting, atau filtering koleksi data besar di dalam blok fungsi @Composable. Seluruh operasi transformasi data wajib didelegasikan ke ViewModel pada Thread Pool IO (Dispatchers.IO) atau dibungkus menggunakan fungsi remember(key) { ... } / derivedStateOf { ... }.

ViewModel Threading: Pastikan semua panggilan jaringan (Ktor) dan operasi baca-tulis database (Room) berjalan secara asinkron di bawah pengawasan CoroutineDispatchersProvider yang di-inject secara formal melalui konstruktor.

9. Standar Kebersihan & Polish Proyek
Sebuah layar UI hanya dinyatakan lolos tahap Polish jika memenuhi kriteria:

Bebas dari penumpukan komponen (no overlapping layout).

Bebas dari pemotongan teks secara tidak sengaja (no text clipped).

Komponen tombol yang berada dalam status tidak aktif (disabled button) wajib memberikan indikasi visual yang jelas, atau memicu dialog umpan balik yang menjelaskan syarat pemenuhan aksi.

Logcat bersih dari indikasi kegagalan alokasi memori (Davey duration logs terkendali di bawah ambang batas kritis).

Bebas Kredensial: Logcat dan baris kode proyek bersih secara mutlak dari token API, kunci privat, kredensial Supabase, atau data sensitif perusahaan lainnya.