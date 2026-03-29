# Pitfalls: Starsector Cloning Industry Mod

---

## 1. Double-Applying Population Growth Bonuses

**Risk:** The `apply()` method on `BaseIndustry` is called repeatedly — every time the colony re-evaluates its stats (which can happen multiple times per in-game day cycle). If you call `modifyFlat` or `modifyMult` on the growth stat using a non-unique source string, or if you fail to call `unapply()` properly, the bonus stacks on itself each tick, compounding into absurd growth rates that break the economy and can crash save files.

**Warning signs:**
- Colony population grows by orders of magnitude within a few in-game months
- Growth tooltip shows the same source listed multiple times
- Removing the industry in-game does not reset growth back to baseline
- Saving and reloading the game changes the displayed growth value

**Prevention:**
- Always pass a stable, unique source string — use `this.getClass().getName()` or a constant like `"oddisz_cloning"` — as the first argument to every `modifyFlat`/`modifyMult` call
- Override `unapply()` and call the matching `unmodifyFlat`/`unmodifyMult` with the same source string; never leave it empty
- Verify the stat is applied once by checking the tooltip in-game and confirming only one entry appears per modifier source
- Do not compute the bonus value inside `apply()` using a field you mutate; compute it from pure colony state each call

**Relevant phase:** Phase 1 (core industry implementation)

---

## 2. Wrong Stat Key for Population Growth

**Risk:** Starsector's growth system has multiple related but distinct stat objects. Using the wrong MutableStat key — for example, modifying the market's `PRODUCTION_CAPACITY` or a condition's score instead of the actual incoming growth stat — results in a mod that compiles and loads cleanly but has zero visible effect in-game, with no error logged.

**Warning signs:**
- Industry is present and applying without errors, but the colony growth rate shows no change
- The growth tooltip shows no entry for your mod's source string
- Growth behaves identically with and without the industry

**Prevention:**
- The correct stat to modify for population growth incoming rate is obtained via `market.getStats().getIncoming().getColonyGrowthFactor()` — this returns the `MutableStat` that the game actually reads for growth tick calculations
- Cross-reference vanilla industries that affect growth (e.g., `Aquaculture`, `Farming`) in the decompiled source to confirm the exact method chain before writing your own
- Use `Stats.COLONY_GROWTH_MOD` or the equivalent constant rather than hard-coding a string key; look up the constant in `com.fs.starfarer.api.util.Misc` or `com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity`
- Add a debug log line printing the stat value before and after `apply()` during development

**Relevant phase:** Phase 1 (core industry implementation)

---

## 3. Flat vs. Multiplier Growth Bonus Applied in Wrong Order

**Risk:** Starsector resolves MutableStat bonuses in a fixed order: flat bonuses are summed first, then multipliers are applied to the total. If you intend a multiplier to scale the colony's existing natural growth AND your flat bonus, but you apply both as separate modifiers on the same stat, the multiplication may only apply to the base value before your flat bonus, or vice versa depending on how the stat is composed. This produces growth numbers that do not match design intent and are difficult to diagnose.

**Warning signs:**
- At small colony sizes the multiplier appears to do nothing noticeable
- At large colony sizes the flat bonus appears negligible compared to what was planned
- The math in the tooltip does not match what you calculated on paper

**Prevention:**
- Understand the resolution order: `base + sum(flat mods)` then `* product(mult mods)` — this is applied per-stat, not per-source
- If you want the multiplier to scale the combined total (base + your flat), you cannot achieve this with a single MutableStat; instead compute a combined effective flat value in Java and apply only one `modifyFlat` call, recalculating it each `apply()` call based on colony size
- Test at colony size 3, 5, and 7 to confirm the scaling curve matches the design spec before moving to balancing

**Relevant phase:** Phase 1 (core industry implementation), Phase 2 (balancing)

---

## 4. AI Core Integration Using Stale Core State

**Risk:** If you read the AI core ID inside `apply()` using `getAICoreId()` without accounting for the fact that this can return `null` before a core is installed or after it is removed, a `NullPointerException` will be thrown every time the colony recomputes stats, which is frequent. This crashes the game silently in the background and manifests as a colony that stops updating or a log full of NPE stack traces.

**Warning signs:**
- `starsector.log` contains repeated `NullPointerException` at your plugin's `apply()` method
- Colony stats appear frozen after installing or removing an AI core
- The game stutters or pauses briefly every few seconds on the colony screen

