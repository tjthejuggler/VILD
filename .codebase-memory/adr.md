# ADR: Reality check techniques library (seeded, swipeable banner)

**Date:** 2026-08-22 (second decision of the day)
**Status:** Accepted

## Context
The daily reality check tells the user *when* to check, but not *how*. Users familiar with lucid dreaming know the classic methods (nose pinch, finger counting, re-reading text…), but new users need ideas, and all users benefit from variety.

## Decision
1. **New first-class entity: `TechniqueItem`** (id, text, createdAt, `isSeeded`) with its own DataStore (`technique_store`) via `TechniqueRepository` — deliberately separate from advice (different concept, different banner, different lifecycle).
2. **One-time seeding of 20 classic methods** in `seedIfEmpty()`, guarded by a `has_seeded` boolean flag rather than list emptiness, so deleting all classics does not resurrect them on next launch.
3. **Own banner on the main screen** (`TechniqueBanner`, glass-styled, "REALITY CHECK IDEAS" section between the reality check card and the advice whisper): swipe left → next random (never same twice), swipe right → previous (history stack), random on app open — mirroring the advice banner interaction model.
4. **Management** via `TechniqueDialog` (add/edit/delete, ✦ marks seeded classics) opened from a card on the Settings screen.
5. State lives in `MainViewModel` as `TechniqueUiState` (list + currentIndex + history).

## Consequences
- Techniques are independent from triggers (the daily check) and advice; no cross-coupling.
- The `isSeeded` flag is display-only metadata; seeded items are fully editable/deletable.
- Seeding runs in `MainViewModel` init before observing the flow, so the banner populates on first launch.
