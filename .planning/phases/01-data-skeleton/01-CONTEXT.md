# Phase 1: Data Skeleton - Context

**Gathered:** 2026-03-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Register all three cloning industry tiers (Clone Vats, Cloning, Clone Mass Manufactorium) via CSV data files so the mod loads cleanly, the upgrade chain is wired, and all three industry names and descriptions are visible in-game. No Java code — pure data.

</domain>

<decisions>
## Implementation Decisions

### Industry IDs
- `clone_vats` — tier 1
- `cloning` — tier 2
- `clone_mass_manufactorium` — tier 3
- IDs are permanent save-file keys; follow snake_case Starsector convention

### Industry Slot
- All three tiers occupy a **regular industry slot** (not heavy industry)
- No colony size requirement on any tier — upgrades are resource-gated only

### Sprites
- Sprites already provided: `cloning1`, `cloning2`, `cloning3`
- Reference these filenames in the CSV sprite fields

### Industry Descriptions
- **Tone:** Sci-fi flavor — evocative, hints at the strangeness of the technology
- **Structure:** One sentence of flavor + one sentence of mechanical function per tier
- **Tier differentiation by scale of operation:**
  - Clone Vats = small/experimental installation
  - Cloning = established facility with serious throughput
  - Clone Mass Manufactorium = full industrial-scale operation
- No specific wording requirements — open to what fits the tone

### Claude's Discretion
- Exact description wording (within the tone/structure/scale decisions above)
- CSV fields not discussed (e.g., upkeep cost values — that's Phase 2)
- Exact sprite path format (directory prefix, file extension)

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches within the decided tone and structure.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-data-skeleton*
*Context gathered: 2026-03-29*
