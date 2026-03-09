---
name: lemurloop-ui-and-settings
description: Guides LemurLoop's UI, screen, and settings patterns. Use when implementing or modifying screens, navigation, settings, or About/privacy surfaces in the Android app.
---

# LemurLoop UI & Settings

## Instructions

- **UI package structure**
  - `ui/MainScreen.kt` and `ui/screen/*`: Top-level screens and screen-specific Composables.
  - `ui/viewmodel/*`: ViewModels and UI state holders.
  - `ui/components/*`: Reusable visual components that are state-light or state-less.
  - `ui/navigation/*`: Navigation graph and route definitions.
  - `ui/theme/*`: Colors, typography, and theming.

- **Screen patterns**
  - Each feature screen should expose a single public entry Composable named `XxxScreen`.
  - Screen state is driven by a ViewModel. **Preferred:** one `UiState` data class and events. **Existing:** some screens (Settings, Alarm list) use multiple `StateFlow`s; acceptable until refactored.
  - Decompose complex layouts into smaller private Composables colocated with the screen.

- **State & events**
  - ViewModels: expose immutable state (single `StateFlow<UiState>` preferred, or multiple StateFlows in existing screens). Handle events via clearly named functions or an `onEvent` API.
  - Composables: read state, render UI, send events to ViewModel. Avoid side effects in Composables; use ViewModel or LaunchedEffect.

- **Navigation**
  - Keep route names and arguments defined centrally in `ui/navigation`.
  - Screens should receive only the arguments they need; avoid passing repositories or heavy dependencies directly.
  - Back navigation and deep links should be handled consistently through the navigation layer.

- **Settings behavior**
  - Each setting should map to a single source of truth (e.g. DataStore or another persistent store), not duplicated state.
  - Preference toggles and options should be reflected in `UiState` and persisted through the appropriate `domain`/`data` APIs.
  - Destructive or irreversible actions should include clear labeling and, where appropriate, confirmation steps.

- **About / privacy surfaces**
  - Use **BuildConfig** (VERSION_SUFFIX, BUILD_DATE) and **PackageManager** for version info (see AboutScreen.kt). Do not hardcode version strings in the UI.
  - Consistently show app name, version, and key legal/privacy links; use a consistent layout for acknowledgements and licenses.
  - Tone should be clear and concise; avoid mixing technical jargon with user-facing copy unnecessarily.

## Examples

- **Example: Adding a new setting**
  - Add a backing field in the appropriate persistent store via `data` and a domain-level accessor or use case.
  - Extend the relevant ViewModel `UiState` to include the setting.
  - Add a row or control in the Settings screen Composable that binds to the ViewModel state and events.

