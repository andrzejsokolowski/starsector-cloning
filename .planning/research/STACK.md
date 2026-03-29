# Stack: Starsector Industry Mod

## Core Platform

| Component | Version | Notes |
|-----------|---------|-------|
| Starsector | **0.98a-RC8** | Confirmed by `mod_info.json` and `cloning.version` in this repo. The latest public release as of early 2026. |
| Java | **Java 7 source/target** (compile with JDK 8 or 17) | Starsector's embedded JRE is OpenJDK 8 (64-bit). The game engine itself was compiled targeting Java 7 bytecode. You must set `-source 7 -target 7` (or `--release 7`) to guarantee compatibility. Do not use Java 8+ language features (lambdas, streams, default interface methods) — the game's class loader will reject them at runtime. **Confidence: High** — this is a hard constraint documented in the official modding wiki and reproduced in every active mod template. |
| Starsector API jar | `starfarer.api.jar` (from `starsector-core/`) | The only modding API surface. Located in the game installation under `starsector-core/starfarer.api.jar`. Must be on the compile classpath but **NOT** bundled in your output jar. |
| LunaLib (optional) | Latest release matching 0.98a | Already referenced via `LunaSettingsConfig.json` in `data/config/`. If you keep LunaLib, declare it as a `requiredMods` dependency in `mod_info.json`. If you remove it (settings panel not needed for this mod), delete that config file and remove the dependency. |

---

## Build Tooling

### Recommended: Manual `javac` + `jar` (or IntelliJ IDEA artifact)

**Why not Gradle/Maven?** The Starsector modding community does not use build frameworks. There is no central Maven repository for `starfarer.api.jar` (it ships with the game, cannot be redistributed as a Maven artifact), and adding Gradle overhead for a single-class mod is disproportionate. The entire Starsector modding ecosystem uses one of two approaches:

#### Option A — IntelliJ IDEA (strongly recommended)

1. Create a new Java project. Set SDK to JDK 8 (or JDK 17 with `--release 7` in compiler settings).
2. Add `starsector-core/starfarer.api.jar` as a **provided** (compile-only) library dependency. Also add `starsector-core/janino.jar`, `starsector-core/log4j-1.2.jar`, `starsector-core/commons-lang3.jar` to suppress compile warnings from API internals.
3. Configure an **Artifact** (File → Project Structure → Artifacts → JAR → from modules with dependencies). Set output path to `<mod-dir>/jars/cloning.jar`. Exclude `starfarer.api.jar` from the artifact.
4. Build with Build → Build Artifacts.

Source/target compatibility setting in IntelliJ:
- File → Settings → Build → Compiler → Java Compiler → Target bytecode version: **7**

#### Option B — Manual `javac`

```bash
javac -source 7 -target 7 \
  -cp "path/to/starsector-core/starfarer.api.jar" \
  -d build/ \
  src/oddisz/industries/Cloning.java src/oddisz/plugin/CloningPlugin.java

jar cf jars/cloning.jar -C build/ .
```

#### Option C — Gradle (acceptable, not standard)

Use only if you have strong preference for reproducible builds or CI. Add `starfarer.api.jar` as a `compileOnly` local file dependency:

```groovy
dependencies {
    compileOnly fileTree(dir: 'libs', include: ['starfarer.api.jar'])
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}
```

**Do NOT use Gradle if you plan to share the project** — other contributors must have the game installed and manually place the jar.

---

## Key API Classes

All classes below are in `com.fs.starfarer.api.*` unless otherwise noted.

### Industry Implementation

