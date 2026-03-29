# Architecture: Starsector Cloning Industry Mod

## Component Overview

```
mod_info.json  ─────────────────────────────────────────────────────────────┐
  (declares jar, modPlugin, id)                                              │
                                                                             │
data/campaign/industries.csv  ──────────────────────────────────────────────┤
  (one row per tier; "plugin" column wires row → Java class)                 │
                                                                             ▼
                        Java Layer (cloning.jar)
  ┌────────────────────────────────────────────────────────────────────────┐
  │  CloneVats.java          (tier 1)  extends BaseIndustry                │
  │  Cloning.java            (tier 2)  extends CloneVats                   │
  │  CloneMassManufactorium  (tier 3)  extends Cloning                     │
  │                                                                         │
  │  Each class overrides:                                                  │
  │    apply()       ─── run every cycle: growth bonus, stability, cores   │
  │    unapply()     ─── undo all stat modifications                        │
  │    getUpgradeData() / getDowngradeData()  ─── tier navigation          │
  │    getStoryPointUpgradeEffect()  ─── story-point branch                │
  │    addAllovedItems() / applyItemEffects()  ─── Biofactory Embryo       │
  │    isAvailableToBuild() / isFunctional()                                │
  └────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ reads at runtime
                                    ▼
               Starsector Colony object (ColonyAPI)
                  getSize()  ──► growth formula
                  getStats() ──► MutableStat for GROWTH_RATE
                  getAICoreId() ──► core tier
                  getSpecialItem() ──► Biofactory Embryo check
```

---

## Java Classes

### Package: `oddisz.industries`

#### `BaseCloning` (abstract base, optional but recommended)

Responsibility: All shared logic for all three tiers.

- Stability penalty calculation and application via `colony.getStats().getDynamic().getMod(Stats.COLONY_STABILITY_MOD)`
- Organics + organs demand declaration (`demand(Commodities.ORGANICS, n)` / `demand("organs", n)`)
- Credit upkeep override (`getUpkeep()`)
- AI core detection (`getAICoreId()`) and per-tier multiplier lookup
- Biofactory Embryo detection (`getSpecialItem()`) and bonus application
- Growth stat modification via `colony.getStats().getGrowthFactor()` or directly on the `MutableStat` returned by the colony's growth-rate modifier list
- Helper method `computeGrowthBonus(int colonySize, float flatBase, float multBase)` — encapsulates the scaling formula

#### `CloneVats` extends `BaseCloning`

Tier 1.

- Constants: `FLAT_BONUS_T1`, `MULT_BONUS_T1`, `STABILITY_PENALTY_T1`
- Commodity demand levels at tier-1 quantities
- `getUpgradeData()` returns id `oddisz_cloning_t2`
- `getSpec().getId()` returns `oddisz_cloning_t1` (must match industries.csv row)

#### `Cloning` extends `CloneVats`

Tier 2. Overrides constants upward. `getUpgradeData()` → `oddisz_cloning_t3`. `getDowngradeData()` → `oddisz_cloning_t1`.

#### `CloneMassManufactorium` extends `Cloning`

Tier 3. Overrides constants to highest values. `getUpgradeData()` returns `null` (no further upgrade). `getDowngradeData()` → `oddisz_cloning_t2`.

#### `CloningPlugin` (implements `ModPlugin`)

Registered in `mod_info.json` as `"modPlugin": "cloning.plugin"`.

- `onApplicationLoad()` — register any global hooks if needed (usually none for a pure industry)
- No forced logic here for a single-industry mod; can remain nearly empty

---

### Class hierarchy summary

```
BaseIndustry  (Starsector API)
  └─ BaseCloning  (abstract — shared logic)
       └─ CloneVats          (tier 1 — concrete)
            └─ Cloning       (tier 2 — concrete)
                 └─ CloneMassManufactorium  (tier 3 — concrete)
```

