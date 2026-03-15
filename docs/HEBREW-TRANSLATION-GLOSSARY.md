# Hebrew Translation Glossary (LemurLoop — Alarm Clock Domain)

Use this glossary so Hebrew strings are consistent and natural across the app. Prefer **context-aware** phrasing over literal word-for-word translation.

## Domain terms

| English | Hebrew | Notes |
|--------|--------|--------|
| **Alarm** (the clock / wake-up) | **שעון מעורר** / **השעון** | In sentences use "השעון" or "שעון מעורר". For short labels (e.g. list item, default label) **שעון** is OK. Avoid **אזעקה** (siren/emergency) unless the context is clearly "alert" (e.g. notification). |
| **Snooze** (noun / button) | **דחייה** | Duration: "משך דחייה". Button: "כפתור דחייה". Avoid **איחור** (lateness). |
| **Dismiss** (turn off alarm) | **כבה** / **סיום** | Use **כבה** for the main "Dismiss" button (turn off). **סיום** for "dismissal" in section titles. Never use **בטל** here (that means Cancel). |
| **Cancel** | **ביטול** (noun), **בטל** (button) | Only for canceling an action, not for turning off the alarm. |
| **Undo** (e.g. after delete) | **החזר** or **בטל מחיקה** | Distinct from "Cancel". |
| **Accountability buddy** | **חבר אחריות** (singular), **חברי אחריות** (plural) | Section title can be "חברי אחריות" or "אחריות השכמה 🤝". |
| **Briefing** | **בריפינג** | Keep as loanword for product identity. |
| **Wake-up / wake you up** | **השכמה** / **להעיר** | Already in use. |
| **The Drill Sergeant** (persona) | **המפקד** | Conveys authority; avoid only "הסמל" (loses "drill" feel). |
| **Surprise Me** | **הפתעה** | Natural for "pick for me at random". |
| **Legal** (About/Credits) | **תנאים ומשפט** or **מידע משפטי** | Not "משפטי" alone (reads as "sentences"). |
| **Vibration pattern** | **דפוס רטט** or **מקצב רטט** | Not "דפוס קצב" (rhythm pattern) for vibration context. |
| **Message preview** | **תצוגה מקדימה של ההודעה** or **תצוגת ההודעה** | Clearer than "תצוגת הודעה". |

## Tone

- **Friendly and clear.** Avoid stiff or bureaucratic phrasing.
- **Second person** where English uses "you" (e.g. "תתעורר", "שתדע").
- **Keep placeholders** (`%1$s`, `%1$d`, `{name}`) unchanged; ensure Hebrew sentence order still reads naturally.

## Reference

- Source strings: [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml)
- Hebrew (runtime): [app/src/main/res/values-iw/strings.xml](../app/src/main/res/values-iw/strings.xml)
- RTL and locale: [RTL-LOCALIZATION.md](RTL-LOCALIZATION.md)