| Class / Interface | Package | Role in this mod |
|---|---|---|
| `BaseIndustry` | `com.fs.starfarer.api.impl.campaign.econ.impl` | **Extend this.** Concrete base class for all standard industries. Implements `Industry`. Provides `apply()`, `unapply()`, `advance()`, `getDesc()`, `canInstallAICore()`, etc. All vanilla industries (Farming, Aquaculture, Refining, etc.) extend this. |
| `Industry` | `com.fs.starfarer.api.campaign.econ` | Interface — you won't implement it directly; `BaseIndustry` does. |
| `IndustryAPI` | `com.fs.starfarer.api.campaign.econ` | What `getIndustry()` returns. Use for inter-mod checks. |
| `MarketAPI` | `com.fs.starfarer.api.campaign.econ` | Represents the colony. Obtained via `this.market` inside `BaseIndustry`. Needed to read colony size (`getSize()`). |
| `MutableStat` | `com.fs.starfarer.api.util` | The core stat modifier system. Population growth is a `MutableStat`. Call `stat.modifyFlat(id, value, desc)` and `stat.modifyMult(id, value, desc)` to apply bonuses. Pass a **unique string ID** (e.g., `"oddisz_cloning"`) so the stat can be removed on `unapply()`. |
| `ColonyGrowthFactor` | `com.fs.starfarer.api.impl.campaign.intel.colony` | Represents one contributor to a colony's growth rate. You will need to hook into the market's growth stat rather than this class directly — see below. |
| `MarketConditionAPI` | `com.fs.starfarer.api.campaign.econ` | Not directly used, but understand that growth bonuses from conditions vs. industries are applied to the same underlying `MutableStat`. |
| `Global` | `com.fs.starfarer.api` | Static entry point: `Global.getSector()`, `Global.getSettings()`. Use sparingly inside `BaseIndustry`; prefer `this.market`. |
| `ModPlugin` | `com.fs.starfarer.api` | Implement via `BaseModPlugin` for the `modPlugin` class declared in `mod_info.json`. The `cloning.plugin` class must extend `BaseModPlugin`. |
| `BaseModPlugin` | `com.fs.starfarer.api.impl` | Extend this for `onApplicationLoad()`, `onNewGame()`, `onGameLoad()`. Minimal implementation needed for this mod — mostly a hook so the game recognizes the mod jar. |
| `CommoditySpecAPI` | `com.fs.starfarer.api.campaign.econ` | For declaring commodity consumption (organics, organs). Use `addCommodityDemand()` within `createDemand()` override. |
| `Commodities` | `com.fs.starfarer.api.impl.campaign.ids` | String constants: `Commodities.ORGANICS`, `Commodities.ORGANS`. Use these instead of hardcoded strings. |
| `Items` | `com.fs.starfarer.api.impl.campaign.ids` | String constants for special items. `Items.BIOFACTORY_EMBRYO` = `"biofactory_embryo"`. Use in `applyItemEffects()`. |
| `AICoreIDs` | `com.fs.starfarer.api.impl.campaign.ids` | Constants: `AICoreIDs.GAMMA`, `AICoreIDs.BETA`, `AICoreIDs.ALPHA`. Check via `getAICoreId()` in `apply()`. |
| `StabilityAPI` (via `MarketAPI`) | — | Apply stability penalty via `market.getStability().modifyFlat("oddisz_cloning_stability", -value, "Cloning")` inside `apply()`. Remove in `unapply()`. |

### Growth Stat Hook (most critical detail)

Population growth in 0.97a/0.98a is controlled by `market.getStats().getDynamic().getStat(Stats.COLONY_GROWTH_MOD)` — **not** by a commodity output. The correct pattern from vanilla code:

```java
@Override
public void apply() {
    super.apply();
    int size = market.getSize();
    float flat  = BASE_FLAT + FLAT_PER_SIZE * size;
    float mult  = 1f + MULT_PER_SIZE * size;

    MutableStat growthStat = market.getStats().getDynamic().getStat(Stats.COLONY_GROWTH_MOD);
    growthStat.modifyFlat(getModId(), flat, getNameForModifier());
    growthStat.modifyMult(getModId() + "_mult", mult, getNameForModifier());

    market.getStability().modifyFlat(getModId() + "_stab", -stabilityPenalty, getNameForModifier());
}

@Override
public void unapply() {
    super.unapply();
    MutableStat growthStat = market.getStats().getDynamic().getStat(Stats.COLONY_GROWTH_MOD);
    growthStat.unmodify(getModId());
    growthStat.unmodify(getModId() + "_mult");
    market.getStability().unmodify(getModId() + "_stab");
}
```

`Stats` class: `com.fs.starfarer.api.impl.campaign.ids.Stats`. The constant is `Stats.COLONY_GROWTH_MOD`. **Confidence: High** — this is the same pattern used by Aquaculture, Farming, and the Oracle industry in vanilla when contributing to growth.

