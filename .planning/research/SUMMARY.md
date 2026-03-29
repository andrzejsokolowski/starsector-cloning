# Research Summary: Starsector Cloning Industry Mod

_Synthesized 2026-03-29 from STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md_

---

## Executive Summary

This is a Starsector 0.98a-RC8 industry mod that adds a three-tier population growth industry
(Clone Vats → Cloning → Clone Mass Manufactorium). The mod is implemented in Java 7 bytecode
targeting the `BaseIndustry` API class, compiled against the game's bundled `starfarer.api.jar`,
and registered entirely through `industries.csv` and `mod_info.json`. No external libraries or
build frameworks are needed or advisable.

The recommended approach is a shallow inheritance chain (`BaseCloning` abstract → `CloneVats` →
`Cloning` → `CloneMassManufactorium`) with three separate industry entries in `industries.csv`
wired via `upgrade`/`downgrade` columns. All tier-specific stat scaling, AI core detection,
Biofactory Embryo detection, story point upgrade, and stability penalty live inside `apply()` /
`unapply()` on `BaseCloning`, inherited for free by the three concrete tiers. This is the same
structural pattern used by vanilla's upgrade chains (e.g. Waystation → High Command).

The key risks are (a) using the wrong stat key for population growth — which silently does
nothing, (b) stat modifier accumulation bugs caused by non-unique source strings or incomplete
`unapply()` logic, and (c) balance-breaking growth at large colony sizes if no cap is applied.
All three are preventable with upfront discipline in `apply()` / `unapply()` design and early
in-game testing at colony sizes 3, 5, and 7.

---

## Stack

| Decision | Detail |
|----------|--------|
| **Game version** | Starsector 0.98a-RC8. `mod_info.json` `gameVersion` must match exactly including the `-RC8` suffix. |
| **Java target** | Java 7 bytecode (`-source 7 -target 7`). The embedded game JRE is JDK 8 but the API is compiled to Java 7. Any Java 8 language feature (lambdas, streams, default interface methods) causes a runtime crash or silent failure. |
| **Compile classpath** | `starfarer.api.jar` from the local game installation as `compileOnly`. Never bundle it in the output jar. |
| **Build tooling** | IntelliJ IDEA Community with an Artifact configuration targeting `jars/cloning.jar` is the community standard. Gradle is acceptable for local use but adds friction for contributors. No Maven. |
| **Base class** | Extend `com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry`. Do not implement `Industry` directly. |
| **Growth stat key** | `Stats.COLONY_GROWTH_MOD` (or the method chain `market.getStats().getDynamic().getStat(Stats.COLONY_GROWTH_MOD)`). Using any other stat key silently does nothing. |
| **Optional dependency** | LunaLib — keep only if a runtime settings panel is desired. Remove `LunaSettingsConfig.json` and the `requiredMods` entry otherwise. |

---

## Architecture

**Class hierarchy**

```
BaseIndustry  (Starsector API)
  └─ BaseCloning  (abstract — all shared logic)
       └─ CloneVats          (tier 1, concrete)
            └─ Cloning       (tier 2, concrete)
                 └─ CloneMassManufactorium  (tier 3, concrete)
```

**Why inheritance, not a single class with a tier flag:** Starsector instantiates a fresh class
per industry ID. The upgrade system is driven by the `plugin` column in `industries.csv`. Putting
all three tiers in one class requires reading tier state from persistent data, which is fragile.
Inheritance makes each tier self-contained and mirrors the vanilla pattern.

**Key method responsibilities**

| Method | Responsibility |
|--------|---------------|
| `apply()` | Applies growth flat + mult modifier on growth stat; applies stability penalty; declares commodity demand; branches on AI core and Biofactory Embryo; checks `isImproved()` for SP upgrade. |
| `unapply()` | Removes every modifier applied in `apply()` using identical source strings. Must call `unmodify()` for base, core, embryo, and SP bonus source strings unconditionally. |
| `addAllowedSpecialItems()` | Adds `"biofactory_embryo"` to enable the special item slot on the industry card. |
| `getStoryPointUpgradeEffect()` | Returns a `BaseStoryPointUpgradeEffect` with upgrade/unupgraded descriptions. |

**Data layer**

- `industries.csv` needs three rows (`oddisz_cloning_t1`, `oddisz_cloning_t2`, `oddisz_cloning_t3`)
  with `upgrade`/`downgrade` chaining before any Java is written.
- `mod_info.json` wires `modPlugin` to `oddisz.plugin.CloningPlugin` (extends `BaseModPlugin`).
- All three tiers can share the same graphics; tier-specific art is optional.

**Recommended build order**

