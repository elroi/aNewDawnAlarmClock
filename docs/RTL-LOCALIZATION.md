# RTL and Hebrew Localization

This document describes how right-to-left (RTL) and app language are applied, any intentional LTR exceptions, and a manual verification checklist for Hebrew/English.

## Where locale and RTL are configured

- **Application:** [LemurLoopApp.kt](../app/src/main/java/com/elroi/lemurloop/LemurLoopApp.kt) overrides `attachBaseContext` to read the stored app language from DataStore (key `app_language`, same store as Settings) and wrap the base context with a `Configuration` that has the chosen locale (`he` or `en`). All activities therefore inherit the correct locale and layout direction from the start.
- **MainActivity:** Still overrides `attachBaseContext` (using AppCompat/DataStore) and `getResources()` to return localized resources, and provides `LocalConfiguration provides resources.configuration` in Compose so layout direction and `stringResource()` use the selected language. This keeps behavior correct when the user changes language and the activity is recreated.
- **Standalone activities (AlarmActivity, WakeupCheckActivity):** They do not override `attachBaseContext` or `getResources()`. They receive the Application’s wrapped context, so their configuration and resources already use the app’s chosen language. Each also provides `LocalConfiguration provides resources.configuration` in `setContent` so Compose uses that configuration for RTL layout.

Language is stored in DataStore (`user_settings`) under the key `app_language`; values are `"system"`, `"en"`, or `"he"`. When applying locale (AppCompat, Configuration), the app uses the tag **`iw`** for Hebrew so that resources load from [values-iw/strings.xml](../app/src/main/res/values-iw/strings.xml), which works reliably on all devices. The UI (e.g. Settings language list) still shows/store "he"; it is translated to "iw" when calling Android/AppCompat APIs. Hebrew strings are kept in both `values-he` and `values-iw` (same content) for compatibility.

## Intentional LTR or direction-specific behavior

- **Math challenge (AlarmActivity):** The math problem and numeric input (e.g. `3 + 5 =`) are left-to-right for readability. The surrounding layout (buttons, labels) still follows RTL when the app is in Hebrew.
- **Time display:** Times (e.g. `10:30`) are typically shown in the system/default format; they are not forced to RTL. Time pickers follow platform behavior.
- **Numeric and formula content:** Any `Text` or `TextField` that shows only digits or arithmetic may be left-to-right. No code currently forces `LayoutDirection.Ltr` on these; if added for clarity, it should be documented here.

## Translation (Hebrew)

Hebrew strings live in [values-iw/strings.xml](../app/src/main/res/values-iw/strings.xml) (and a copy in [values-he/strings.xml](../app/src/main/res/values-he/strings.xml)). For consistent, context-aware wording in the alarm-clock domain, see **[HEBREW-TRANSLATION-GLOSSARY.md](HEBREW-TRANSLATION-GLOSSARY.md)**. When adding or editing Hebrew strings, keep both `values-iw` and `values-he` in sync and follow the glossary.

## RTL verification checklist (manual QA)

Run the app with **App language** set to **Hebrew** (Settings → App language → עברית), then restart. Verify the following on each screen.

| Screen / flow | Checks |
|---------------|--------|
| **Onboarding** (all steps) | (1) All text in Hebrew. (2) Layout is RTL (content starts from the right). (3) Back/Next buttons on correct sides. (4) No clipped text. |
| **Alarm list** | (1) Title and settings icon on correct side for RTL. (2) FAB (add alarm) on correct side. (3) List items align RTL; swipe-to-delete feels natural (swipe left or right to reveal delete). (4) Hebrew strings. |
| **Alarm detail (create/edit)** | (1) Top bar: back and title RTL. (2) Form fields and labels RTL. (3) Save and other actions on correct side. (4) Hebrew strings. |
| **Alarm creation wizard** (all steps) | (1) Back/forward arrows point correctly. (2) Step content and buttons RTL. (3) Hebrew strings. |
| **Settings** | (1) Screen title and back RTL. (2) List and subscreens (Help, About, Diagnostic logs, language selector) RTL. (3) Hebrew strings. |
| **Alarm firing (AlarmActivity)** | (1) Snooze, dismiss, and challenge UI RTL when language is Hebrew. (2) Math problem can remain LTR. (3) Hebrew strings for buttons and labels. |
| **Wake-up check (WakeupCheckActivity)** | (1) Layout RTL. (2) Hebrew strings. |

**Regression:** Switch back to **English**, restart, and confirm (1) Layout is LTR and (2) All strings in English.

## Reference

- [Hebrew RTL UI/UX Review Plan](../.cursor/plans/hebrew_rtl_ui_ux_review_246661b8.plan.md)
- [Troubleshoot language crash and strings](../.cursor/plans/troubleshoot-lang-crash-and-strings.md)
