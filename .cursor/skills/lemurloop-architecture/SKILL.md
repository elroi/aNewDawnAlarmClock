---
name: lemurloop-architecture
description: Captures LemurLoop's Android architecture, package responsibilities, and allowed dependencies. Use when adding or refactoring features, deciding where logic belongs across data/domain/ui, or reviewing layering decisions.
---

# LemurLoop Architecture

## Instructions

- **Overall goal**
  - Keep LemurLoop's code structured into clear layers so features are easy to understand and change.

- **Layers and responsibilities**
  - `data`: Data sources, repositories, network, storage, DTOs, and mapping to domain models.
  - `domain`: Use cases, business rules, domain models, and domain-level validation.
  - `ui`: Screens, ViewModels, UI state, events, and Composables.
  - `di`: Dependency injection wiring only (no business logic).
  - `service` / `receiver`: Background work, alarms, notifications, and system integration that delegates to `domain`/`data`.
  - `util`: Small, reusable helpers that do not embed business rules and can be shared safely.

- **Allowed dependency directions**
  - **Domain defines repository interfaces**; `data` implements them. So: `data` → `domain` (implements interfaces, uses domain models). `domain` does not depend on `data` types in the ideal design.
  - `ui` → `domain` only. ViewModels should depend on domain use cases or repository interfaces, not on concrete `data` types (DAOs, entities, AppDatabase).
  - `service` / `receiver` → `domain` (and platform APIs as needed); they do not depend on `ui`.
  - Test code must not be depended on by production code.

- **Current exceptions (see RULE.md; refactor over time)**
  - SettingsViewModel uses AppDatabase in `wipeAllData()`; DiagnosticLogsViewModel uses DiagnosticLogDao/Entity directly; DiagnosticLogger (domain) uses data-layer DAO/entity. Some domain managers (e.g. SettingsManager) are Android-aware (Context, DataStore). Prefer introducing domain abstractions and moving persistence to `data` when touching these areas.

- **Placing new code**
  - Put business rules or decisions (what should happen) in `domain`.
  - Put integration details (how we talk to network/storage/system) in `data`, `service`, or `receiver`.
  - Put presentation concerns (how we show state and handle user interaction) in `ui`.
  - Keep cross-cutting wiring in `di` or small, focused helpers in `util`.

- **Coroutines and threading**
  - Use `viewModelScope` only in the `ui` layer.
  - Use `suspend` functions and `Flow` in `domain`/`data` for async work.
  - Avoid blocking the main thread; run heavy work on appropriate dispatchers.

- **Error handling**
  - Translate low-level errors into domain-level errors before reaching the UI.
  - Prefer explicit error types or `Result`-like wrappers over silent failures.

## Examples

- **Example: Adding a new feature**
  - Define or update a use case in `domain` to represent the core action.
  - Implement repository or data source changes in `data` to support the use case.
  - Add or update a screen/ViewModel in `ui` that calls the use case and renders the resulting state.

- **Example: Background-triggered work**
  - Handle the trigger in `service` or `receiver`.
  - Delegate business decisions to `domain` use cases.
  - Persist or fetch data through `data` repositories.

