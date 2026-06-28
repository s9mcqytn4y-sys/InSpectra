# Implementation Plan - Project Hardening & UI/Schema Normalization

This plan addresses a comprehensive set of hardening tasks across the database, serialization, and UI layers.

## User Review Required

- **Schema Normalization**: I will normalize all table and view names to use consistent prefixes (e.g., `m_` for master, `e_` for entry/transaction, `v_` for view).
- **Serialization Hardening**: Every DTO will be audited to use `@SerialName` for every property to ensure resilience against naming changes and clear mapping to Supabase snake_case.
- **UI detaling**: "Laporan Produksi" header (Tenaga Kerja, OT) will be moved to the bottom.

## Proposed Changes

### 1. Supabase Schema Normalization & Audit
- Normalize table names to consistent `m_` (master), `e_` (entry), and `v_` (view) prefixes.
- Audit all `.sql` files to ensure snake_case naming throughout.
- Ensure all views use `CASCADE` for safe recreation.

#### [NEW] [20260702000000_schema_normalization.sql](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/supabase/migrations/20260702000000_schema_normalization.sql)
- Rename any inconsistent tables (if found during full audit).
- Update all views to follow strict `v_` prefix.

---

### 2. Serialization Hardening
- Audit all DTOs and domain models used for Supabase interactions.
- Add `@SerialName` to all properties.
- Ensure `ignoreUnknownKeys = true` is set in the global Json configuration.

#### [ChecksheetSubmitDto.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/checksheet/domain/ChecksheetSubmitDto.kt)
- Add `@SerialName` to all fields.

#### [LaporanDto.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/laporan/domain/LaporanDto.kt)
- Add `@SerialName` to all remaining fields.

---

### 3. UI Detailing & Refactoring
- Move `HeaderSection` (Tenaga Kerja, Overtime) to the bottom of the form in `LaporanProduksiScreen`.
- Enhance the Snackbar/Toast positioning to be at the top of the z-index.
- Detailing `InspectraMultiPickerSheet` with better theme harmonization.

#### [LaporanProduksiScreen.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/laporan/ui/LaporanProduksiScreen.kt)
- Move `HeaderSection` after the list of parts.

#### [AppComponents2.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/core/ui/component/AppComponents2.kt) (or where Snackbar is defined)
- Ensure Snackbar is properly positioned.

---

### 4. Performance & Logging Fixes
- Address "Skipped frames" by moving any heavy parsing or data manipulation to `Dispatchers.Default/IO`.
- Investigate the `vold` error (likely related to Android 15 target SDK or emulator specific storage issues).

---

## Verification Plan

### Automated Tests
- Run all unit tests: `.\gradlew testDebugUnitTest`
- Run build to check for compilation errors: `.\gradlew assembleDebug`

### Manual Verification
- Deploy to emulator and test the full flow of "Laporan Produksi".
- Verify UI Detailing and snackbar visibility.
- Verify Supabase data persistence through the updated serialization.
