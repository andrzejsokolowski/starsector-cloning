# Features: Starsector Cloning Industry

## Population Growth Mechanics

### How Vanilla Handles It

Population growth in Starsector is stored as a `MutableStat` on the market. The canonical field is accessed via:

```java
market.getStats().getDynamic().getMod("colony_growth_rate")
```

However, the **correct primary stat** used in vanilla for the net population growth rate displayed in the UI and used by the game loop is:

```java
Stat growthStat = market.getStats().getColonyGrowthRateModifier();
```

This resolves to the `Stats.COLONY_GROWTH_RATE_MODIFIER` key. Modifiers are applied as:
- **Flat additions** via `stat.modifyFlat(sourceId, amount, description)`
- **Percent multipliers** via `stat.modifyPercent(sourceId, amount, description)`
- **Multiplicative modifiers** via `stat.modifyMult(sourceId, factor, description)`

The game applies all flat additions first, then all multiplicative/percent modifiers on top.

**Colony size** in Starsector runs on a scale of 1–10. The underlying population number scales approximately as `10^size` (i.e., size 3 = ~1,000 people, size 6 = ~1,000,000). Growth bonuses from industries scale with this power-of-10 progression, which is why a flat bonus at size 3 is very different from the same flat bonus at size 6.

**The growth loop**: Each in-game month, the game evaluates all `ColonyGrowthFactor` contributions across all markets and updates the population. The growth check is triggered during the `ColonyEventListener` / monthly update cycle. Time-to-next-size is shown in the Colony screen.

### Vanilla Growth Sources (for calibration)

| Source | Approximate bonus |
|--------|-------------------|
| Orbital station (population condition) | ~+10% growth |
| Population condition: "Rich" hazard rating | negative modifier |
| Aquatic world (ideal) | positive flat |
| Spaceport (indirect, access) | no direct growth |
| No vanilla industry directly boosts growth | — |

**Important**: No vanilla *industry* directly applies a `COLONY_GROWTH_RATE_MODIFIER`. Vanilla growth modifiers come from conditions, planet types, and events — not buildable industries. This means the Cloning industry is genuinely novel in vanilla terms and has no direct reference implementation in the base game's industry code.

### Key API Points

- `market.getStats()` returns `MutableCharacteristicsAPI`
- `Stats.COLONY_GROWTH_RATE_MODIFIER` is the string key (value: `"colony_growth_rate_modifier"`)
- In `BaseIndustry.apply()`, call `market.getStats().getDynamic().getMod(Stats.COLONY_GROWTH_RATE_MODIFIER).modifyFlat(...)` and `.modifyPercent(...)`
- Remove modifiers in `unapply()` using `.unmodify(sourceId)` on the same stat

**Uncertain**: The exact string key for the growth stat changed between Starsector 0.95 and 0.97+. Cross-check `com.fs.starfarer.api.campaign.econ.MutableCharacteristicsAPI` javadoc or decompiled vanilla source before hard-coding the key string. The field may be `Stats.COLONY_GROWTH_RATE_MODIFIER` or accessed via `market.getStats().getColonyGrowthRateModifier()`.

---

## Industry Tier System

### How Upgrade Tiers Work in Vanilla

Vanilla industries are single-level structures. The "tier" system (building → upgraded building → further upgraded) is **not natively supported** in vanilla `BaseIndustry` as separate buildings with a formal upgrade chain — however, there are two established implementation patterns:

**Pattern A: Single industry with internal tier tracking (recommended)**
The industry CSV registers *one* industry ID (`oddisz_cloning`). The Java class checks the story point upgrade state and the AI core installed to determine internal output tier. Tiers are not separate industries.

**Pattern B: Separate industry IDs with upgrade/downgrade columns in CSV**
The industries.csv has `upgrade` and `downgrade` columns (already present in the project CSV). Clicking the upgrade button in the UI triggers the market to replace one industry with another. Vanilla uses this for:
- `spaceport` → `megaport` (via the `upgrade` CSV field)
- Population-size-gated upgrades

The CSV columns `upgrade` and `downgrade` take an industry ID string. The vanilla UI will show an Upgrade button on the industry card if the `upgrade` field is populated and the colony meets requirements. Requirements are enforced by the target industry's `isAvailableToBuild()` override.

**For this mod (3 tiers: Clone Vats → Cloning → Clone Mass Manufactorium)**:
Use Pattern B — define three separate industry IDs in industries.csv with upgrade/downgrade chaining. Each tier is its own Java class (or a shared class parameterized by tier). This gives the native Starsector upgrade UI rather than custom UI work.

CSV structure (schematic):
```
oddisz_clone_vats,      ... upgrade=oddisz_cloning, downgrade=
oddisz_cloning,         ... upgrade=oddisz_clone_mass_manufactorium, downgrade=oddisz_clone_vats
oddisz_clone_mass_manufactorium, ... upgrade=, downgrade=oddisz_cloning
```

Each tier's Java class extends `BaseIndustry` and overrides `apply()` to provide tier-appropriate stat values.

---

## AI Core Integration

### Standard Vanilla Pattern

