package oddisz.industries;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;

public abstract class BaseCloning extends BaseIndustry implements MarketImmigrationModifier {

    private static final String MOD_ID = "cloning";

    private static final double DEFAULT_SCALING_FACTOR = 0.15;
    private static final double DEFAULT_GLOBAL_MULT = 1.0;

    /**
     * The LunaSettings fieldID for this tier's base growth value.
     * e.g. "baseGrowthTier1", "baseGrowthTier2", "baseGrowthTier3"
     */
    protected abstract String getBaseGrowthFieldId();

    protected abstract int getDefaultBaseGrowth();

    protected abstract String getIndustryName();

    /**
     * Final Growth = basePopGrowthPerTier
     *              × (1 + (size × log10(size) × scalingFactor))
     *              × globalMult
     */
    protected int computeGrowthWeight() {
        int size = market.getSize();
        int base = getIntSetting(getBaseGrowthFieldId(), getDefaultBaseGrowth());
        double scaling = getDoubleSetting("scalingFactor", DEFAULT_SCALING_FACTOR);
        double globalMult = getDoubleSetting("globalMult", DEFAULT_GLOBAL_MULT);
        double sizeComponent = size * Math.log10(Math.max(size, 1));
        return (int) Math.round(base * (1.0 + sizeComponent * scaling) * globalMult);
    }

    private static int getIntSetting(String fieldId, int defaultValue) {
        Integer val = LunaSettings.getInt(MOD_ID, fieldId);
        return val != null ? val : defaultValue;
    }

    private static double getDoubleSetting(String fieldId, double defaultValue) {
        Double val = LunaSettings.getDouble(MOD_ID, fieldId);
        return val != null ? val : defaultValue;
    }

    @Override
    public void apply() {
        super.apply(true);

        if (!isFunctional()) return;

        market.addImmigrationModifier(this);

        // Stability penalty scales with colony size
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
        float weight = computeGrowthWeight();
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
        float weight = computeGrowthWeight();

        tooltip.addSectionHeading("Effects",
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                Alignment.MID, opad);

        tooltip.addPara(String.format("Population growth: +%.0f points (scales with colony size)", weight),
                Misc.getPositiveHighlightColor(), opad);
        tooltip.addPara("Stability: -" + size + " (scales with colony size)",
                Misc.getNegativeHighlightColor(), pad);
    }
}
