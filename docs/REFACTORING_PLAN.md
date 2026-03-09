# LemurLoop Long-Term Refactoring Plan

This document tracks refactors that align the codebase with the architecture and UI rules in [RULE.md](../RULE.md) and `.cursor/rules/`. Items are ordered by impact and dependency; later items can build on earlier ones.

---

## 1. Layering: Remove UI → Data Dependencies

**Goal:** ViewModels and UI layer must not depend on concrete `data` types (DAOs, entities, `AppDatabase`). They should depend only on `domain` (use cases or repository interfaces).

### 1.1 SettingsViewModel – Wipe All Data

- **Current:** `SettingsViewModel` injects `AppDatabase` and calls `database.alarmDao()`, `database.sleepRecordDao()`, and `settingsManager.clearAll()` in `wipeAllData()`.
- **Target:**
  - Add a domain abstraction, e.g. `WipeAllDataUseCase` or `AppDataRepository` interface in `domain` with a single method such as `suspend fun wipeAll()`.
  - Implement it in `data` (e.g. `WipeAllDataUseCaseImpl` or `AppDataRepositoryImpl`) that uses Room DAOs and DataStore/SettingsManager.
  - Inject the use case (or repository) into `SettingsViewModel` and replace direct `AppDatabase` usage with a call to that abstraction.
- **Benefit:** UI no longer depends on `data`; wipe behavior is testable via domain interface; single place to extend if new data sources are added.

### 1.2 DiagnosticLogsViewModel – Diagnostic Logs Repository

- **Current:** `DiagnosticLogsViewModel` injects `DiagnosticLogDao` and `DiagnosticLogEntity`; exposes `List<DiagnosticLogEntity>` to the UI.
- **Target:**
  - Define a `DiagnosticLogRepository` interface in `domain` (e.g. `getLatestLogs(): Flow<List<DiagnosticLog>>`, `clearAll()`, with a domain model `DiagnosticLog` if desired).
  - Implement in `data` using `DiagnosticLogDao` and map entities to domain models.
  - Inject `DiagnosticLogRepository` into `DiagnosticLogsViewModel`; remove DAO and entity imports from `ui`.
- **Benefit:** Diagnostic logs screen follows the same layering as alarms; UI is decoupled from persistence details.

---

## 2. Layering: Remove Domain → Data Dependencies

**Goal:** Domain layer must not depend on `data` implementation types (DAOs, entities). Persistence and logging should be behind interfaces implemented in `data`.

### 2.1 DiagnosticLogger – Logging Interface

- **Current:** `DiagnosticLogger` (in `domain/manager`) uses `DiagnosticLogDao` and `DiagnosticLogEntity` directly.
- **Target:**
  - Define a small interface in `domain`, e.g. `DiagnosticLogWriter` or extend `DiagnosticLogRepository` with a write method used by domain.
  - Implement in `data` (e.g. in the same class that implements `DiagnosticLogRepository` or a dedicated writer).
  - Inject that interface into `DiagnosticLogger`; remove `data` imports from `domain`.
- **Benefit:** Domain no longer depends on Room/entities; logging can be mocked or replaced without touching domain.

---

## 3. Android-Aware Domain – Move Persistence to Data

**Goal:** Reduce Android and persistence concerns inside `domain`. Prefer `data` for storage and platform access; `domain` for business rules and use cases.

### 3.1 SettingsManager – Settings Repository / Data Layer

- **Current:** `SettingsManager` lives in `domain/manager` and uses Android `Context`, DataStore, and `preferencesDataStore` directly.
- **Target (long-term):**
  - Introduce a `SettingsRepository` (or `UserPreferencesRepository`) interface in `domain` that exposes flows and suspend save methods for settings and alarm defaults.
  - Implement it in `data` using DataStore (and any other storage). Move DataStore keys, serialization, and default values into the `data` implementation.
  - Keep a thin `SettingsManager` in `domain` if it still holds business logic (e.g. validation, derived state), or replace usages with the repository and use cases.
  - Update `SettingsViewModel`, `AlarmViewModel`, `BriefingGenerator`, and other consumers to depend on the repository (or use cases that use it) instead of `SettingsManager` for persistence.
- **Benefit:** Clear separation: `data` owns storage; `domain` owns rules; easier to test and to swap storage later.
- **Note:** This is a larger refactor; can be done incrementally (e.g. one settings group at a time) and in coordination with 1.1 if a single “app data” or “settings” abstraction is introduced.

---

## 4. UI State – Single UiState Per Screen (Optional)

**Goal:** Prefer one immutable `UiState` data class and a single event channel per screen, as described in `.cursor/rules/ui-compose.mdc`. Improves testability and consistency.

### 4.1 SettingsScreen / SettingsViewModel

- **Current:** Many separate `StateFlow`s (location, isCelsius, hasChanges, alarmDefaults, etc.) and no single `SettingsUiState`.
- **Target:**
  - Define `data class SettingsUiState(...)` and optionally `sealed class SettingsUiEvent` (or keep existing method-based events).
  - Consolidate all read-only state into `UiState`; expose `StateFlow<SettingsUiState>` and update via a single `onEvent` or existing methods that update internal state and emit a new `UiState`.
  - Update `SettingsScreen` to collect one state and dispatch events. Consider splitting very large state into a few sub-state classes if needed for readability.
- **Benefit:** Single place to reason about screen state; easier to snapshot for tests and to add loading/error states consistently.

### 4.2 AlarmViewModel / AlarmListScreen

- **Current:** Multiple `StateFlow`s (alarms, defaultAlarmSettings, alarmCreationStyle, etc.).
- **Target:** Same idea as 4.1: one `AlarmListUiState` (and optional events), single `StateFlow`, composable collects once.
- **Benefit:** Consistency with new screens; clearer state shape for future features.

---

## 5. Testing and Documentation

- **Testing:** Add or extend unit tests for any new use cases or repository interfaces (e.g. wipe-all, diagnostic logs). Prefer testing domain and data boundaries; keep UI tests for critical flows.
- **Docs:** After each refactor, update [RULE.md](../RULE.md) “Current exceptions” and remove the item once the exception is resolved. Update the architecture skill if dependency directions or examples change.

---

## Priority and Ordering

| Order | Item | Depends on | Effort |
|-------|------|------------|--------|
| 1 | 1.2 Diagnostic logs repository (UI + domain) | — | Small |
| 2 | 2.1 DiagnosticLogger interface | 1.2 | Small |
| 3 | 1.1 Wipe-all use case / repository | — | Small–medium |
| 4 | 3.1 Settings persistence in data | — | Large |
| 5 | 4.1 / 4.2 Single UiState (Settings, Alarm) | Optional | Medium |

Items 1 and 2 can be done together (diagnostic logs). Item 3 is independent. Item 4 can be split into smaller steps (e.g. one settings group at a time). Item 5 (testing/docs) should follow each refactor.

---

## Tracking

- When a refactor is completed, move it to a “Completed” section at the bottom of this file with a short note and PR or commit reference.
- Revisit this plan when adding new features that touch the same layers (e.g. new settings or new data to wipe).