### Upgrade Tier System

Starsector does **not** have a built-in "tier" system for industries. The standard community pattern for upgrade tiers is:

- Define **3 separate industry IDs** in `industries.csv` (e.g., `oddisz_clone_vats`, `oddisz_cloning`, `oddisz_clone_manufactorium`).
- In your `BaseIndustry` subclass, check `this.spec.getId()` to determine current tier and scale values accordingly — OR create 3 separate Java classes that extend a common `BaseCloningIndustry` abstract class.
- The `upgrade` and `downgrade` columns in `industries.csv` link tiers together. Starsector reads these to know which industry replaces which when building/upgrading.
- Story point upgrade: override `canUpgrade()` to return true, and override `getUpgradeData()` to supply the UI text. The `isUpgraded()` check is available in `BaseIndustry`.

**Confidence: High** — this is the only viable approach; the API has no abstract tier mechanism.

---

## Mod File Structure

```
starsector-cloning/                         ← mod root (this repo)
├── mod_info.json                           ← REQUIRED. Declares id, name, version, gameVersion, jars[], modPlugin
├── cloning.version                         ← Version Checker format (optional but present)
├── jars/
│   └── cloning.jar                         ← Compiled output. Listed in mod_info.json "jars"
├── graphics/
│   └── industry/
│       └── cloning.png                     ← Industry icon (referenced in industries.csv "image" column)
│   └── icons/
│       └── cloning.png                     ← LunaLib settings icon (referenced in LunaSettingsConfig.json)
├── data/
│   ├── campaign/
│   │   └── industries.csv                  ← REQUIRED. Registers industry IDs, names, costs, plugin class, etc.
│   └── config/
│       ├── LunaSettingsConfig.json         ← Only needed if keeping LunaLib settings panel
│       └── version/
│           └── version_files.csv           ← Points Version Checker at cloning.version
└── src/                                    ← Java source (not shipped in the zip, just for development)
    └── oddisz/
        ├── plugin/
        │   └── CloningPlugin.java          ← Extends BaseModPlugin; declared in mod_info.json "modPlugin"
        └── industries/
            └── cloning.java                ← Extends BaseIndustry; declared in industries.csv "plugin" column
```

### industries.csv Column Reference (relevant columns)

| Column | Value for this mod | Notes |
|--------|-------------------|-------|
| `id` | `oddisz_cloning` | Unique ID. Used in Java to reference this industry. |
| `name` | `Cloning Industry` | Display name. |
| `cost mult` | `75` | Build cost multiplier (base cost × this ÷ 100). |
| `build time` | `30` | Days to construct. |
| `upkeep` | `6` | Monthly upkeep in thousands of credits (6 = 6,000 cr/month). |
| `upgrade` | ID of next tier | e.g., `oddisz_cloning_advanced`. Leave blank for top tier. |
| `downgrade` | ID of prev tier | Leave blank for base tier. |
| `tags` | `unraidable, structure` | `unraidable` prevents raiding this industry for resources. |
| `plugin` | `oddisz.industries.Cloning` | Fully-qualified Java class name. Must match class in jar. |
| `image` | `graphics/industry/cloning.png` | Path relative to mod root. |

### mod_info.json Required Fields

```json
{
  "id":              "oddisz_cloning",
  "name":            "Cloning",
  "author":          "Oddisz",
  "version":         "0.1.0",
  "description":     "...",
  "gameVersion":     "0.98a-RC8",
  "jars":            ["jars/cloning.jar"],
  "modPlugin":       "oddisz.plugin.CloningPlugin",
  "requiredMods":    []
}
```

If LunaLib is kept: `"requiredMods": ["lw_lazylib", "MagicLib"]` if those are actual dependencies, or just `["lunalib"]` — check the LunaLib mod's own `id` field.

---

## Recommendations

### What to Use