**Prevention:**
- Always null-check: `if (getAICoreId() != null)` before branching on core type
- Use `AICoreAPI.getAICoreSpec(getAICoreId())` only inside the null-check guard
- Mirror how vanilla industries like `OrbitalStation` handle core checks — they always guard with a null check and use `equals()` against the constant IDs (`Commodities.ALPHA_CORE`, `Commodities.BETA_CORE`, `Commodities.GAMMA_CORE`)
- Test by installing a core, saving, reloading, then removing the core to catch lifecycle edge cases

**Relevant phase:** Phase 1 (AI core feature), Phase 2 (QA)

---

## 5. AI Core Bonus Not Removed When Core Is Uninstalled

**Risk:** AI core bonuses applied via `modifyFlat`/`modifyMult` persist after the core is removed if `unapply()` does not also remove the core-specific modifier. The source string for core bonuses must be distinct from the base industry source string so they can be removed independently.

**Warning signs:**
- Removing the AI core from the industry does not change the displayed growth bonus
- Reinstalling a lower-tier core still shows the higher-tier bonus in the tooltip

**Prevention:**
- Use separate source strings for base bonuses vs. core bonuses: e.g., `"oddisz_cloning_base"` and `"oddisz_cloning_core"`
- In `unapply()`, always call `unmodify` for both source strings unconditionally — removing a modifier that was never applied is a no-op and safe
- Alternatively, call `unmodifyAll(source)` for all possible core source strings at the start of every `apply()` call, then re-apply only the currently-installed core's bonus; this is the simplest safe pattern

**Relevant phase:** Phase 1 (AI core feature)

---

## 6. Biofactory Embryo Item Not Recognized After Reload

**Risk:** Colony special items are persisted in save data by their string ID. If you reference the Biofactory Embryo using a hardcoded string like `"biofactory_embryo"` but get the casing or underscores wrong, the check silently fails on every `apply()` call. More subtly, if you only check for the item inside `apply()` and do not also update the tooltip, the player sees no feedback that the item is doing anything.

**Warning signs:**
- Installing the Biofactory Embryo shows no change to the growth stat or tooltip
- No error in the log — the check just silently returns false
- After a save/reload the bonus disappears even though the item is still installed

**Prevention:**
- Use the constant `Items.BIOFACTORY_EMBRYO` from the API rather than a raw string — this is `"biofactory_embryo"` but using the constant protects against typos and version changes
- Retrieve installed special items via `market.getSpecialItem()` and compare by ID with `.equals()`, not `==`
- Add a tooltip entry in `addPostDemandSection` that explicitly confirms whether the item is detected as active
- Test detection by removing and re-adding the item across a save/reload cycle

**Relevant phase:** Phase 1 (colony item feature)

---

## 7. Industry Tier Upgrades Not Wired to Separate Industry IDs

**Risk:** In Starsector, upgrade tiers for an industry are implemented as completely separate industry IDs in `industries.csv` with an `upgrade` column pointing to the next tier's ID. A common mistake is implementing all tier logic inside one Java class and toggling behavior with a level variable, but failing to set the `upgrade` column in the CSV — meaning the in-game upgrade button never appears and the player cannot progress past tier 1.

**Warning signs:**
- The upgrade button is missing or grayed out in the industry panel for tier 1 and tier 2
- The industry can be built but shows no upgrade path
- Attempting to add the upgrade column after initial testing causes save compatibility issues

**Prevention:**
- Define all three tiers in `industries.csv` as separate rows: `oddisz_clonevats`, `oddisz_cloning`, `oddisz_clonemanufactorium`
- Set the `upgrade` column of tier 1 to the ID of tier 2, and tier 2 to the ID of tier 3; leave tier 3 blank
- Set the `downgrade` column of tiers 2 and 3 to the previous tier's ID
- Each tier can share the same Java plugin class or use separate classes; if sharing, query `getId()` inside `apply()` to determine which tier is active
- Test the full upgrade chain from tier 1 to tier 3 in a single session before committing the CSV structure

**Relevant phase:** Phase 1 (industry structure), Phase 2 (progression QA)

---

## 8. Stability Penalty Applied as Positive Value (Sign Error)

**Risk:** The stability stat in Starsector uses negative modifiers to represent penalties. Passing a positive value to `modifyFlat` on the stability stat results in a stability bonus instead of a penalty. Because the value is positive and the industry description says "penalty," this is extremely easy to miss during testing if you do not specifically check the tooltip.

**Warning signs:**
- Colony stability increases after building the Cloning industry instead of decreasing
- The stability tooltip shows your industry as a bonus entry
- Players report that the industry feels unbalanced in a different direction than expected

**Prevention:**
- Always pass a negative value when applying a stability penalty: `stats.getStability().modifyFlat(source, -penaltyValue, "Cloning: unstable population")`
- Double-check the sign by inspecting the stability tooltip in-game immediately after the first build test
- Add a comment in the code noting the sign convention used to avoid future confusion during maintenance

