# Implementation Plan - InSpectra Elite UI/UX Overhaul

This plan outlines the massive transformation of InSpectra from a text-centric application to an elite, selection-centric, and highly responsive digital mandate.

## User Review Required

- **Modal vs Dropdown**: We are switching from expandable cards to Modal Detail boxes for all Master Data.
- **Cutting Logic**: Material specs will now automate the input field, and sizes will be selected via dropdown.
- **Color Scheme**: Tunning white text readability and making Dark Mode more professional (Slate-based).

## Proposed Changes

### 1. Product Identity & Theme [Hardening]

#### [Theme.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/core/ui/theme/Theme.kt)
- Update color scheme to higher contrast Slate (900/950) for Dark Mode.
- Improve white text visibility in Light Mode.

#### [NEW] [InspectraEliteModal.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/core/ui/component/InspectraEliteModal.kt)
- Reusable modal component with blur background (API 31+) and smooth entrance/exit motions.

#### [NEW] [InspectraEliteSnackbar.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/core/ui/component/InspectraEliteSnackbar.kt)
- Custom Snackbar host that overrides native UI for better brand alignment.

---

### 2. Master Data [Overhaul]

#### [MasterDataScreen.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/masterdata/ui/MasterDataScreen.kt)
- Implement `DetailModal` for Part, Material, Supplier, and Defect.
- Add Sort/Filter for "Komoditas".
- Correct the logic for "Belum Lengkap" (incomplete relations) and "Unknown" badges.

#### [MaterialMasterCard.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/masterdata/ui/components/MaterialMasterCard.kt)
- Remove `AnimatedVisibility` expansion.
- Add "Info/Detail" icon button to trigger Modal.

---

### 3. Cutting Process [Hardening]

#### [CuttingScreen.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/cutting/ui/CuttingScreen.kt)
- Automate "Spesifikasi Material" text field based on selected material.
- Change "Ukuran Cutting" from manual input to priority dropdown (with manual override option).
- Implement inline field errors instead of summary list.

#### [CuttingViewModel.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/cutting/ui/CuttingViewModel.kt)
- Improve validation logic to trigger per-field error states.
- Debug and fix Supabase submission failures (Audit RPC permissions).

---

### 4. Data Consistency (Checksheet)

#### [DataAcuanChecksheet.kt](file:///C:/Users/Acer/AndroidStudioProjects/InSpectra/app/src/main/java/com/primaraya/inspectra/fitur/checksheet/domain/DataAcuanChecksheet.kt)
- Populate missing material defects for Press parts.
- Update all composite part/material relations based on provided reference images.

---

## Verification Plan

### Automated Tests
- Update `LaporanViewModelTest` and `ChecksheetViewModelTest` to match new validation flows.
- Run: `.\gradlew :app:testDebugUnitTest`

### Manual Verification
- Deploy to tablet and verify:
    - Detail Modals look & feel (Blur, Smoothness).
    - Cutting form automation.
    - Master Data filtering.
    - Data sync consistency between Checksheet and Laporan.
