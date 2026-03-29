# Roadmap: Starsector Cloning Mod

## Overview

Four phases take the mod from a data skeleton that proves the mod loader accepts it, through a
buildable tier-1 industry stub with resource demands, through full mechanics (growth formula, AI
cores, Biofactory Embryo, stability penalty, story point upgrade), to a complete three-tier
upgrade chain with LunaLib-exposed tuning knobs. Each phase ends with something verifiable
in-game; no phase requires the next to be testable.

## Phases

- [ ] **Phase 1: Data Skeleton** — industries.csv wired for all three tiers; mod loads without crash
- [ ] **Phase 2: Working Tier 1 Stub** — Clone Vats is buildable and consumes resources; build pipeline proven
- [ ] **Phase 3: Full Mechanics** — all growth, booster, and downside logic implemented on tier 1 via BaseCloning
- [ ] **Phase 4: Tiers 2-3 and Settings** — full upgrade chain functional; LunaLib settings wired; balance tuned

## Phase Details

### Phase 1: Data Skeleton
**Goal**: The mod loads into Starsector with all three industry IDs registered, names visible, and the upgrade chain wired — no Java required.
**Depends on**: Nothing (first phase)
**Requirements**: IND-01, IND-02, IND-03, IND-04
**Success Criteria** (what must be TRUE):
  1. The game launches with the mod enabled and no crash or error log entries from this mod.
  2. Clone Vats appears in the colony industry build menu as a buildable industry.
  3. The Upgrade button is present on a built Clone Vats and leads to "Cloning"; the Upgrade button on Cloning leads to "Clone Mass Manufactorium".
  4. Each tier displays a distinct name and description in the industry card UI.
**Plans**: TBD

Plans: (to be created by plan-phase)

---

### Phase 2: Working Tier 1 Stub
**Goal**: Clone Vats can be built, functions as a real industry, and consumes organics and organs as declared resource demands — build pipeline proven end to end.
**Depends on**: Phase 1
**Requirements**: RES-01, RES-02, RES-03
**Success Criteria** (what must be TRUE):
  1. Clone Vats can be constructed on an inhabited colony and appears in the Industries tab.
  2. The colony screen shows organics and organs listed as commodity demands for Clone Vats.
  3. The industry has a credit upkeep cost visible in the colony budget panel.
  4. Selling or destroying the industry removes all its commodity demands from the colony screen.
**Plans**: TBD

Plans: (to be created by plan-phase)

---

### Phase 3: Full Mechanics
**Goal**: All growth, stability, and booster mechanics are live on tier 1 — growth formula, AI core branching, Biofactory Embryo, story point upgrade, and stability penalty all work correctly and cleanly remove on unapply.
**Depends on**: Phase 2
**Requirements**: GROW-01, GROW-02, GROW-03, BOOST-01, BOOST-02, BOOST-03, BOOST-04, BOOST-05, BOOST-06, DOWN-01
**Success Criteria** (what must be TRUE):
  1. The colony growth rate tooltip shows a growth bonus from Clone Vats that increases as colony size increases.
  2. Installing an Alpha AI core increases growth output and reduces upkeep; Gamma reduces upkeep only; Beta falls between; removing a core reverts all changes.
  3. Installing a Biofactory Embryo in the industry item slot shows an additional growth bonus in the tooltip.
  4. Using the Story Point "Improve" button on the industry increases growth output; the effect persists across save/reload.
  5. The colony stability tooltip shows a negative modifier attributed to the cloning industry, scaling with colony size.
**Plans**: TBD

Plans: (to be created by plan-phase)

---

### Phase 4: Tiers 2-3 and Settings
**Goal**: The full three-tier upgrade chain works in-game with correct per-tier base production values, and LunaLib settings expose the scaling and production multipliers for player tuning.
**Depends on**: Phase 3
**Requirements**: LUNA-01, LUNA-02
**Success Criteria** (what must be TRUE):
  1. Upgrading Clone Vats to Cloning raises the base growth contribution; upgrading again to Clone Mass Manufactorium raises it further; downgrading reverses the change — all visible in the growth tooltip.
  2. The LunaLib settings panel contains a scaling multiplier and an overall production multiplier that change growth output immediately when adjusted.
  3. Growth numbers at colony sizes 3, 5, and 7 are demonstrably higher than the vanilla growth ceiling at the same sizes, confirming the mod delivers its core value.
**Plans**: TBD

Plans: (to be created by plan-phase)

---

## Progress

**Execution Order:** 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Data Skeleton | 0/TBD | Not started | - |
| 2. Working Tier 1 Stub | 0/TBD | Not started | - |
| 3. Full Mechanics | 0/TBD | Not started | - |
| 4. Tiers 2-3 and Settings | 0/TBD | Not started | - |
