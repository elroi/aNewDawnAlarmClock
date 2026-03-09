## MCP & Workflow Guidelines for LemurLoop

This document describes how to use Cursor's MCP integrations with LemurLoop. It complements the architectural rules in `RULE.md` and the AI collaboration guidance in `AI_COLLABORATION.md`.

### GitHub (user-github)
- **Primary usage**
  - Summarize and review pull requests for LemurLoop.
  - Compare branches before and after larger changes (e.g. settings refactors, background work changes).
  - Draft or improve PR descriptions, focusing on intent and risk areas.
- **When to use**
  - After making non-trivial changes that warrant a PR.
  - When you need a high-level review of a branch compared to `main` or another base.

### Observability (user-better-stack)
- **Use if LemurLoop emits logs or metrics to Better Stack or a similar platform.**
- **Typical workflows**
  - Investigate crashes, ANRs, or performance issues by correlating logs with code paths.
  - Pull example log entries when debugging complex background or network behavior.
- **When to use**
  - When a bug or performance issue is reported and you need real-world signals, not just code inspection.

### Backend & Database (user-postgres, user-Render, user-vercel)
- **Use only if LemurLoop relies on your own backend or database.**
- **Typical workflows**
  - Inspect schemas, example rows, or configuration values that affect the app’s behavior.
  - Verify that backend changes are compatible with the app’s data and domain models.
- **Safety guidelines**
  - Prefer read-only queries and operations, especially against staging or test environments.
  - Avoid destructive or irreversible changes via MCP unless explicitly intended and reviewed.

### Browser-Based Docs & Testing (cursor-ide-browser)
- **Docs & reference**
  - Open and read Android, Kotlin, Compose, and library documentation directly when designing or refactoring features.
- **UI testing**
  - Use for web-based tools or dashboards related to LemurLoop (e.g. feature flag consoles, backend admin UIs), not the Android app UI itself.

### General Best Practices
- Treat MCP tools as helpers that reveal external context (PRs, logs, DB state), not as substitutes for tests.
- When using MCP results to justify code changes, reference the relevant PR, log, or query in commit messages or PR descriptions.
- Prefer reproducible workflows: document recurring MCP-based debugging patterns here so they can be reused consistently.

