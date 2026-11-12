package oddisz.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import oddisz.cloning.CloningUnlock;

/**
 * Shared parts of the branch buildings that Clone Vat Experiments can upgrade into
 * (Organ Flashcloning, Marine Sim-Cloning): unlock check, AI core tiers and upkeep (same
 * numbers as the main cloning chain), and AI core text with the core's icon.
 */
public abstract class BaseCloningBranch extends BaseIndustry {

    public static final String OMEGA_CORE = "omega_core";
    public static final String BIOFACTORY_EMBRYO = "biofactory_embryo";
    public static final String PRISTINE_NANOFORGE = "pristine_nanoforge";
    public static final String CRYOARITHMETIC_ENGINE = "cryoarithmetic_engine";

    public static final int CORE_NONE = 0, CORE_GAMMA = 1, CORE_BETA = 2, CORE_ALPHA = 3, CORE_OMEGA = 4;

    protected int getCoreTier() {
        return coreTier(getAICoreId());
    }

    protected static int coreTier(String coreId) {
        if (coreId == null) return CORE_NONE;
        if (Commodities.GAMMA_CORE.equals(coreId)) return CORE_GAMMA;
        if (Commodities.BETA_CORE.equals(coreId)) return CORE_BETA;
        if (Commodities.ALPHA_CORE.equals(coreId)) return CORE_ALPHA;
        if (OMEGA_CORE.equals(coreId)) return CORE_OMEGA;
        return CORE_NONE;
    }

    protected boolean hasItem(String id) {
        SpecialItemData item = getSpecialItem();
        return item != null && id.equals(item.getId());
    }

    /** Text for an AI core of this tier, without the "Reduces upkeep" part. May use %s with {@link #coreHighlights}. */
    protected abstract String getCoreEffectText(int tier);

    protected abstract String[] coreHighlights(int tier);

    // --- Unlock ---

    @Override
    public boolean isAvailableToBuild() {
        return CloningUnlock.isUnlocked();
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    public String getUnavailableReason() {
        if (!CloningUnlock.isUnlocked()) return "Requires cloning research";
        return super.getUnavailableReason();
    }

    // --- AI cores: same upkeep cut as the cloning chain, no vanilla supply/demand change ---

    @Override
    protected void applyAICoreToIncomeAndUpkeep() {
        int tier = getCoreTier();
        if (tier == CORE_NONE) {
            getUpkeep().unmodifyMult("ind_core");
            return;
        }
        getUpkeep().modifyMult("ind_core", 1f - upkeepCut(tier), coreName(tier) + " assigned");
    }

    protected static float upkeepCut(int tier) {
        switch (tier) {
            case CORE_GAMMA: return 0.25f;
            case CORE_BETA: return 0.10f;
            case CORE_ALPHA: return 0.25f;
            case CORE_OMEGA: return 0.50f;
            default: return 0f;
        }
    }

    protected static String coreName(int tier) {
        switch (tier) {
            case CORE_GAMMA: return "Gamma Core";
            case CORE_BETA: return "Beta Core";
            case CORE_ALPHA: return "Alpha Core";
            case CORE_OMEGA: return "Omega Core";
            default: return "";
        }
    }

    @Override protected void applyAlphaCoreSupplyAndDemandModifiers() {}
    @Override protected void applyBetaCoreSupplyAndDemandModifiers() {}
    @Override protected void applyGammaCoreSupplyAndDemandModifiers() {}

    @Override
    public void addAICoreSection(TooltipMakerAPI tooltip, String coreId, AICoreDescriptionMode mode) {
        int tier = coreTier(coreId);
        if (tier == CORE_NONE) {
            super.addAICoreSection(tooltip, coreId, mode);
            return;
        }
        float opad = 10f;
        String level = tier == CORE_GAMMA ? "Gamma" : tier == CORE_BETA ? "Beta" : tier == CORE_ALPHA ? "Alpha" : "Omega";
        String pre = mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST
                ? level + "-level AI core. " : level + "-level AI core currently assigned. ";
        String[] extra = coreHighlights(tier);
        String[] hl = new String[1 + (extra == null ? 0 : extra.length)];
        hl[0] = Math.round(upkeepCut(tier) * 100f) + "%";
        for (int i = 1; i < hl.length; i++) hl[i] = extra[i - 1];
        String desc = pre + "Reduces upkeep cost by %s. " + getCoreEffectText(tier);

        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(coreId);
            TooltipMakerAPI text = tooltip.beginImageWithText(spec.getIconName(), 48);
            text.addPara(desc, 0f, Misc.getHighlightColor(), hl);
            tooltip.addImageWithText(opad);
        } else {
            tooltip.addPara(desc, opad, Misc.getHighlightColor(), hl);
        }
    }

    // --- Story point improvement: effects are added in apply() by each building ---

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    protected void applyImproveModifiers() {
    }
}