AI cores are installed into industries using the vanilla item slot mechanism. `BaseIndustry` already has full plumbing for this. The relevant methods:

```java
// Get the currently installed AI core item ID (null if none)
String coreId = getAICoreId();

// Check in apply() to branch by core tier
if (AICores.GAMMA_CORE_ID.equals(coreId)) { ... }
if (AICores.BETA_CORE_ID.equals(coreId)) { ... }
if (AICores.ALPHA_CORE_ID.equals(coreId)) { ... }
```

Constants from `com.fs.starfarer.api.impl.campaign.ids.Commodities` (or `AICores`):
- Gamma: `"gamma_core"`
- Beta: `"beta_core"`
- Alpha: `"alpha_core"`

**The `AICorePlugin` interface** (`com.fs.starfarer.api.campaign.econ.AICorePlugin`) is the *alternate* approach: an industry can register an `AICorePlugin` implementation that the game calls for description text and effect application. Vanilla uses `BaseAICorePlugin` as a base. However, most simple mods skip this and just branch in `apply()` — the plugin approach is only needed if you want custom tooltips in the core description screen.

**Typical apply() pattern**:
```java
@Override
public void apply() {
    super.apply();
    String coreId = getAICoreId();
    float growthFlat = BASE_GROWTH_FLAT;
    float growthPercent = BASE_GROWTH_PERCENT;

    if (AICores.BETA_CORE_ID.equals(coreId)) {
        growthFlat *= 1.25f;
        growthPercent += 25f;
    } else if (AICores.ALPHA_CORE_ID.equals(coreId)) {
        growthFlat *= 1.5f;
        growthPercent += 50f;
        // Alpha might also reduce organ consumption
    }
    // apply to stat...
}
```

**Upkeep reduction by AI core** is also conventional: vanilla industries reduce upkeep when an alpha core is installed. This is done via `supply.getUpkeep().modifyMult(...)`.

**Uncertain**: Whether `AICorePlugin` tooltips integrate cleanly when the industry also uses the upgrade chain pattern (Pattern B above) — test this in-game.

---

## Story Point Upgrade

### How It Works

The story point (SP) upgrade for an industry is a **one-time permanent enhancement** applied via a UI button in the colony industry card. Mechanically:

1. The player clicks "Improve" on the industry card, spending 1 story point.
2. The game calls `BaseIndustry.notifyBeingImproved()` then sets a flag on the industry.
3. The industry is flagged as "improved" accessible via `isImproved()` (returns `boolean`).
4. `apply()` is called again and should check `isImproved()` to add the SP bonus on top of the base stats.

```java
@Override
public void apply() {
    super.apply();
    float extraGrowth = 0f;
    if (isImproved()) {
        extraGrowth = IMPROVED_BONUS_FLAT;
        // e.g., add another flat growth modifier
    }
    // ...
}
```

The SP upgrade is **permanent** — it survives the industry being disrupted and re-applying. It does NOT stack (can only be improved once).

**Important**: `super.apply()` in `BaseIndustry` already handles the story point flag display and upkeep modification for the improved state. Do not duplicate that logic.

**Tooltip for SP upgrade**: Override `getImprovementsDesc()` to return the description shown to the player when they hover the "Improve" button:
```java
@Override
public String getImprovementsDesc() {
    return "Increases clone production efficiency, further boosting population growth.";
}
```

---

## Colony Item Integration

### Biofactory Embryo

The Biofactory Embryo (`"biofactory_embryo"`) is a vanilla **special item** (`SpecialItemData`) that already exists in the game. It is the colony item used by the vanilla `Aquaculture` industry (and the `Farming` industry IIRC). It is stored in the market's special items slot for the industry.

**How colony items hook into industries**:

An industry can declare it accepts a special item by overriding `getSpecialItem()` and handling item application in `apply()`. The canonical approach in `BaseIndustry`:

```java
// In apply(), check if the special item is installed
SpecialItemData item = getSpecialItem();
if (item != null && "biofactory_embryo".equals(item.getId())) {
    // Apply item bonus
    market.getStats().getDynamic().getMod(Stats.COLONY_GROWTH_RATE_MODIFIER)
          .modifyPercent(getModId() + "_embryo", EMBRYO_BONUS_PERCENT, "Biofactory Embryo");
}
```

The UI automatically shows the special item slot on the industry card when the industry's data is configured to accept one. The relevant CSV field and industry data config determines item slot availability — `BaseIndustry` handles the slot display and item drag-and-drop.

**Uncertain**: The exact CSV/data field to enable the item slot. In vanilla, the item slot appears to be enabled by the Java class overriding `canInstallSpecialItems()` returning `true`, or by the industry type being in the list of item-accepting industries. Verify via vanilla `Farming` or `Aquaculture` source as reference.

**Item stacking**: Only one special item can be installed per industry (vanilla hard limit). The Biofactory Embryo is already defined with its own bonuses for the vanilla Aquaculture industry — when installed in the Cloning industry instead, this mod's `apply()` code controls what bonus it grants (separate from Aquaculture's handling).

---

## Reference Industries