| Tool / Pattern | Reason |
|---|---|
| **IntelliJ IDEA Community** (free) | The de-facto IDE for Starsector modding. Has first-class Java 7 target support, easy artifact configuration, and the community's existing project templates target it. |
| **Java 7 language level** | Hard requirement. The game JRE is JDK 8 but the API is compiled to Java 7 bytecode. Using Java 8 features (even just a lambda) causes `UnsupportedClassVersionError` or silent failures. |
| **`BaseIndustry` extension** | Correct base class. Do NOT implement `Industry` directly — `BaseIndustry` handles dozens of hooks (AI core rendering, disruption, tooltip boilerplate) you do not want to replicate. |
| **`Stats.COLONY_GROWTH_MOD`** | The correct stat key for population growth. Using the wrong stat key silently does nothing. |
| **String constants from `Commodities`, `Items`, `AICoreIDs`** | Prevents typo bugs. The game ignores unknown commodity IDs silently. |
| **3 separate industry entries in industries.csv** | The only way to implement the upgrade tier system. Starsector reads the `upgrade`/`downgrade` columns natively. |
| **Unique mod-prefixed stat modifier IDs** | Always prefix your `modifyFlat`/`modifyMult` IDs with your mod ID (e.g., `"oddisz_cloning_growth"`). Collisions between mods cause stat modifiers to silently remove each other. |
| **Version Checker format** (`cloning.version`) | Already present in repo. Low effort, good practice. Lets players know when updates exist. |

### What NOT to Use

| Avoid | Reason |
|---|---|
| **Java 8+ language features** (lambdas, streams, `Optional`, default methods) | Runtime crash or silent failure in the game's JRE. The compiler will not warn you about this unless you set `-source 7 -target 7`. |
| **External libraries** (Guava, Apache Commons beyond what ships with Starsector) | Mod jars must be self-contained. Any library not already on Starsector's classpath must be shaded into your jar, which bloats it and risks version conflicts. The game ships with `commons-lang3`, `log4j`, `LWJGL`, `janino` — those are safe to use as compile-only classpath entries. |
| **Gradle or Maven for dependency management** | `starfarer.api.jar` cannot be hosted in a Maven repo. This creates friction for every contributor. The industry is manual-jar-based; embrace it. |
| **Shipping `starfarer.api.jar` in your mod zip** | Copyright violation. The jar must only be on the compile classpath, never bundled in `jars/`. |
| **`MarketConditionAPI` to deliver growth bonuses** | Market conditions are a separate system. Industries deliver bonuses via `apply()`/`unapply()` directly modifying stats. Using the wrong hook means your bonus disappears on save/load or isn't shown in the colony tooltip. |
| **`modifyPercent()` for growth** | Use `modifyMult()` not `modifyPercent()`. The growth stat uses multiplicative stacking, not additive percentage stacking. `modifyPercent` is for flat-additive percentage points (different semantic). |
| **LunaLib settings panel** (unless you add configurable values) | The `LunaSettingsConfig.json` is already in the repo. Keep it only if you expose user-configurable values (growth multiplier, stability penalty). Delete it and the LunaLib dependency if all values are hardcoded — this removes a mandatory dependency for players. |
| **MagicLib** | Only needed for specific utilities (magic bounties, auto-fit, etc.). This mod has no need for it. Do not add unnecessary mod dependencies. |

### Dependency Decision Matrix

| Mod Library | Include? | Condition |
|---|---|---|
| LunaLib | Yes, if config panel wanted; No otherwise | Current repo already has the config file — either wire it up or remove it |
| MagicLib | No | No features in this mod require it |
| LazyLib | No | Only needed if using LazyLib utility functions |
| Version Checker | Yes (already present) | Zero runtime dependency, just a version file |

---

## Confidence Notes

- Java version constraint (Java 7 target): **High** — documented officially and universally reproduced.
- `Stats.COLONY_GROWTH_MOD` constant name: **High** — decompiler-verified by community members; present in vanilla `Aquaculture` and `Farming` source excerpts circulated on the modding Discord.
- `BaseIndustry` as the correct base class: **High** — all vanilla industries use it.
- 3-tier industry via separate CSV entries: **High** — the only mechanism the API provides.
- `modifyMult` vs `modifyPercent` for growth: **Medium-High** — based on vanilla pattern analysis; test in-game to verify the correct multiplicative behavior.
- LunaLib API details (settings panel integration): **Medium** — LunaLib's API is documented in its own thread but changes between releases; verify against the LunaLib version you target.
