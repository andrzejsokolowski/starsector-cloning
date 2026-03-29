# Cloning Mod — Development Log 2

## Session: AI Core Effects, Omega Core, Crew Scaling, Versioning

---

### AI Core Custom Effects

Overrode the vanilla AI core system in `BaseCloning` to give each tier custom behavior instead of the default -1 demand / +1 supply.

**Core effects:**

| Core | Upkeep | Growth | Crew & Marines |
|---|---|---|---|
| None | — | Flat base | No |
| Gamma | -25% | Flat base | No |
| Beta | -10% | Flat base | Yes (scales with size) |
| Alpha | -25% | Scales with colony size | Yes (scales with size) |
| Omega *(Astral Ascension)* | -50% | Scales with colony size (2× factor) | Yes (scales with size) |

**Key methods overridden:**
- `applyAICoreToIncomeAndUpkeep()` — custom upkeep multipliers
- `applyAlpha/Beta/GammaCoreSupplyAndDemandModifiers()` — all emptied to suppress vanilla +1 supply / -1 demand
- `addAlpha/Beta/GammaCoreDescription()` — custom text in tooltip and core picker dialog
- `addAICoreSection()` — intercepts `omega_core` before delegating to super

**Crew/marines production** scales with `market.getSize()`:
- Crew: `market.getSize()`
- Marines: `Math.max(1, market.getSize() - 1)`

---

### Omega Core (Astral Ascension)

Omega core ID is `omega_core` (from Astral Ascension's `commodities.csv`).

No mod detection needed — `OMEGA_CORE` is a plain string constant. If Astral Ascension is absent, `getAICoreId()` can never return `"omega_core"`, so all omega branches are simply dead code. Zero crash risk.

The omega growth formula doubles `scalingFactor`:
```java
if (OMEGA_CORE.equals(getAICoreId())) scaling *= 2.0;
```

---

### Growth Tooltip Fix

"Scales with colony size" text is now conditional — only shown when alpha or omega core is equipped, since flat growth doesn't scale:

```java
if (hasScaling()) {
    tooltip.addPara("Population growth: +%d points (scales with colony size)", ...)
} else {
    tooltip.addPara("Population growth: +%d points", ...)
}
```

Stability penalty always says "scales with colony size" since it always does.

---

### Versioning

Two files must be kept in sync for version checking to work:

| File | Role |
|---|---|
| `cloning.version` | Fetched from GitHub by Version Checker at runtime. If remote > local, update notification shown in-game. |
| `mod_info.json` | Local version shown in the mod manager. |

Both updated to `0.1.0`. `mod_info.json`'s `updateCheckURL` was also corrected — it previously pointed to `version.json` (nonexistent), now correctly points to `cloning.version`:
```
https://raw.githubusercontent.com/andrzejsokolowski/starsector-cloning/main/cloning.version
```

Note: `raw.githubusercontent.com` is required — `github.com` returns an HTML page, not raw file content.

`directDownloadURL` in `cloning.version`:
```
https://github.com/andrzejsokolowski/starsector-cloning/releases/latest/download/cloning.zip
```

**Release checklist:**
1. `./gradlew jar`
2. Zip mod folder (exclude `.git`, `.gradle`, `build`, `src`)
3. GitHub release tagged `0.1.0`, attach `cloning.zip`
4. Push updated `cloning.version` to `main`

---

### Gotchas

**`addAICoreSection` signature** takes `(TooltipMakerAPI, String, AICoreDescriptionMode)` — the `String` is the core ID. When delegating to super for vanilla cores, call the two-arg variant `super.addAICoreSection(tooltip, mode)` to avoid passing omega through vanilla logic.

**Unused import warning** — `AICoreDescriptionMode` and `IndustryTooltipMode` are inherited through `BaseIndustry` and don't need explicit imports; removing them clears the warnings.
