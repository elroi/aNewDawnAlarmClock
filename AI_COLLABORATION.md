## AI Collaboration in LemurLoop

This project uses multiple AI assistants (for example Cursor and AntiGravity). Git is the single source of truth, and all assistants must work against the same repository state.

### General Principles
- **One source of truth**: Only the committed code in this repo is authoritative. Avoid long-lived local changes that never get pushed.
- **Clear branches**: Use feature branches for non-trivial work and give branches descriptive names (e.g. `feature/settings-improvements`).
- **Small, reviewable changes**: Prefer small, focused PRs over very large ones, regardless of which assistant helped write the code.

### Using Cursor
- Use Cursor when:
  - You want to refactor or extend architecture across layers (`data`/`domain`/`ui`).
  - You want to evolve project rules, skills, or MCP workflows.
  - You need multi-file reasoning or guidance that follows the documented rules in `RULE.md` and `.cursor/rules/`.
- Cursor should:
  - Follow the architecture and rules defined in `RULE.md` and `.cursor/rules/*.mdc`.
  - Update or reference project docs (plans, rules) as part of larger design changes.

### Using AntiGravity
- Use AntiGravity when:
  - You want help on a specific feature or flow and prefer its UX or prompts.
  - You are iterating on a change that already lives in a feature branch.
- Before starting a session in AntiGravity:
  - Commit and push any relevant changes from Cursor so AntiGravity can read the latest code.
  - If AntiGravity works from a remote mirror (e.g. GitHub), make sure the correct branch is selected.

### Switching Between Assistants
- When switching from Cursor to another assistant:
  - Ensure your work is saved and committed where appropriate.
  - Optionally summarize recent changes (especially intent) in the new assistant’s chat or notes.
- When switching back to Cursor:
  - Pull the latest changes and open the relevant branch.
  - If another assistant significantly changed architecture or patterns, consider updating `RULE.md` or rules files to keep them aligned.

### Conflict Avoidance
- Avoid having multiple assistants make significant edits to the same files at the same time.
- Use Git diffs to review and understand what each assistant changed before merging.
- If different assistants propose conflicting patterns, prefer the one that best aligns with `RULE.md` and the `.cursor/rules/*.mdc` files, or update the rules to reflect a deliberate new direction.

