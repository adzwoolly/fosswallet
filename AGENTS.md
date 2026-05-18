# FOSSWallet — Agent Orientation

FossWallet is an Android wallet app (Kotlin, Jetpack Compose, minSdk 28) that stores and manages Apple Passbook / PKPass format loyalty cards, tickets, and boarding passes. It has no server component — everything runs on-device.

Package root: `nz.eloque.foss_wallet`  
Source root: `app/src/main/java/nz/eloque/foss_wallet/`

---

## Architecture overview

The app follows a standard Android MVVM pattern with Hilt DI:

```
UI (Compose screens)
  └── ViewModel (HiltViewModel)
        └── PassStore  ← thin service layer, orchestrates everything
              ├── PassRepository  ← Room DAO wrapper + file I/O
              ├── PassLocalizationRepository
              ├── TagRepository
              ├── PassbookApi  ← OkHttp, pass update polling
              ├── NotificationService
              └── UpdateScheduler  ← WorkManager
```

`PassStore` (`persistence/PassStore.kt`) is the single entry point for all pass mutations. ViewModels call it; nothing below it knows about UI.

`PassRepository` (`persistence/pass/PassRepository.kt`) wraps `PassDao` and handles file-system operations (bitmaps + `original.pkpass` stored under `filesDir/<passId>/`).

DI wiring lives entirely in `app/AppModule.kt`.

---

## Data model

### Core entities (Room DB `wallet_db`, version 25)

| Entity | Key fields | Notes |
|--------|-----------|-------|
| `Pass` | `id` (SHA-256 of content or barcode), all PKPass fields | Images stored as files, not in DB |
| `PassMetadata` | `passId`, `archived`, `autoArchive`, `groupId`, `renderLegacy` | One-to-one with Pass |
| `PassLocalization` | `passId`, `lang`, `key`, `value` | From `.lproj/pass.strings` |
| `PassGroup` | `id` (Long) | Groups passes in the wallet list |
| `Tag` | `label` | User-defined tags |
| `PassTagCrossRef` | `passId`, `label` | Many-to-many join |
| `Attachment` | `passId`, `fileName` | File stored at `filesDir/<passId>/attachments/<fileName>` |

### Pass field structure

A `Pass` carries five ordered field lists (`headerFields`, `primaryFields`, `secondaryFields`, `auxiliaryFields`, `backFields`), each a `List<PassField>`. `PassField` has a `key`, optional `label`, `PassContent` (plain text, date, or number), and optional `changeMessage`.

### Pass types

`PassType` is a sealed class: `Generic`, `Event`, `Coupon`, `StoreCard`, `Boarding(TransitType)`. The JSON key for each type (`generic`, `eventTicket`, `coupon`, `storeCard`, `boardingPass`) is stored on the type object.

### Pass ID

For imported `.pkpass` files the ID is `SHA-256(pass.json content)`. For passes created in-app it is `SHA-256(barcodes joined by "|")`. The ID is also used as the directory name for stored files.

---

## Data flow

### Importing a `.pkpass` file

```
bytes (ZIP)
  → PassLoader.load()              // unzips, extracts images & localisations
      → PassParser.parse()         // builds Pass from pass.json
  → PassLoadResult
  → PassStore.add()
      → PassRepository.insert()    // DB row + PassMetadata + writes files to disk
      → PassLocalizationRepository.insert()  // per-language strings
      → UpdateScheduler.scheduleUpdate()     // if pass has webServiceURL
```

`PassLoader` (`persistence/loader/PassLoader.kt`) handles multi-resolution images (`@2x`, `@3x`) by picking the highest-resolution variant.

### Creating a pass in-app

```
CreateViewModel.savePass()
  → PassCreator.create()       // builds Pass domain object
  → Coil / BitmapFactory       // loads user-selected images
  → PassStore.create()
      → PassRepository.insert()
```

### Pass update (background sync)

`UpdateWorker` (WorkManager) calls `PassStore.update(pass)` → `PassbookApi.getUpdated(pass)` which hits `{webServiceURL}/v1/passes/{passTypeIdentifier}/{serialNumber}` with `Authorization: ApplePass {authToken}`. On success it re-inserts the pass and fires change-message notifications.

### Backup / restore

`BackupStore.exportBackup()` writes a `.fosswallet` ZIP containing `backup.json` (all DB data) plus `passes/<passId>/` image trees.  
`BackupStore.importBackup()` reads that ZIP and re-inserts everything inside a single DB transaction.

---

## Navigation

Compose Navigation with `NavHostController`. Routes are defined as `sealed class Screen` objects in `ui/WalletApp.kt`:

| Route | Screen |
|-------|--------|
| `wallet` | Main wallet list |
| `pass/{passId}` | Single pass view |
| `edit/{passId}` | Edit existing pass |
| `create` | Create pass (with optional barcode pre-fill) |
| `advanced_add` | Import from file / URL |
| `scan` | Camera barcode scanner |
| `archive` | Archived passes |
| `settings` | App settings |
| `webview/{url}` | In-app web view |

---

## Key packages at a glance

| Package | Purpose |
|---------|---------|
| `model/` | Data classes and domain entities |
| `parsing/` | `PassParser`, `FieldParser`, `LocalizationParser` — JSON → model |
| `persistence/` | Room DB, repositories, `PassStore`, `SettingsStore`, backup |
| `persistence/loader/` | ZIP loading, bitmap extraction |
| `api/` | `PassbookApi` (OkHttp), `UpdateWorker`, `UpdateScheduler` |
| `ui/screens/` | One sub-package per screen, each with `*Screen`, `*View`, `*ViewModel` |
| `ui/card/` | Pass card rendering (front, fields, barcodes) |
| `ui/components/` | Shared Compose components |
| `notifications/` | Pass-update change-message notifications |
| `contentprovider/` | `CatimaContentProvider` for Catima loyalty app interop |

---

## Settings

`SettingsStore` (`persistence/SettingsStore.kt`) wraps `SharedPreferences`. Keys: sync enabled/interval, barcode position (Top/Center/Bottom), sort option, pass-view brightness boost, delete confirmation toggle.

---

## Building

```bash
# Debug APK (installs alongside release, suffix .dev)
./gradlew assembleDebug

# Check for compile errors without building a full APK
./gradlew compileDebugKotlin

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Release APK (requires signing config — see build-release.sh)
./build-release.sh
```

> **Tip:** Debug builds have significant scroll-lag overhead that does not exist in release. Use `assembleRelease` (via `build-release.sh`) or a benchmark build to verify UI performance.

Unit tests live under `app/src/test/`. Sample pass JSON fixtures are in `app/src/test/res/`.

---

## Tech stack

| Library | Use |
|---------|-----|
| Jetpack Compose + Material 3 | UI |
| Hilt | Dependency injection |
| Room | Local database |
| WorkManager | Background pass updates |
| OkHttp | HTTP for pass updates |
| Coil | Image loading |
| ZXing / zxing-cpp | Barcode generation & scanning |
| CameraX | Camera for scanner |
| bcbp-parser | BCBP boarding pass parsing |
| aboutlibraries | Open-source credits screen |