**Why inheritance over a single class with switch logic:** Starsector's upgrade system works by ID — the game instantiates a *new* class when a player upgrades. Each tier is a distinct entry in industries.csv with its own `plugin` class. A single-class + tier-field approach requires the instance to know its own tier from persistent data, which is more fragile. The inheritance approach is the vanilla pattern (e.g., `Farming` → `Aquaculture`).

---

## Data Files

### `data/campaign/industries.csv`

One row per tier. Current state has a single row (`oddisz_cloning`). Needs expanding to three rows.

| Column | Purpose | Example values |
|--------|---------|----------------|
| `id` | Unique industry ID, referenced in Java and upgrade chains | `oddisz_cloning_t1`, `oddisz_cloning_t2`, `oddisz_cloning_t3` |
| `name` | Display name shown in colony UI | `Clone Vats`, `Cloning`, `Clone Mass Manufactorium` |
| `cost mult` | Build cost multiplier (relative to base) | `75`, `100`, `150` |
| `build time` | Days to build | `30`, `45`, `60` |
| `upkeep` | Monthly credit upkeep (base, before AI core modifier) | `6`, `10`, `16` |
| `upgrade` | ID of the next-tier industry row | `oddisz_cloning_t2`, `oddisz_cloning_t3`, *(empty)* |
| `downgrade` | ID of the previous-tier industry row | *(empty)*, `oddisz_cloning_t1`, `oddisz_cloning_t2` |
| `tags` | Comma-separated tags | `unraidable, structure` |
| `plugin` | Fully-qualified Java class name instantiated for this industry | `oddisz.industries.CloneVats` |
| `image` | Path to industry panel art | `graphics/industry/cloning.png` |
| `desc` | Tooltip description | *(flavor text per tier)* |
| `order` | Sort order in colony UI list | `3460`, `3461`, `3462` |

Note: The `data` column can carry an arbitrary string readable in Java via `getSpec().getData()` — useful for storing tier-specific numeric tuning without hardcoding in Java. Not required but a useful extension point.

### `mod_info.json`

Already present. The `"jars"` array and `"modPlugin"` field are the two wiring points between this file and the Java layer. No changes needed once the jar build is set up.

### `data/config/LunaSettingsConfig.json`

Already present with a stub. If Luna Lib (runtime settings) is used, settings like growth bonus magnitude or stability penalty could be exposed here. For the initial implementation this is optional — hardcode values in Java and revisit.

### `cloning.version` / `data/config/version/version_files.csv`

Version checker integration (ModVer / Version Checker). Already scaffolded. No architecture impact.

### Graphics

- `graphics/industry/cloning.png` — industry panel image (shown in colony industry list). Already present.
- `graphics/icons/cloning.png` — icon used in various UI contexts. Already present.
- All three tiers share the same images unless you add tier-specific art.

---

## Data Flow

### How colony size feeds into growth bonus

```
Game tick
  │
  ▼
BaseCloning.apply(colony)
  │
  ├─ int size = colony.getSize()           // 1–10 integer
  │
  ├─ float scaleFactor = (float) Math.pow(10, size)
  │   // Mirrors vanilla commodity scaling: size 3 → 1000, size 5 → 100000
  │   // For growth, a softer curve (e.g. size * multiplier) may be preferable
  │   // to avoid absurd numbers at size 10. Choose curve in design phase.
  │
  ├─ float flat  = FLAT_BASE  * tierMult * coreMult * embryoMult
  ├─ float mult  = MULT_BASE  * tierMult * coreMult * embryoMult
  │
  └─ colony.getStats()
         .getGrowthFactor()           // returns MutableStat
         .addModFlat(id, "Cloning",  flat)
         .addModMult(id, "Cloning",  mult)
         // 'id' is a unique string key, e.g. "oddisz_cloning"
         // The colony aggregates all such mods; Starsector applies them
         // in the standard order: sum-of-flats first, then product-of-mults
```

### AI core modifier lookup

```
String coreId = getAICoreId();   // null, "gamma_core", "beta_core", "alpha_core"

float coreMult = switch(coreId) {
    case "gamma_core"  -> 1.1f;
    case "beta_core"   -> 1.25f;
    case "alpha_core"  -> 1.5f;
    default            -> 1.0f;
};
```

