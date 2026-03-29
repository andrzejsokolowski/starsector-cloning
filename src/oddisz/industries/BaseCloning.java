package oddisz.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
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

    private static final String MOD_ID = "oddisz_cloning";
    private static final String OMEGA_CORE = "omega_core";

    private static final double DEFAULT_SCALING_FACTOR = 0.15;
    private static final double DEFAULT_GLOBAL_MULT = 1.0;

    protected abstract String getBaseGrowthFieldId();
    protected abstract int getDefaultBaseGrowth();
    protected abstract String getIndustryName();

    private boolean hasScaling() {
        String coreId = getAICoreId();
        return Commodities.ALPHA_CORE.equals(coreId) || OMEGA_CORE.equals(coreId);
    }

    private int computeCrewProduction() {
        return market.getSize();
    }

    private int computeMarinesProduction() {
        return Math.max(1, market.getSize() - 1);
    }

    /**
     * Alpha/Omega: base × (1 + size × log10(size) × scalingFactor) × globalMult
     * Omega doubles the scalingFactor.
     * Others: flat base × globalMult
     */
    protected int computeGrowthWeight() {
        int size = market.getSize();
        int base = getIntSetting(getBaseGrowthFieldId(), getDefaultBaseGrowth());
        double globalMult = getDoubleSetting("globalMult", DEFAULT_GLOBAL_MULT);

        if (hasScaling()) {
            double scaling = getDoubleSetting("scalingFactor", DEFAULT_SCALING_FACTOR);
            if (OMEGA_CORE.equals(getAICoreId())) scaling *= 2.0;
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

    // --- AI Core: upkeep ---

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

    // Suppress vanilla +1 supply / -1 demand defaults
    @Override protected void applyAlphaCoreSupplyAndDemandModifiers() {}
    @Override protected void applyBetaCoreSupplyAndDemandModifiers() {}
    @Override protected void applyGammaCoreSupplyAndDemandModifiers() {}

    // --- AI Core: descriptions ---

    @Override
    public void addAICoreSection(TooltipMakerAPI tooltip, String coreId, AICoreDescriptionMode mode) {
        if (OMEGA_CORE.equals(coreId)) {
            addOmegaCoreDescription(tooltip, mode);
        } else {
            super.addAICoreSection(tooltip, mode == null ? AICoreDescriptionMode.INDUSTRY_TOOLTIP : mode);
        }
    }

    @Override
    protected void addGammaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        float opad = 10f;
        String pre = mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST ? "Gamma-level AI core. "
                : "Gamma-level AI core currently assigned. ";
        String desc = pre + "Reduces upkeep cost by %s.";
        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.GAMMA_CORE);
            TooltipMakerAPI text = tooltip.beginImageWithText(spec.getIconName(), 48);
            text.addPara(desc, 0f, Misc.getHighlightColor(), "25%");
            tooltip.addImageWithText(opad);
        } else {
            tooltip.addPara(desc, opad, Misc.getHighlightColor(), "25%");
        }
    }

    @Override
    protected void addBetaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        float opad = 10f;
        String pre = mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST ? "Beta-level AI core. "
                : "Beta-level AI core currently assigned. ";
        String desc = pre + "Reduces upkeep cost by %s. Produces crew and marines.";
        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.BETA_CORE);
            TooltipMakerAPI text = tooltip.beginImageWithText(spec.getIconName(), 48);
            text.addPara(desc, 0f, Misc.getHighlightColor(), "10%");
            tooltip.addImageWithText(opad);
        } else {
            tooltip.addPara(desc, opad, Misc.getHighlightColor(), "10%");
        }
    }

    @Override
    protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        float opad = 10f;
        String pre = mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST ? "Alpha-level AI core. "
                : "Alpha-level AI core currently assigned. ";
        String desc = pre + "Reduces upkeep cost by %s. Population growth now scales with colony size. "
                + "Produces crew and marines.";
        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.ALPHA_CORE);
            TooltipMakerAPI text = tooltip.beginImageWithText(spec.getIconName(), 48);
            text.addPara(desc, 0f, Misc.getHighlightColor(), "25%");
            tooltip.addImageWithText(opad);
        } else {
            tooltip.addPara(desc, opad, Misc.getHighlightColor(), "25%");
        }
    }

    protected void addOmegaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        float opad = 10f;
        String pre = mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST ? "Omega-level AI core. "
                : "Omega-level AI core currently assigned. ";
        String desc = pre + "Reduces upkeep cost by %s. Population growth scales with colony size at double the rate. "
                + "Produces crew and marines.";
        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(OMEGA_CORE);
            TooltipMakerAPI text = tooltip.beginImageWithText(spec.getIconName(), 48);
            text.addPara(desc, 0f, Misc.getHighlightColor(), "50%");
            tooltip.addImageWithText(opad);
        } else {
            tooltip.addPara(desc, opad, Misc.getHighlightColor(), "50%");
        }
    }

    // --- Industry lifecycle ---

    @Override
    public void apply() {
        super.apply(true);

        if (!isFunctional()) return;

        String coreId = getAICoreId();
        if (Commodities.BETA_CORE.equals(coreId)
                || Commodities.ALPHA_CORE.equals(coreId)
                || OMEGA_CORE.equals(coreId)) {
            supply(Commodities.CREW, computeCrewProduction());
            supply(Commodities.MARINES, computeMarinesProduction());
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

        if (hasScaling()) {
            tooltip.addPara(String.format("Population growth: +%d points (scales with colony size)", weight),
                    Misc.getPositiveHighlightColor(), opad);
        } else {
            tooltip.addPara(String.format("Population growth: +%d points", weight),
                    Misc.getPositiveHighlightColor(), opad);
        }

        tooltip.addPara("Stability: -" + size + " (scales with colony size)",
                Misc.getNegativeHighlightColor(), pad);

        if (Commodities.BETA_CORE.equals(coreId)
                || Commodities.ALPHA_CORE.equals(coreId)
                || OMEGA_CORE.equals(coreId)) {
            tooltip.addPara(
                    String.format("Produces: %d crew, %d marines", computeCrewProduction(), computeMarinesProduction()),
                    Misc.getPositiveHighlightColor(), pad);
        }
    }
}