The following vanilla industries are the best reference implementations for the Cloning industry's patterns:

| Industry | Why It's Relevant | Key Pattern |
|----------|------------------|-------------|
| `Aquaculture` / `Farming` | Uses Biofactory Embryo as special item; organic commodity inputs | `getSpecialItem()`, item bonus application |
| `Waystation` / `Patrol HQ` → `High Command` | Multi-tier upgrade chain via CSV upgrade/downgrade fields | CSV `upgrade`/`downgrade` columns, separate industry IDs |
| `Ground Defenses` → `Planetary Shield` | Story point upgrade with meaningful stat change; `isImproved()` pattern | `isImproved()`, `getImprovementsDesc()` |
| `Fuelprod` (Fuel Production) | AI core integration with upkeep reduction and output bonus | `getAICoreId()` branching in `apply()` |
| `Mining` | Commodity demand/supply with hazard rating modifier; stability penalty | `supply.getUpkeep()`, `market.getStability().modifyFlat()` |

**Stability penalty reference**: The `Mining` industry and the `Pather Cells` condition both apply stability penalties. The pattern is:

```java
market.getStability().modifyFlat(getModId(), -STABILITY_PENALTY, "Cloning (unrest)");
```

Remove in `unapply()`:
```java
market.getStability().unmodify(getModId());
```

---

## Table Stakes

These are behaviors players expect from any well-made Starsector industry mod. Failing to implement these will generate bug reports or feel broken:

| Behavior | Implementation Note |
|----------|-------------------|
| Growth bonus appears in Colony screen tooltip | Must use `modifyFlat` / `modifyPercent` with a non-null description string |
| Bonus removed when industry is sold/disrupted | `unapply()` must call `.unmodify(sourceId)` on every modified stat |
| Upkeep cost shown in credits | Set `upkeep` in industries.csv (already: 6); show in tooltip via vanilla rendering |
| Build time and cost shown correctly | `cost mult` and `build time` in industries.csv (already set) |
| AI core slot visible and functional | Override `getAICoreId()` works out-of-box; just branch in `apply()` |
| Story point "Improve" button present | `isImproved()` works out-of-box; override `getImprovementsDesc()` |
| Special item slot visible | Override `canInstallSpecialItems()` or equivalent |
| Commodity demands (organics, organs) show in Colony screen | Use `demand(Commodities.ORGANICS, amount)` and equivalent in `apply()` |
| Disruption does not permanently break the market | `unapply()` removes all stat mods; they re-apply when `apply()` is called again |
| Stability penalty appears in stability tooltip | Pass a human-readable description to `modifyFlat` |
| Industry tooltip explains what it does | Override `getTooltipImagePath()` and `addAlphaCoreDescription()` as needed |
| Industry not buildable on uninhabited planets | Vanilla `BaseIndustry` handles this; verify `isAvailableToBuild()` |

---

## Differentiators

What makes the Cloning industry stand out from vanilla and generic mods:

| Feature | Design Note |
|---------|-------------|
| **First vanilla-style growth industry** | No vanilla industry directly boosts population growth; Cloning fills a genuine gap |
| **Flat + multiplier growth scaling with colony size** | Flat ensures value at small colonies; multiplier rewards large ones. Formula should be tuned so Clone Mass Manufactorium with Alpha Core + Embryo + SP upgrade does not make size 10 impossible to stop |
| **3-tier upgrade chain** | Gives a progression arc tied to colony investment, not just a binary on/off |
| **Stability penalty that scales with tier** | Higher tiers = more clones = more unrest. Creates a meaningful tradeoff: faster growth costs stability |
| **Organ consumption** | Differentiates from generic organics consumers; implies a darker flavor; creates a secondary market for the organ commodity |
| **Thematic coherence** | The description in mod_info.json ("simulated life by an alpha-level AI, weaker, frail") gives flavor context for both the stability penalty and the growth mechanic. This is a feature — lean into it in tooltips |
| **AI core reduces organ consumption** | An alpha core could "optimize" cloning to require fewer organs, giving the AI core a dual role (growth boost + input reduction) rather than just being a stat multiplier |

### Suggested Calibration Targets (for design, not requirements)

These are rough starting points to prevent trivializing the game:

| Tier | Base flat growth (per month?) | Base % growth | Stability penalty |
|------|-------------------------------|---------------|-------------------|
| Clone Vats | +0.5 | +20% | -1 |
| Cloning | +1.0 | +40% | -2 |
| Clone Mass Manufactorium | +2.0 | +75% | -3 |

SP upgrade: +0.5 flat, +15% additional
Alpha core: x1.5 multiplier on flat, +25% on percent, -1 organ demand
Biofactory Embryo: +30% additional growth percent

**Uncertain**: Whether the growth stat uses a monthly flat value or a per-cycle abstraction. Verify units against vanilla growth tooltip values before setting numbers.

---

*Research compiled: 2026-03-29. Based on Starsector 0.97a–0.98a API knowledge. Cross-check against vanilla source (`starfarer.api` javadoc or decompiled `starfarer.res` jar) before finalizing implementation.*
