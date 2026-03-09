## LemurLoop Project Rules

### Purpose
- **Goal**: Keep LemurLoop’s Android codebase consistent, testable, and easy to evolve.
- **Scope**: Applies to all code in this repo, regardless of whether it was written by a human or an AI assistant.

### Architecture & Layering
- **Packages and responsibilities**
  - `data`: Data sources, repositories, network, storage, DTOs, and mappers.
  - `domain`: Use cases, business logic, domain models, and domain-level validation.
  - `ui`: Screens, ViewModels, UI state, events, and Jetpack Compose UI.
  - `di`: Dependency injection wiring only (no business logic).
  - `service` / `receiver`: Background work and system integration (alarms, notifications, broadcasts), delegating logic to `domain` and `data`.
  - `util`: Small, reusable helpers that are free of business logic and can be safely shared across layers.

- **Allowed dependencies**
  - **Domain defines repository interfaces**; the `data` layer implements them. So: `data` depends on `domain` (implements domain interfaces and uses domain models); `domain` does not depend on `data` types directly in the ideal design.
  - `ui` may depend on `domain` only. Prefer domain use cases or repository interfaces; avoid ViewModels holding concrete `data` types (repositories, DAOs, entities).
  - `service` and `receiver` may depend on `domain` (and, where necessary, platform APIs); they should not depend on `ui` components.
  - No layer may depend on test code.

- **Current exceptions (to be refactored over time)**
  - **SettingsViewModel** uses `AppDatabase` directly in `wipeAllData()`. Preferred: a domain use case or “wipe” repository interface implemented in `data`.
  - **DiagnosticLogsViewModel** uses `DiagnosticLogDao` and `DiagnosticLogEntity` directly. Preferred: a domain abstraction (e.g. diagnostic logs repository) implemented in `data`.
  - **Domain → data**: `DiagnosticLogger` (domain/manager) uses `DiagnosticLogDao` and entities. Preferred: logging behind a domain interface implemented in `data`.
  - **Android-aware domain**: Some components in `domain/manager` (e.g. `SettingsManager`) use Android `Context`, DataStore, or other platform APIs. This is a pragmatic exception; prefer moving persistence and platform details into the `data` layer over time so domain stays as use cases and business rules.

  See **docs/REFACTORING_PLAN.md** for the long-term plan to remove these exceptions.

- **General principles**
  - Prefer immutable data structures and explicit state.
  - Keep side effects at the edges (repositories, services), not inside pure domain logic where possible.
  - Keep classes small and focused; one clear responsibility per class or file.

### Threading & Coroutines
- **Main thread rules**
  - No blocking I/O on the main thread.
  - Long-running work must run on an appropriate dispatcher (e.g. `Dispatchers.IO` or `Dispatchers.Default`).

- **Coroutine usage**
  - `ui` layer:
    - Use `viewModelScope` in ViewModels for suspending work and `Flow` collection.
    - Expose hot streams to the UI via `StateFlow`, `SharedFlow`, or Compose `State`.
  - `domain` and `data`:
    - Prefer `suspend` functions and `Flow` for asynchronous operations.
    - Do not reference `viewModelScope` or other UI-scoped coroutine scopes.
  - Always use structured concurrency; avoid launching unscoped global jobs.

### Error Handling
- Prefer explicit error modeling over silent failures.
- Use one of the following patterns consistently within a feature:
  - Kotlin `Result` or a project-level wrapper type.
  - Sealed `Error`/`Failure` types for domain-level errors.
  - Exceptions only for truly exceptional conditions; avoid using them for normal control flow.
- Convert low-level errors (network, persistence) into domain-level errors in the `data` or `domain` layer before they reach the UI.
- The `ui` layer should translate domain errors into user-facing messages or states, not contain business rules.

### State Management & UI
- Treat the ViewModel as the single source of truth for each screen’s UI state.
- Expose state from ViewModels as immutable types (`StateFlow`, `LiveData`, or Compose `State`) and update them through well-defined events or intents.
- Avoid storing navigation state or global configuration directly in Composables; keep them in appropriate controllers or ViewModels.

### Introducing New Code
- **Where code belongs**
  - Business rules or decisions → `domain`.
  - Data fetching, caching, or persistence → `data`.
  - Screen-specific presentation logic and user interaction handling → `ui`.
  - Cross-cutting or wiring concerns → `di`, `service`, `receiver`, or `util` as appropriate.

- **When adding new features**
  - Keep feature-specific logic close together across layers (e.g. feature-related use cases, repositories, and screens should be easy to locate).
  - Prefer extending existing patterns over inventing new ones; if a new pattern is needed, document it in this file or a dedicated rules file.

### Use of AI Assistants
- All AI-generated code must follow these rules and existing project patterns.
- When in doubt about where to place new logic or how to shape an API, prefer:
  - Keeping domain rules in `domain`.
  - Keeping all UI-specific logic in `ui`.
  - Keeping integration details in `data`, `service`, or `receiver`.
- Significant deviations from these rules should be discussed and, if accepted, documented here or in a related rule document.