1. Expand `industries.csv` to three rows and verify the mod loads (no crash) without Java.
2. Write `BaseCloning` + `CloneVats` stub; verify tier 1 is buildable in-game.
3. Implement all mechanics in `BaseCloning` (growth, stability, commodities, AI core, embryo, SP upgrade).
4. Add `Cloning` (T2) and `CloneMassManufactorium` (T3); verify full upgrade chain.
5. Tune balance at sizes 3, 5, 7 against vanilla growth ceiling; apply cap.

---

## Table Stakes Features

These behaviors are expected by every Starsector player. Absent any one of them, the mod will
receive bug reports or feel broken.

| Behavior | Notes |
|----------|-------|
| Growth bonus visible in Colony screen tooltip | Requires a non-null description string in `modifyFlat`/`modifyMult` calls. |
| Bonus fully removed when industry is sold or disrupted | `unapply()` must call `unmodify()` for every modified stat, including core and embryo modifiers. |
| Upgrade button present on tier 1 and tier 2 | Requires `upgrade` column set in `industries.csv`. |
| Story point "Improve" button functional | Use `isImproved()` / `getImprovementsDesc()`; never cache improvement state in instance fields. |
| AI core slot visible and responding to all three core tiers | Null-check `getAICoreId()` before branching; use distinct source strings per core tier. |
| Biofactory Embryo slot visible and functional | Override `addAllowedSpecialItems()`; use `Items.BIOFACTORY_EMBRYO` constant, not a raw string. |
| Commodity demands (organics) appear in Colony screen | Declare via `demand(Commodities.ORGANICS, level)` inside `apply()`. |
| Stability penalty shown in stability tooltip | Pass negative value: `modifyFlat(source, -penalty, "Cloning")`. |
| Industry not placeable on uninhabited planets | Vanilla `BaseIndustry` handles this by default; verify `isAvailableToBuild()`. |
| Disruption followed by re-apply restores correct stats | Guaranteed if `unapply()` / `apply()` are symmetric. |
| No stat accumulation across reload | Source strings must be stable constants, never dynamically generated. |

---

## Critical Pitfalls

**Pitfall 1 — Wrong growth stat key (silent failure)**
Using any stat other than `Stats.COLONY_GROWTH_MOD` results in a mod that loads cleanly,
applies no growth bonus, and logs nothing. Verify the stat key against the vanilla `Aquaculture`
or `Farming` decompiled source before writing `apply()`.

**Pitfall 2 — Stat modifier accumulation (save corruption)**
`apply()` is called on every colony re-evaluation. A non-unique source string causes the bonus to
stack on itself each call, producing exponential growth rates that corrupt saves. Always use a
fixed constant like `"oddisz_cloning_base"` and always call the matching `unmodify()` in
`unapply()`. Use separate source string suffixes for base, core, embryo, and SP bonuses so they
can each be removed independently.

**Pitfall 3 — AI core null pointer crash**
`getAICoreId()` returns `null` when no core is installed. Without a null check, `apply()` throws
a `NullPointerException` on every colony evaluation tick, which freezes colony stats and floods
`starsector.log`. Always guard: `if (getAICoreId() != null)` before any `.equals()` check on the
core ID.

**Pitfall 4 — Balance-breaking growth at large colony sizes**
The game's colony size scale is logarithmic (size 6 ≈ 1,000,000 people). A flat bonus that
makes sense at size 3 is enormous at size 7. Test at sizes 3, 5, and 7. Apply a hard cap with
`Math.min(computedBonus, MAX_CAP)` and use a softening curve (e.g., log-scale multiplier)
rather than raw power-of-10 scaling. The industry should feel impactful, not a win condition.

**Pitfall 5 — Industries.csv structure errors**
Two sub-pitfalls with identical consequences (all vanilla industries disappear from build menus):
(a) Column count mismatch between header and data rows — always edit in a spreadsheet tool, not a
text editor. (b) `upgrade`/`downgrade` columns not set — the upgrade button will never appear and
the tier system is invisible to players.

**Pitfall 6 — "Organs" commodity does not exist in vanilla**
Vanilla Starsector has `"organics"` but not `"organs"`. Demanding a non-existent commodity ID is
silently ignored or causes a data load error. Confirm against
`starsector-core/data/campaign/commodities.csv`. Either use `Commodities.ORGANICS` only, or
define a custom `"organs"` commodity with a full entry and a supply source.

**Pitfall 7 — Stability penalty applied with wrong sign**
`modifyFlat` with a positive value increases stability. Pass the negative explicitly:
`modifyFlat(source, -PENALTY_VALUE, "Cloning")`. Check the stability tooltip on first build to
confirm the sign is correct.

---

## Surprises / Non-Obvious Findings

**No vanilla industry directly boosts population growth.**
Every vanilla growth modifier comes from conditions, planet types, or events — never a buildable
industry. There is no direct reference implementation in the base game's industry code to copy.
The closest analogues for the API patterns are `Aquaculture` (Biofactory Embryo, organics input)
and `Fuel Production` (AI core branching), not any growth-specific industry.

