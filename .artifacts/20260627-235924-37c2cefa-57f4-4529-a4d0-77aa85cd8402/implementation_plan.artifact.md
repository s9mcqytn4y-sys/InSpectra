# Implementation Plan - Phase 1 & 2 + Massive Elite UI/UX Hardening

This plan covers the implementation of Storage (Phase 1), Offline-First (Phase 2), and a comprehensive cleanup of serialization and UI/UX issues across InSpectra.

## User Review Required

- **Data Migration**: Applying the new material specs to `DataAcuanChecksheet.kt` and Supabase seed.
- **Permission Requests**: Adding Camera and Storage permissions to `AndroidManifest.xml`.
- **Theme Standardization**: Using Material 3 `surfaceContainer` variants for card backgrounds.

## Proposed Changes

### 1. Data Integrity & Serialization [Hardening]

#### [ChecksheetModels.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/checksheet/domain/ChecksheetModels.kt)
- Convert `ImmutableList` to `List` in all `@Serializable` DTOs (e.g., `InputDefect`, `PayloadChecksheet`) to fix submission crashes.
- Convert back to `ImmutableList` only at the UI state level.

#### [MasterDataModels.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/masterdata/domain/MasterDataModels.kt)
- Add `@SerialName` to every field in every DTO for resilient Supabase mapping.
- Update `MasterPartDto` to include the logic for "Siap Input" vs "Incomplete Data".

---

### 2. Elite UI/UX Overhaul [Master Data]

#### [MasterDataScreen.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/masterdata/ui/MasterDataScreen.kt)
- Implement high-fidelity **Modal Details** with background blur (API 31+).
- Replace card expansion with modal trigger icon.
- Standardize colors to adapt to Light/Dark mode with high legibility.

#### [DataIndukTopBar.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/masterdata/ui/components/DataIndukTopBar.kt)
- Refine TopBar and Tab anatomy for better contrast.

---

### 3. Cutting Process [Elite Automation]

#### [CuttingScreen.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/cutting/ui/CuttingScreen.kt)
- Automate "Spesifikasi Material" based on selection.
- Implement inline field errors for immediate validation feedback.
- Change "Ukuran Cutting" to a dropdown-first selection.

---

### 4. Phase 1 & 2 [Industrial Foundation]

#### [SupabaseStorageDriver.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/core/data/SupabaseStorageDriver.kt)
- Full implementation of `uploadFile` with status 429/503 handling.

#### [InSpectraDatabase.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/core/data/local/InSpectraDatabase.kt)
- Setup full Room entities for offline caching and submission queuing.

---

## Verification Plan

### Automated Tests
- Run updated `LaporanViewModelTest` and `ChecksheetViewModelTest`.
- Run build to verify R8/ProGuard won't break serialization.

### Manual Verification
- Verify submission of Checksheet and Laporan on Tablet.
- Verify Modal detailing on all Master Data tabs.
- Verify theme adaptation (Dark/Light) across all primary screens.
