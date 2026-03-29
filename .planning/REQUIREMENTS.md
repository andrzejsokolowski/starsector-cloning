# Requirements: Starsector Cloning Mod

**Defined:** 2026-03-29
**Core Value:** The cloning industry must deliver dramatically faster population growth than anything in vanilla — that's the entire point of building it.

## v1 Requirements

### Industry Structure

- [ ] **IND-01**: Clone Vats industry exists as a buildable colony industry
- [ ] **IND-02**: Clone Vats can be upgraded to Cloning (tier 2)
- [ ] **IND-03**: Cloning can be upgraded to Clone Mass Manufactorium (tier 3)
- [ ] **IND-04**: Each tier has a distinct name, description, and icon

### Population Growth

- [ ] **GROW-01**: Growth formula: `baseProduction × (1 + (Colony_Size × log10(Colony_Size) × LunaScaling)) × LunaAllProdMult`
- [ ] **GROW-02**: Base production per tier: 100 (Clone Vats), 250 (Cloning), 500 (Clone Mass Manufactorium)
- [ ] **GROW-03**: No artificial cap — intentionally game-breaking at high colony sizes by design

### Resources

- [ ] **RES-01**: Industry consumes organics as an input commodity
- [ ] **RES-02**: Industry consumes organs (vanilla commodity ID: `organs`, "Harvested Organs") as an input commodity
- [ ] **RES-03**: Industry has a credit upkeep cost

### LunaLib Settings

- [ ] **LUNA-01**: LunaLib setting exposes a scaling multiplier for the size-based growth curve (`LunaScaling` in formula)
- [ ] **LUNA-02**: LunaLib setting exposes an overall production multiplier applied to final output (`LunaAllProdMult` in formula)

### AI Core & Boosters

- [ ] **BOOST-01**: Story point upgrade increases growth output
- [ ] **BOOST-02**: Gamma AI core reduces credit upkeep only (standard vanilla behavior)
- [ ] **BOOST-03**: Beta AI core reduces upkeep more + increases production slightly + reduces commodity demand by 1
- [ ] **BOOST-04**: Alpha AI core reduces upkeep significantly + increases production more + reduces commodity demand by 1
- [ ] **BOOST-05**: Omega AI core (external mod dependency) grants extreme bonuses; requires runtime mod-existence check before applying — gracefully no-ops if mod absent
- [ ] **BOOST-06**: Biofactory Embryo (vanilla special item) boosts growth output when installed in the industry item slot

### Downsides

- [ ] **DOWN-01**: Stability penalty scales with both upgrade tier and colony size — a large colony at tier 3 should be very difficult to keep stable

## v2 Requirements

### Polish

- **POL-01**: Custom industry tooltip with formula breakdown visible to player
- **POL-02**: Custom sprite/icon for each tier (beyond placeholder)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Custom colony item | Using vanilla Biofactory Embryo — no new item needed |
| Omega AI core implementation | External mod dependency; only consumed if mod is present |
| Population growth cap | Intentionally removed — game-breaking growth is the feature |
| New factions, ships, or weapons | Out of scope for this mod entirely |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| IND-01 | Phase 1 | Pending |
| IND-02 | Phase 1 | Pending |
| IND-03 | Phase 1 | Pending |
| IND-04 | Phase 1 | Pending |
| RES-01 | Phase 2 | Pending |
| RES-02 | Phase 2 | Pending |
| RES-03 | Phase 2 | Pending |
| GROW-01 | Phase 3 | Pending |
| GROW-02 | Phase 3 | Pending |
| GROW-03 | Phase 3 | Pending |
| BOOST-01 | Phase 3 | Pending |
| BOOST-02 | Phase 3 | Pending |
| BOOST-03 | Phase 3 | Pending |
| BOOST-04 | Phase 3 | Pending |
| BOOST-05 | Phase 3 | Pending |
| BOOST-06 | Phase 3 | Pending |
| DOWN-01 | Phase 3 | Pending |
| LUNA-01 | Phase 4 | Pending |
| LUNA-02 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 19 total
- Mapped to phases: 19
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-29*
*Last updated: 2026-03-29 — traceability updated after roadmap creation (LUNA-01/02 moved to Phase 4)*