**Relevant phase:** Phase 1 (stability penalty implementation)

---

## 9. Resource Demand Not Declared in CSV (Organics/Organs)

**Risk:** Starsector industries declare their commodity inputs and outputs in `industries.csv` (or via the `demand`/`supply` plugin methods). If you rely entirely on the Java plugin to visually display demand without also declaring it in the CSV, the market's shortage/surplus calculations will not account for your industry, and the supply chain AI will not route goods to the colony. The industry will appear to function but colonies will never actually import organics or organs.

**Warning signs:**
- The industry's input resources show as permanently satisfied regardless of colony trade routes
- The colony never generates a demand mission for organics or organs
- Decompiling the save file shows no demand entries for your industry's IDs

**Prevention:**
- Use the `demand` column in `industries.csv` for static, tier-independent inputs, or override `createDemand()` / use `addDemand()` in the Java plugin for dynamic per-tier inputs
- For organs (a commodity that may not exist in vanilla — confirm whether it is `"organs"` or a mod-added commodity), verify the commodity ID exists in `data/campaign/commodities.csv` before referencing it
- Test demand by building the industry on a colony with no trade routes and confirming shortage icons appear for the required inputs

**Relevant phase:** Phase 1 (resource consumption), Phase 2 (economy testing)

---

## 10. mod_info.json Plugin Class Path Mismatch

**Risk:** The `modPlugin` field in `mod_info.json` must match the fully-qualified Java class name of your `ModPlugin` implementation exactly, including package capitalization. A mismatch — even a single character difference — causes the mod to fail to load entirely with a `ClassNotFoundException` logged, which looks identical to a missing JAR error and can be confusing.

**Warning signs:**
- The game launches but your mod's industries are absent from the build menu
- `starsector.log` shows `ClassNotFoundException: cloning.plugin` or similar
- The mod appears in the mod list but all its content is missing

**Prevention:**
- Ensure the `modPlugin` value in `mod_info.json` exactly matches the fully-qualified class name: e.g., `"oddisz.cloning.CloningModPlugin"` must correspond to a class `CloningModPlugin` in package `oddisz.cloning`
- The JAR in the `jars` array must include the compiled `.class` file at the matching path inside the archive
- After every build, open the JAR with a zip tool and verify the class file exists at `oddisz/cloning/CloningModPlugin.class`
- If the mod has no custom `ModPlugin` (only data files and an industry plugin), the `modPlugin` field can be omitted entirely — leaving it set to a non-existent class is worse than omitting it

**Relevant phase:** Phase 1 (project setup), every compile cycle

---

## 11. JAR Not Rebuilt After Code Changes (Stale Bytecode)

**Risk:** Starsector loads industry plugins from the compiled JAR at startup. If you modify Java source files but forget to recompile and re-package the JAR before launching the game, the old bytecode runs silently. This is one of the most common time-wasting traps — you make a fix, test, see the bug still present, and assume your logic is wrong, when actually the old code is running.

**Warning signs:**
- A code change that should obviously fix a bug has no effect
- Adding a deliberate syntax error to a `.java` file, running the game, and having it still work (proving the source is not being compiled on launch)
- The JAR's modification timestamp is older than the source file's modification timestamp

**Prevention:**
- Establish a single build command (Gradle, Ant, or a shell script) that compiles all sources and packages the JAR atomically; never manually copy `.class` files
- Add the JAR modification timestamp to your test checklist
- Starsector does not hot-reload mods; a full game restart is always required after a JAR change

**Relevant phase:** All phases (ongoing development discipline)

---

## 12. `industries.csv` Column Count or Header Mismatch

**Risk:** Starsector's CSV parser is strict about column count relative to the header row. Adding, removing, or reordering columns without updating the header — or adding trailing commas — causes a parse error that prevents all industries in the file (not just your own) from loading. The error appears as a generic data-loading failure with a line number that does not always point to the real problem.

**Warning signs:**
- All vanilla industries disappear from the build menu, not just your custom one
- `starsector.log` shows a CSV parse error with a line number pointing into `industries.csv`
- The colony screen is blank or crashes on open

**Prevention:**
- Copy the vanilla `industries.csv` header row exactly and only append new rows; never modify the header
- Use a CSV editor that validates column count (LibreOffice Calc, Excel) rather than a plain text editor
- The column count in your data row must exactly match the header — trailing empty fields must still be represented by trailing commas up to the final column
- Validate the CSV after every edit by opening it in a spreadsheet application and confirming no shifted columns

**Relevant phase:** Phase 1 (data file setup), any time the CSV is edited