AI cores also typically reduce upkeep or commodity demand. Override `getUpkeep()` and adjust demand levels inside `apply()` based on `getAICoreId()`.

### Biofactory Embryo check

```
SpecialItemData item = getSpecialItem();
float embryoMult = (item != null && "biofactory_embryo".equals(item.getId()))
    ? 1.25f   // tune this
    : 1.0f;
```

`getSpecialItem()` is a `BaseIndustry` method returning the installed colony special item, or `null`. To allow the Biofactory Embryo to be installed in the first place, override `addAllowedSpecialItems()` and add `"biofactory_embryo"`.

### Stability penalty application

```
colony.getStats()
      .getDynamic()
      .getMod(Stats.COLONY_STABILITY_MOD)
      .modifyFlat(id, -STABILITY_PENALTY, "Cloning (stability)");
```

`Stats.COLONY_STABILITY_MOD` is the canonical key. The penalty should scale slightly with tier (T1: -1, T2: -2, T3: -3) to discourage rushing to tier 3 without infrastructure.

### Story point upgrade

Override `getStoryPointUpgradeEffect()` to return a `StoryPointUpgradeEffectAPI` (or the vanilla `BaseStoryPointUpgradeEffect`). This grants a permanent bonus (e.g., +25% growth mult) when the player spends a story point. The bonus is stored on the industry's persistent data map.

```java
@Override
public StoryPointUpgradeEffectAPI getStoryPointUpgradeEffect() {
    return new BaseStoryPointUpgradeEffect() {
        public String getUnupgradedDesc() { return "Spend a story point to optimize cloning protocols."; }
        public String getUpgradedDesc()   { return "Cloning protocols optimized: +25% growth output."; }
        // apply bonus in the parent apply() by checking isUpgraded()
    };
}
```

Inside `apply()`, check `isUpgraded()` and apply an additional mult modifier.

### Resource (commodity) demand

Starsector industries declare commodity demand inside `apply()` using:

```java
demand(Commodities.ORGANICS, ORGANICS_DEMAND_TIER);   // integer 1–10 maps to quantity
demand("organs",             ORGANS_DEMAND_TIER);
```

The integer demand level corresponds to a logarithmic quantity: level 3 ≈ 1000 units/month at size 3. Demand levels should match tier (T1 lower, T3 higher) and can be reduced one level by an alpha core. Organs is a vanilla commodity id available in `Commodities` or as the string literal `"organs"`.

---

## Build Order

### Phase 1 — Data skeleton (no Java yet)

