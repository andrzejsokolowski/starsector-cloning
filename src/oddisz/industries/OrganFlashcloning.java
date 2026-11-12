package oddisz.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import oddisz.cloning.CloningSettings;

/**
 * Organ Flashcloning: a branch upgrade of Clone Vat Experiments. Supplies harvested organs
 * (colony size + bonus) and adds stability. AI cores, the story point improvement, a
 * Biofactory Embryo or a Pristine Nanoforge raise both. Every number is a LunaLib setting.
 */
public class OrganFlashcloning extends BaseCloningBranch {

    public static final String ID = "oddisz_organ_flashcloning";

    private static int s(String field, int def) {
        return CloningSettings.getInt(field, def);
    }

    protected int coreOrgans(int tier) {
        switch (tier) {
            case CORE_GAMMA: return s("organGammaOrgans", 1);
            case CORE_BETA: return s("organBetaOrgans", 1);
            case CORE_ALPHA: return s("organAlphaOrgans", 2);
            case CORE_OMEGA: return s("organOmegaOrgans", 3);
            default: return 0;
        }
    }

    protected int coreStability(int tier) {
        switch (tier) {
            case CORE_BETA: return s("organBetaStability", 1);
            case CORE_ALPHA: return s("organAlphaStability", 1);
            case CORE_OMEGA: return s("organOmegaStability", 2);
            default: return 0;
        }
    }

    protected int itemOrgans() {
        if (hasItem(BIOFACTORY_EMBRYO)) return s("organEmbryoOrgans", 2);
        if (hasItem(PRISTINE_NANOFORGE)) return s("organNanoforgeOrgans", 3);
        return 0;
    }

    protected int itemStability() {
        if (hasItem(BIOFACTORY_EMBRYO)) return s("organEmbryoStability", 1);
        if (hasItem(PRISTINE_NANOFORGE)) return s("organNanoforgeStability", 1);
        return 0;
    }

    protected int getStabilityBonus() {
        int total = s("organBaseStability", 1) + coreStability(getCoreTier()) + itemStability();
        if (isImproved()) total += s("organImproveStability", 1);
        return Math.max(0, total);
    }

    @Override
    public void apply() {
        super.apply(true);

        int size = market.getSize();
        supply(0, Commodities.ORGANS, size + s("organBaseBonus", 1), BASE_VALUE_TEXT);
        int tier = getCoreTier();
        supply(1, Commodities.ORGANS, coreOrgans(tier), tier == CORE_NONE ? "AI core" : coreName(tier));
        supply(2, Commodities.ORGANS, isImproved() ? s("organImproveOrgans", 1) : 0, "Improvements");
        SpecialItemData item = getSpecialItem();
        supply(3, Commodities.ORGANS, itemOrgans(), item != null ? Global.getSettings().getSpecialItemSpec(item.getId()).getName() : "Colony item");

        if (!isFunctional()) {
            supply.clear();
            market.getStability().unmodifyFlat(getModId());
            return;
        }
        market.getStability().modifyFlat(getModId(), getStabilityBonus(), getCurrentName());
    }

    @Override
    public void unapply() {
        market.getStability().unmodifyFlat(getModId());
        super.unapply();
    }

    @Override
    public boolean wantsToUseSpecialItem(SpecialItemData data) {
        return BIOFACTORY_EMBRYO.equals(data.getId()) || PRISTINE_NANOFORGE.equals(data.getId());
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        float opad = 10f;
        tooltip.addSectionHeading("Effects", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, opad);
        tooltip.addPara("Stability: +" + getStabilityBonus(), Misc.getPositiveHighlightColor(), opad);
    }

    @Override
    protected String getCoreEffectText(int tier) {
        int st = coreStability(tier);
        return st > 0 ? "Harvested organs %s, stability %s." : "Harvested organs %s.";
    }

    @Override
    protected String[] coreHighlights(int tier) {
        int st = coreStability(tier);
        if (st > 0) return new String[]{"+" + coreOrgans(tier), "+" + st};
        return new String[]{"+" + coreOrgans(tier)};
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        info.addPara("Harvested organs %s, stability %s.", 0f, Misc.getHighlightColor(),
                "+" + s("organImproveOrgans", 1), "+" + s("organImproveStability", 1));
        info.addSpacer(10f);
        super.addImproveDesc(info, mode);
    }
}
