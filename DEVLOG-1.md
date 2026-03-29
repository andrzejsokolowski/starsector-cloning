# Cloning Mod — Development Log

## Session: Three-Tier Industry Chain + LunaSettings Formula

### Goal
Extend the mod from a single `cloning` industry into a three-tier upgrade chain with a configurable population growth formula driven by LunaLib's LunaSettings.

---

### What Was Built

#### Industry Upgrade Chain
Three industries in sequence, defined in `data/campaign/industries.csv`:

```
Clone Vat Experiments  →  Cloning Industry  →  Cloning Megafactory
(clone_vat_experiments)    (cloning)             (cloning_megafactory)
```

- Only `Clone Vat Experiments` is directly buildable (`isAvailableToBuild() = true` on all three tiers — required for the upgrade/downgrade buttons to work).
- `upgrade` and `downgrade` columns in the CSV wire up the chain.

#### Growth Formula
Implemented in `BaseCloning.computeGrowthWeight()`:

```
FinalGrowth = basePopGrowthPerTier × (1 + (size × log10(size) × scalingFactor)) × globalMult
```

- Returns an `int` (via `Math.round`) — Starsector treats pop growth as integer points (10,000 = one size level).
- Each tier has its own base value read from LunaSettings.

#### Tier Base Values (defaults)

| Tier | Industry               | Default base growth |
|------|------------------------|---------------------|
| 1    | Clone Vat Experiments  | 50                  |
| 2    | Cloning Industry       | 125                 |
| 3    | Cloning Megafactory    | 250                 |

---

### File Structure

```
src/oddisz/industries/
    BaseCloning.java          # Abstract base — formula, apply/unapply, tooltip
    CloneVatExperiments.java  # Tier 1 (directly buildable)
    Cloning.java              # Tier 2
    CloningMegafactory.java   # Tier 3

data/campaign/industries.csv  # All three industries + upgrade chain
data/config/LunaSettings.csv  # Five configurable settings (see below)
```

---

### LunaSettings (`data/config/LunaSettings.csv`)

| fieldID           | Type   | Default | Description                                      |
|-------------------|--------|---------|--------------------------------------------------|
| `baseGrowthTier1` | Int    | 50      | Base growth points for Clone Vat Experiments     |
| `baseGrowthTier2` | Int    | 125     | Base growth points for Cloning Industry          |
| `baseGrowthTier3` | Int    | 250     | Base growth points for Cloning Megafactory       |
| `scalingFactor`   | Double | 0.15    | How much colony size amplifies growth            |
| `globalMult`      | Double | 1.0     | Master multiplier (set to 0 to disable all growth) |

ModID used: `cloning`

---

### Build Setup

`build.gradle` references LunaLib for compilation:
```groovy
compileOnly fileTree(dir: "${starsectorPath}/mods/LunaLib-2.0.5/jars", include: '*.jar')
```

`mod_info.json` declares LunaLib as a dependency:
```json
"dependencies": [{ "id": "lunalib", "name": "LunaLib" }]
```

---

### Gotchas Encountered

**`isAvailableToBuild()` blocks upgrades**
Returning `false` from `isAvailableToBuild()` on tiers 2 and 3 also blocked the upgrade/downgrade buttons — not just direct building. All three tiers must return `true`.

**Stale JAR**
The "Can not be built" tooltip persisted after fixing `isAvailableToBuild()` because the JAR hadn't been rebuilt. Always run `./gradlew jar` after Java changes.

**LunaLib mod folder name**
The installed folder is `LunaLib-2.0.5`, not `LunaLib` — the `build.gradle` path must match exactly.

**Pop growth is integer points**
Starsector's immigration weight system uses integer point values (10,000 points = one size level), not small floats. The original code used `0.5f` which was far too small. Sensible defaults are in the range of 50–250.