**The upgrade/downgrade chain is purely data-driven.**
The in-game upgrade button appears solely because of the `upgrade` column in `industries.csv`.
There is no Java method to call to "enable" upgrades. If the CSV column is missing or misspelled,
the upgrade button never shows regardless of what the Java class does.

**Story point upgrade state is already persisted by `BaseIndustry`.**
Calling `isImproved()` is the only correct way to check SP upgrade state. Any instance field
used to cache this will be lost on every save/reload. `BaseIndustry` handles all persistence
automatically.

**`modifyPercent` and `modifyMult` are semantically different.**
`modifyPercent` adds additive percentage points to the stat's flat sum. `modifyMult` multiplies
the result. For population growth, the vanilla pattern uses `modifyMult` so the multiplier applies
to the total (base + all flats). Using `modifyPercent` where `modifyMult` is intended produces
growth numbers that diverge from design at large colony sizes.

**Mod-prefix discipline on source strings is mandatory for mod compatibility.**
If two mods apply a modifier with the same source string (e.g., both use `"growth_bonus"`), each
`unmodify()` call removes the other mod's modifier as well. Always prefix: `"oddisz_cloning_base"`,
`"oddisz_cloning_core"`, etc.

**The `gameVersion` field requires the full string including RC suffix.**
`"0.98a"` and `"0.98a-RC8"` are treated as different versions by the mod loader. Using the
truncated form triggers a compatibility warning for all players even on the correct version.

---

## Confidence Assessment

| Area | Confidence | Basis |
|------|-----------|-------|
| Java version constraint (Java 7 target) | High | Official documentation, universally reproduced in all active mods |
| `BaseIndustry` as correct base class | High | All vanilla industries use it; documented in modding wiki |
| Upgrade/downgrade CSV mechanism | High | Directly observable in vanilla spaceport → megaport chain |
| `Stats.COLONY_GROWTH_MOD` stat key | High (STACK.md) / Medium (FEATURES.md) | FEATURES.md notes the key changed between 0.95 and 0.97+; verify against decompiled vanilla before use |
| AI core integration patterns | High | Vanilla reference implementations exist (Fuel Production, Orbital Station) |
| Growth balance numbers | Low | No vanilla industry to calibrate against; requires in-game playtesting |
| `"organs"` commodity existence in vanilla | Low | PITFALLS.md flags it as likely absent; must verify in `commodities.csv` |
| LunaLib settings panel integration | Medium | LunaLib API changes between releases; verify against target version |

**Primary gap to resolve before implementation:** Confirm the exact stat key string for the
colony growth rate modifier against the decompiled 0.98a source. STACK.md and FEATURES.md give
slightly different method chains (`Stats.COLONY_GROWTH_MOD` vs `Stats.COLONY_GROWTH_RATE_MODIFIER`).
This must be verified before `apply()` is written.

---

## Roadmap Implications

### Suggested Phase Structure

**Phase 1 — Data skeleton (no Java)**
Expand `industries.csv` to three rows with correct IDs, names, upgrade/downgrade chain, tags,
and placeholder `plugin` class names. Set `gameVersion` correctly in `mod_info.json`. Verify the
mod loads without crash. Completing this first eliminates an entire class of ID-drift bugs before
any Java is written.

**Phase 2 — Minimal working tier 1**
Set up the Java build (IntelliJ artifact or Gradle). Write `BaseCloning` and `CloneVats` as a
stub that compiles and loads. Verify the industry appears in the build menu and can be constructed.
No mechanics yet. This proves the build pipeline before implementing logic.

**Phase 3 — Core mechanics on tier 1**
Implement the full `apply()` / `unapply()` logic in `BaseCloning`: growth stat modification
(confirm the correct stat key first), stability penalty, commodity demand, AI core branching,
Biofactory Embryo detection, story point upgrade. Test each mechanic in isolation. This phase
should end with a fully functional tier 1 industry.

**Phase 4 — Tiers 2 and 3**
Write `Cloning` (T2) and `CloneMassManufactorium` (T3) extending `CloneVats`. Override tier
constants. Verify the full upgrade/downgrade chain in-game. Confirm stats change correctly at
each transition and after save/reload.

**Phase 5 — Balance and polish**
Tune growth numbers against vanilla growth ceiling. Apply a hard cap. Add per-tier description
text. Optionally expose tuning values via LunaLib settings panel. Prepare release package.

### Research Flags

- Phase 3 requires verifying the growth stat key against decompiled 0.98a source before writing
  any modifier code. This is the single highest-risk ambiguity in the research.
- Phase 5 has no well-documented balance baseline for growth industries; playtesting at colony
  sizes 3, 5, and 7 is the only validation path.
- Phases 1 and 2 follow well-documented vanilla patterns and need no additional research.
