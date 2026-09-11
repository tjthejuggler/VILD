# ADR: Seeded Reality Check Content Libraries (2026-09-11)

## Context
Users reported too many repeats in "Today's Reality Check" (triggers) and "Reality Check Ideas" (techniques). The technique list had 20 classics; the trigger list shipped empty with a single fallback line.

## Decision
- **Trigger library**: added `SEEDED_TRIGGERS` (50 prompts) in `RealityCheckRepository`, seeded once via `seedIfEmpty()` guarded by a `triggers_seeded` DataStore flag. Prompts are organised by expert practice: LaBerge prospective-memory state tests (doorway/phone/mirror/faucet anchors), false-awakening protocol (check on every waking), dream-sign spotting, physics probes, metacognition.
- **Technique library**: added `EXPANDED_TECHNIQUES` (42 new checks) in `TechniqueRepository`, seeded via a *separate* `seedExpandedIfAbsent()` pass with its own `has_seeded_expanded_v2` flag so existing installs receive it without violating the "never resurrect deleted classics" guarantee. Sources: r/LucidDreaming community lists, LucidWiki, World of Lucid Dreaming, oneironauts.io, The Lucid Guide.
- **Duplicate protection**: both repositories' `add()` silently ignore exact-text duplicates; seeders skip texts the user already has.

## Consequences
- Idea banner: 62 checks; trigger picker: 50 prompts on first run. Daily variety greatly increased.
- Seeding stays non-destructive: user edits/deletions always win over seeds (flag-guarded, text-deduped).
- Future library expansions should follow the same pattern: new list constant + new flag + `seedXIfAbsent()`.