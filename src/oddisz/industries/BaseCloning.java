package oddisz.industries;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;

public abstract class BaseCloning extends BaseIndustry implements MarketImmigrationModifier {

    private static final String MOD_ID = "cloning";

    // Astral Ascension commodity ID — safe to reference even if mod is absent;
    // getAICoreId() will simply never return this value in that case.
    private static final String OMEGA_CORE = "omega_core";

    private static final double DEFAULT_SCALING_FACTOR = 0.15;
    private static final double DEFAULT_GLOBAL_MULT = 1.0;

    private static final int CREW_PRODUCTION = 3;
    private static final int MARINES_PRODUCTION = 2;

    protected abstract String getBaseGrowthFieldId();
    protected abstract int getDefaultBaseGrowth();
    protected abstract String getIndustryName();

    /**
     * Alpha:  base × (1 + size × log10(size) × scalingFactor) × globalMult
     * Omega:  same formula but scalingFactor is doubled
     * Others: flat base × globalMult
     */
    protected int computeGrowthWeight() {
        int size = market.getSize();
        int base = getIntSetting(getBaseGrowthFieldId(), getDefaultBaseGrowth());
        double globalMult = getDoubleSetting("globalMult", DEFAULT_GLOBAL_MULT);
        String coreId = getAICoreId();

        if (Commodities.ALPHA_CORE.equals(coreId) || OMEGA_CORE.equals(coreId)) {
            double scaling = getDoubleSetting("scalingFactor", DEFAULT_SCALING_FACTOR);
            if (OMEGA_CORE.equals(coreId)) scaling *= 2.0;
            double sizeComponent = size * Math.log10(Math.max(size, 1));
            return (int) Math.round(base * (1.0 + sizeComponent * scaling) * globalMult);
        }
        return (int) Math.round(base * globalMult);
    }

    private static int getIntSetting(String fieldId, int defaultValue) {
        Integer val = LunaSettings.getInt(MOD_ID, fieldId);
        return val != null ? val : defaultValue;
    }

    private static double getDoubleSetting(String fieldId, double defaultValue) {
        Double val = LunaSettings.getDouble(MOD_ID, fieldId);
        return val != null ? val : defaultValue;
    }

    // --- AI Core overrides ---

    /**
     * Gamma: -25% upkeep
     * Beta:  -10% upkeep
     * Alpha: -25% upkeep
     * Omega: -50% upkeep
     */
    @Override
    protected void applyAICoreToIncomeAndUpkeep() {
        String coreId = getAICoreId();
        if (coreId == null) {
            getUpkeep().unmodifyMult("ind_core");
            return;
        }
        if (Commodities.GAMMA_CORE.equals(coreId)) {
            getUpkeep().modifyMult("ind_core", 0.75f, "Gamma Core assigned");
        } else if (Commodities.BETA_CORE.equals(coreId)) {
            getUpkeep().modifyMult("ind_core", 0.9f, "Beta Core assigned");
        } else if (Commodities.ALPHA_CORE.equals(coreId)) {
            getUpkeep().modifyMult("ind_core", 0.75f, "Alpha Core assigned");
        } else if (OMEGA_CORE.equals(coreId)) {
            getUpkeep().modifyMult("ind_core", 0.5f, "Omega Core assigned");
        }
    }

    // Suppress vanilla +1 supply / -1 demand defaults — we use our own effects instead
    @Override protected void applyAlphaCoreSupplyAndDemandModifiers() {}
    @Override protected void applyBetaCoreSupplyAndDemandModifiers() {}
    @Override protected void applyGammaCoreSupplyAndDemandModifiers() {}

    // --- Industry lifecycle ---

    @Override
    public void apply() {
        super.apply(true);

        if (!isFunctional()) return;

        String coreId = getAICoreId();
        if (Commodities.BETA_CORE.equals(coreId)
                || Commodities.ALPHA_CORE.equals(coreId)
                || OMEGA_CORE.equals(coreId)) {
            supply(Commodities.CREW, CREW_PRODUCTION);
            supply(Commodities.MARINES, MARINES_PRODUCTION);
        }

        market.addImmigrationModifier(this);
        market.getStability().modifyFlat(getModId(), -market.getSize(), getIndustryName());
    }

    @Override
    public void unapply() {
        market.removeImmigrationModifier(this);
        market.getStability().unmodify(getModId());
        super.unapply();
    }

    @Override
    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        int weight = computeGrowthWeight();
        incoming.add(Factions.NEUTRAL, weight * 10f);
        incoming.getWeight().modifyFlat(getModId(), weight, getIndustryName());
    }

    @Override
    public boolean showWhenUnavailable() {
        return true;
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        float opad = 10f;
        float pad = 3f;
        int size = market.getSize();
        int weight = computeGrowthWeight();
        String coreId = getAICoreId();

        tooltip.addSectionHeading("Effects",
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                Alignment.MID, opad);

        tooltip.addPara(String.format("Population growth: +%d points (scales with colony size)", weight),
                Misc.getPositiveHighlightColor(), opad);
        tooltip.addPara("Stability: -" + size + " (scales with colony size)",
                Misc.getNegativeHighlightColor(), pad);

        if (Commodities.BETA_CORE.equals(coreId)
                || Commodities.ALPHA_CORE.equals(coreId)
                || OMEGA_CORE.equals(coreId)) {
            tooltip.addPara(
                    String.format("Produces: %d crew, %d marines (AI core)", CREW_PRODUCTION, MARINES_PRODUCTION),
                    Misc.getPositiveHighlightColor(), pad);
        }
    }
}
