# Starsector Cloning Mod

## What This Is

A Starsector mod that adds a single new industry: **Cloning**. The industry provides colonies with population growth far exceeding any natural means, scaling with colony size like all vanilla industries. It is upgradeable through three named tiers and supports the full suite of vanilla boosters (story point upgrade, AI cores, Biofactory Embryo item).

## Core Value

The cloning industry must deliver dramatically faster population growth than anything in vanilla — that's the entire point of building it.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Industry exists with 3 upgrade tiers: Clone Vats → Cloning → Clone Mass Manufactorium
- [ ] Population growth bonus is flat + multiplier, scales with colony size (10^Size)
- [ ] Story point upgrade increases growth output
- [ ] AI cores (gamma/beta/alpha) slot into the industry and improve output + modify resource consumption
- [ ] Biofactory Embryo (vanilla item) functions as the colony item and boosts output
- [ ] Industry consumes organics and organs as inputs, plus credit upkeep
- [ ] Industry applies a stability penalty to the colony

### Out of Scope

- Omega AI core support — not implemented anywhere in vanilla, skip for now
- Custom colony items — using the vanilla Biofactory Embryo, no new item needed

## Context

- Starsector uses Java modding via the `MagicLib`/vanilla modding API
- Industries are defined via a combination of JSON (mod_info.json, industry data CSVs) and Java classes extending `Industry` or `BaseIndustry`
- Population growth in vanilla is handled by `ColonyGrowthFactor` conditions; custom industries hook in via `modifyIncoming` on the `MutableStat` for growth
- Biofactory Embryo is a vanilla special item (`biofactory_embryo`) that already exists in game data
- AI core integration is standard — vanilla provides `AICorePlugin` hooks, or industries can check `getAICoreId()` in `apply()`
- Colony size in Starsector uses a 0-10 scale; production quantities scale as `10^size` in vanilla formulas

## Constraints

- **Tech stack**: Java (Starsector modding API), CSV/JSON for data files — no alternate choices
- **Compatibility**: Must work with the current Starsector version without requiring other mods
- **Scope**: Single industry, no new factions, ships, weapons, or items beyond what's listed

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 3 upgrade tiers (not just 1) | User specified; gives progression feel | — Pending |
| Flat + multiplier growth | Best of both: baseline regardless of size, scales up for big colonies | — Pending |
| Stability penalty | Balances the powerful growth; colonists uneasy about cloning | — Pending |
| Use vanilla Biofactory Embryo | Already exists, no need to add a new item | — Pending |

---
*Last updated: 2026-03-29 after initialization*