---

## 13. Balance-Breaking Growth Rate at High Colony Sizes

**Risk:** Population growth that scales with `10^size` can produce astronomically large bonuses at size 6+ colonies. A multiplier of even 1.5x on top of a large flat bonus at colony size 7 or 8 can result in a colony hitting size 10 in a matter of weeks rather than years, trivializing the entire colony progression system and making other industries irrelevant.

**Warning signs:**
- A size 6 colony reaches size 10 within 1-2 in-game years of building the industry
- The growth tooltip shows a bonus larger than the entire vanilla growth capacity of the colony
- The campaign economy collapses because the player's income outruns any challenge

**Prevention:**
- Cap the effective growth bonus at a maximum absolute value regardless of colony size, using `Math.min(computedBonus, MAX_BONUS_CONSTANT)`
- Test at sizes 3, 5, 6, and 7 (not just small colonies) before declaring balance acceptable
- Compare against vanilla's fastest growth scenarios (max Farming + conditions + officer bonuses) and target no more than 2-3x that ceiling as your maximum
- Consider a diminishing returns curve: the bonus increases with size but the marginal gain shrinks, e.g., `baseBonus * Math.log10(colonySize + 1)`
- The story point upgrade and AI cores should each add a modest percentage on top of the base, not multiplicative stacking on top of each other

**Relevant phase:** Phase 2 (balancing), Phase 3 (playtesting)

---

## 14. Story Point Upgrade Not Persisted Across Save/Reload

**Risk:** The story point upgrade state is tracked internally by `BaseIndustry` via `isImproved()`. If you manually track the upgrade state in an instance variable instead of using the API's built-in flag, the state will be lost on every reload because instance variables are not persisted in save data — only data stored via `market.getMemoryWithoutUpdate()` or the built-in improvement system persists.

**Warning signs:**
- The story point upgrade visually shows as applied in the UI, but after reloading the save the bonus is gone
- `isImproved()` returns `false` after a reload even though the player paid the story point
- The upgrade can be applied multiple times without spending story points

**Prevention:**
- Use `isImproved()` as the sole source of truth for whether the story point upgrade is active; never cache this in a field
- Override `applyImproveEffect()` to apply the bonus and `unapplyImproveEffect()` to remove it; do not apply improvement bonuses inside `apply()` directly
- Test by: (1) applying the story point upgrade, (2) saving, (3) reloading, (4) confirming the bonus is still present in the tooltip

**Relevant phase:** Phase 1 (story point upgrade feature)

---

## 15. `gameVersion` Field Causing Load Failures on Version Mismatch

**Risk:** The `gameVersion` field in `mod_info.json` is checked by the game's mod loader. If the field does not match the running game version exactly (including the `-RC8` suffix), the game may warn the player or refuse to load the mod depending on user settings. More critically, if the `originalGameVersion` field is set incorrectly it can cause false compatibility warnings that discourage players from using the mod.

**Warning signs:**
- A "mod made for a different version" warning appears at launch even on the correct game version
- Players report the mod not appearing in their mod list
- The game version field in `mod_info.json` reads `0.97a` while players are on `0.98a-RC8`

**Prevention:**
- Set `gameVersion` to exactly the current release string including the RC suffix: `"0.98a-RC8"`
- Keep `originalGameVersion` in sync with `gameVersion` unless you have a specific reason to distinguish them
- Do not use partial version strings like `"0.98a"` — the game matches the full string

**Relevant phase:** Phase 1 (project setup), before any public release

---

## 16. Missing or Incorrect `tags` in `industries.csv` Breaking Raid/Event Logic

