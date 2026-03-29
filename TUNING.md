# Cloning Industry — Number Tuning Reference

## CSV Values (`data/campaign/industries.csv`)

| Field        | Current Value | Notes                                                            |
| ------------ | ------------- | ---------------------------------------------------------------- |
| `cost mult`  | 75            | 100 = 500k. 75 = 75% of 500k = 375k                              |
| `build time` | 30            | Days to build. Vanilla spaceport is 15, heavy industry is 120.   |
| `upkeep`     | 6             | Base monthly upkeep ×1000 credits. Vanilla heavy industry is 12. |

**To edit:** open `data/campaign/industries.csv` directly. No recompile needed, just copy the CSV to mods folder.

---

## Java Values (`src/oddisz/industries/cloning.java`)

### Growth bonus

```java
private static final float GROWTH_WEIGHT_BONUS = 0.5f;
```

Controls how fast the colony grows. This value is added to `incoming.getWeight()` each growth cycle.

For reference, `CoreImmigrationPluginImpl.GROWTH_NO_INDUSTRIES` (base growth with no pop-boosting industries) is roughly **0.1**. So 0.5 is ~5× the base rate — very strong.

| Value | Effect              |
| ----- | ------------------- |
| 0.1   | Same as base growth |
| 0.3   | Moderate boost      |
| 0.5   | Current — very fast |
| 1.0   | Extremely fast      |

The `incoming.add(Factions.NEUTRAL, GROWTH_WEIGHT_BONUS * 10f)` line controls the _composition_ of new population (neutral faction bias). The multiplier of 10f is arbitrary — adjust or remove if you want different faction makeup.

### Stability penalty

```java
market.getStability().modifyFlat(getModId(), -market.getSize(), "Cloning Industry");
```

Currently: **−(colony size)**. A size-3 colony loses 3 stability, size-6 loses 6.

Vanilla max stability is 10. Common stability penalties from other sources:

- Low accessibility: −1 to −3
- Pather cells: −1 to −3
- Corruption: −1 to −2

Possible formulas to consider:

```java
// Fixed penalty regardless of size
market.getStability().modifyFlat(getModId(), -3f, "Cloning Industry");

// Milder scaling (half of size)
market.getStability().modifyFlat(getModId(), -(market.getSize() / 2f), "Cloning Industry");

// Capped penalty
market.getStability().modifyFlat(getModId(), -Math.min(market.getSize(), 5), "Cloning Industry");
```

---

## Rebuild after Java changes

```
gradlew.bat jar
```

Then copy `jars/cloning.jar` to your mods folder. CSV changes don't require a rebuild.