1. **Expand `industries.csv`** to three rows (`oddisz_cloning_t1`, `oddisz_cloning_t2`, `oddisz_cloning_t3`) with correct upgrade/downgrade chains and placeholder plugin class names.
2. **Verify mod loads** in game without Java (the industry won't be buildable without the class, but the mod should not crash on load).

*Why first:* The CSV is the registration point. If IDs drift between CSV and Java, nothing works. Getting the data layer right before writing any Java eliminates a whole class of "industry not appearing" bugs.

### Phase 2 — Minimal working industry (tier 1 only)

3. **Set up Java build** (IDE project or Ant/Gradle script targeting `starfarer.api.jar` and outputting `jars/cloning.jar`).
4. **Write `BaseCloning`** with `apply()` / `unapply()` stub — no bonuses yet, just compiles and loads.
5. **Write `CloneVats`** extending `BaseCloning`; register it as plugin for `oddisz_cloning_t1`.
6. **Verify in game**: build Clone Vats on a colony, confirm no crash, industry appears.

*Why before tier 2/3:* Each tier is mechanically identical except constants. If tier 1 works, tiers 2/3 are copy-paste + override. Debugging is much simpler with one tier.

### Phase 3 — Core mechanics (single tier, full features)

7. **Growth bonus**: implement `computeGrowthBonus()` and `addModFlat`/`addModMult` on the growth stat. Tune the growth curve in-game.
8. **Stability penalty**: add `modifyFlat` on `COLONY_STABILITY_MOD`.
9. **Commodity demand**: declare organics + organs demand.
10. **Upkeep**: override `getUpkeep()` if different from CSV baseline.
11. **AI core support**: detect `getAICoreId()` and apply multipliers.
12. **Biofactory Embryo**: override `addAllowedSpecialItems()` and apply embryo mult.
13. **Story point upgrade**: implement `getStoryPointUpgradeEffect()`.

*Why together:* These all live inside `apply()` / `unapply()` in `BaseCloning`. They are independent of tier. Locking them down here means tier 2/3 inherit them for free.

### Phase 4 — Tiers 2 and 3

14. **Write `Cloning` (T2)** extending `CloneVats` — override constants, point upgrade/downgrade IDs.
15. **Write `CloneMassManufactorium` (T3)** extending `Cloning` — same pattern.
16. **Verify upgrade chain in game**: build T1, upgrade to T2, upgrade to T3, downgrade, confirm stats change correctly at each step.

### Phase 5 — Polish and balance

17. **Tune growth numbers** against vanilla (`TerranCondition`, `PopulationAndInfrastructure`, organic markets) — make sure Cloning is powerful but not instant-win.
18. **Tune stability penalty** — verify it stacks correctly with other sources.
19. **Description text** per tier in industries.csv.
20. **LunaLib settings** (optional): expose tuning constants as in-game settings if desired.

---

## Dependencies Between Components

```
mod_info.json
  └─ depends on: cloning.jar (must be built before the mod is playable)
  └─ depends on: modPlugin class path matching Java package

industries.csv
  └─ depends on: industry IDs being stable (changing IDs breaks existing saves)
  └─ "plugin" column depends on: Java class existing at that fully-qualified name

BaseCloning.apply()
  └─ depends on: Starsector API (starfarer.api.jar) — ColonyAPI, MutableStat, Stats
  └─ depends on: Commodities constants — use string literals as fallback for "organs"
  └─ depends on: industries.csv IDs matching getSpec().getId() return values

Upgrade chain
  └─ depends on: industries.csv "upgrade"/"downgrade" columns being consistent
  └─ depends on: Java getUpgradeData()/getDowngradeData() returning matching IDs

Story point upgrade
  └─ depends on: BaseIndustry.isUpgraded() state being persisted across saves
     (vanilla handles this automatically — no custom persistence needed)

Biofactory Embryo
  └─ depends on: vanilla item id "biofactory_embryo" existing in base game data
     (confirmed present in 0.98a — no mod dependency needed)
```

---

## Key API Reference Points (Starsector 0.98a)

| What you need | Where it lives in the API |
|---|---|
| Industry base class | `com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry` |
| Colony access | `IndustryAPI.getColony()` → `ColonyAPI` |
| Colony size | `ColonyAPI.getSize()` → `int` (1–10) |
| Growth rate stat | `ColonyAPI.getStats().getGrowthFactor()` → `MutableStat` |
| Stability mod | `ColonyAPI.getStats().getDynamic().getMod(Stats.COLONY_STABILITY_MOD)` |
| Demand declaration | `BaseIndustry.demand(String commodityId, int level)` |
| Installed AI core | `BaseIndustry.getAICoreId()` → `String` (null if none) |
| Installed special item | `BaseIndustry.getSpecialItem()` → `SpecialItemData` (null if none) |
| Allow special items | Override `BaseIndustry.addAllowedSpecialItems(List<String> items)` |
| Story point upgrade | Override `BaseIndustry.getStoryPointUpgradeEffect()` |
| Check if SP-upgraded | `BaseIndustry.isUpgraded()` → `boolean` |
| Commodity IDs | `com.fs.starfarer.api.impl.campaign.ids.Commodities` |
| Stat keys | `com.fs.starfarer.api.impl.campaign.ids.Stats` |
| Mod plugin hook | Implement `com.fs.starfarer.api.BaseModPlugin` |