**Risk:** The `tags` column in `industries.csv` controls whether the industry is affected by raids, events, and certain campaign mechanics. The `"unraidable"` tag (already present in this project's CSV) prevents the industry from being a valid raid target. However, forgetting the `"structure"` tag (also present) means the industry will not be treated as a physical structure — it may appear in wrong UI contexts or be ineligible for certain game mechanics that require a structure tag.

**Warning signs:**
- The industry appears in raid target selection even with `unraidable` set (indicates `unraidable` was misspelled)
- The industry does not appear in the structures tab of the colony screen
- AI fleet behavior around the colony is inconsistent

**Prevention:**
- Verify tags are comma-separated within the cell with no extra spaces: `"unraidable, structure"` matches the vanilla format
- Cross-reference tags against vanilla industries of similar type to ensure nothing is missing
- Check that `unraidable` is spelled correctly — it is a case-sensitive string match internally

**Relevant phase:** Phase 1 (data file setup)

---

## 17. Organs Commodity Not Existing in Vanilla Data

**Risk:** The project specification lists "organs" as an input commodity. Vanilla Starsector does not have a commodity with the ID `"organs"` — it has `"organics"`. If you define a demand for a non-existent commodity ID, the game will either silently ignore the demand, throw a data error at load time, or create a phantom commodity entry that has no supply chain and can never be satisfied.

**Warning signs:**
- The industry shows a permanent red shortage for an input with no name or a blank icon
- `starsector.log` shows `"No commodity found for id: organs"` or similar
- The industry's demand is listed as satisfied even when no goods are present

**Prevention:**
- Confirm the exact commodity ID by looking it up in `[starsector install]/starsector-core/data/campaign/commodities.csv`
- If `"organs"` does not exist in vanilla, either: (a) use `"organics"` as the only organic-type input, (b) define a custom `"organs"` commodity in your own `data/campaign/commodities.csv` override, or (c) remove the organs demand entirely
- Adding a custom commodity requires a corresponding entry in the commodities CSV with all required fields (name, icon, base price, etc.) — an incomplete entry will cause load errors
- If adding a custom commodity, also add a supply source somewhere in the game, or the demand can never be satisfied and will permanently debuff the industry

**Relevant phase:** Phase 1 (resource consumption design)

---

## 18. Upkeep Cost Defined in CSV vs. Computed in Plugin (Double-Counting)

**Risk:** Industry upkeep can be set via the `upkeep` column in `industries.csv` (a static credit cost multiplier) AND via the Java plugin's `getUpkeep()` override. If you define a value in both places, the costs stack, making the industry far more expensive than intended. The CSV upkeep is applied as a base and the plugin can further modify it.

**Warning signs:**
- The displayed upkeep cost in the industry panel is roughly double what was designed
- Removing the upkeep logic from the plugin does not zero out the cost

**Prevention:**
- Choose one source of truth: either use the CSV `upkeep` column for a fixed cost, or set it to `0` in the CSV and compute everything in the plugin's `getUpkeep()` or `apply()` method
- The CSV `upkeep` column is a multiplier (not an absolute value) applied to a base rate — read the vanilla CSV comments to understand the scale
- Test the displayed upkeep at colony size 3 and 6 against your design spec

**Relevant phase:** Phase 1 (economy design)

---

## 19. Java Classpath Missing `starfarer.api.jar` at Compile Time

**Risk:** Starsector's modding API is provided by `starfarer.api.jar` (and `starfarer_obf.jar` for internal access). If this JAR is not on the classpath when compiling your mod, you will get hundreds of `cannot find symbol` errors for every API class. This is purely a build configuration issue, not a code issue, but it blocks all progress until resolved.

**Warning signs:**
- The compiler reports errors for `BaseIndustry`, `MutableStat`, `MarketAPI`, and other API classes
- IntelliJ IDEA or Eclipse shows red imports for any `com.fs.starfarer.api` package
- The project compiles fine locally but fails on a clean checkout

**Prevention:**
- Add `[starsector install]/starsector-core/starfarer.api.jar` to the compile-time classpath; it should be `compileOnly` (not bundled into your mod JAR — the game provides it at runtime)
- For Gradle: `compileOnly files('path/to/starfarer.api.jar')`
- For IntelliJ: add as a module dependency with scope `Provided`
- Commit a `build.gradle` or equivalent that references the JAR by a relative or environment-variable path so other contributors can build without manual setup

**Relevant phase:** Phase 1 (project setup)

---

## 20. Industry Icon Path Case Sensitivity on Linux/macOS Hosts

**Risk:** The `image` column in `industries.csv` and the icon path in `LunaSettingsConfig.json` use forward-slash paths. On Windows (where Starsector is typically developed), path lookups are case-insensitive, so `Graphics/Industry/Cloning.png` and `graphics/industry/cloning.png` both work. On Linux hosts (where players may run Starsector via Steam or a server), paths are case-sensitive — a mismatch causes a missing texture error and the industry icon defaults to a generic placeholder or causes a log error.

**Warning signs:**
- Industry icon appears correctly during development but is missing in screenshots shared by Linux players
- `starsector.log` on a Linux system shows `WARNING: Missing texture: graphics/Industry/cloning.png`
- The icon works in your build but not in a distribution zip

**Prevention:**
- Use all-lowercase paths consistently: `graphics/industry/cloning.png` and `graphics/icons/cloning.png`
- Match the path in the CSV exactly to the actual filename and directory name on disk, including case
- The current `industries.csv` entry already uses `graphics/industry/cloning.png` (lowercase) — ensure the actual file is at exactly that path

**Relevant phase:** Phase 1 (asset setup), before first public release
