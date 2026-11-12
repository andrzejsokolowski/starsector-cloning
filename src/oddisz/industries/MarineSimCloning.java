package oddisz.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker.PersonnelAtEntity;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker.PersonnelRank;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import oddisz.cloning.CloningSettings;

/**
 * Marine Sim-Cloning: a branch upgrade of Clone Vat Experiments, one level. Every 30 days puts
 * trained marines into the colony's storage, with their experience recorded in vanilla's
 * personnel tracker so they keep it when taken aboard. Needs harvested organs like the other
 * cloning buildings. Officer cloning (port menu) is offered at colonies that have this building.
 */
public class MarineSimCloning extends BaseCloningBranch {

    public static final String ID = "oddisz_marine_cloning";
    public static final float DAYS_PER_BATCH = 30f;

    protected float daysSinceBatch = 0f;

    private static int s(String field, int def) {
        return CloningSettings.getInt(field, def);
    }

    protected int getOrganDemand() {
        return Math.max(0, market.getSize() - 2);
    }

    protected float getOrganSupplyFraction() {
        int demand = getOrganDemand();
        if (demand <= 0) return 1f;
        int missing = getMaxDeficit(Commodities.ORGANS).two;
        return Math.max(0f, Math.min(1f, (demand - missing) / (float) demand));
    }

    protected int coreBonusPercent(int tier) {
        switch (tier) {
            case CORE_GAMMA: return s("marineGammaBonus", 10);
            case CORE_BETA: return s("marineBetaBonus", 25);
            case CORE_ALPHA: return s("marineAlphaBonus", 50);
            case CORE_OMEGA: return s("marineOmegaBonus", 100);
            default: return 0;
        }
    }

    protected int coreXP(int tier) {
        if (tier == CORE_ALPHA) return s("marineAlphaXP", 10);
        if (tier == CORE_OMEGA) return s("marineOmegaXP", 25);
        return 0;
    }

    /** Marines per batch with every bonus and the organ supply. */
    public int getMarinesPerBatch() {
        int base = market.getSize() * s("marinesPerSize", 10);
        int pct = coreBonusPercent(getCoreTier());
        if (isImproved()) pct += s("marineImproveBonus", 25);
        if (hasItem(BIOFACTORY_EMBRYO)) pct += s("marineEmbryoBonus", 50);
        float total = base * (1f + pct / 100f) * getOrganSupplyFraction();
        return Math.max(0, Math.round(total));
    }

    /** Experience of new marines, 0..1 (1 is the most a marine can have). */
    public float getMarineXP() {
        int xp = s("marineBaseXP", 30);
        if (hasItem(CRYOARITHMETIC_ENGINE)) {
            if (market.hasCondition(Conditions.VERY_HOT)) xp = Math.max(xp, s("marineCryoVeryHotXP", 100));
            else if (market.hasCondition(Conditions.HOT)) xp = Math.max(xp, s("marineCryoHotXP", 65));
        }
        xp += coreXP(getCoreTier());
        return Math.max(0f, Math.min(1f, xp / 100f));
    }

    @Override
    public void apply() {
        super.apply(true);
        demand(Commodities.ORGANS, getOrganDemand());
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (!isFunctional() || market == null || !market.isPlayerOwned()) return;
        daysSinceBatch += Global.getSector().getClock().convertToDays(amount);
        if (daysSinceBatch < DAYS_PER_BATCH) return;
        daysSinceBatch -= DAYS_PER_BATCH;
        if (daysSinceBatch > DAYS_PER_BATCH) daysSinceBatch = 0f;
        deliverBatch();
    }

    protected void deliverBatch() {
        int count = getMarinesPerBatch();
        if (count <= 0) return;
        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (storage == null || storage.getCargo() == null) return;
        CargoAPI cargo = storage.getCargo();

        // Keep vanilla's record of the marines stored here in step, then add the new ones with their experience.
        PersonnelAtEntity record = PlayerFleetPersonnelTracker.getInstance()
                .getDroppedOffAt(Commodities.MARINES, market.getPrimaryEntity(), storage, true);
        record.data.numMayHaveChanged(cargo.getMarines(), true);
        cargo.addMarines(count);
        record.data.add(count);
        record.data.addXP(count * getMarineXP());

        String num = Misc.getWithDGS(count);
        Global.getSector().getCampaignUI().addMessage(
                num + " " + rankName() + " marines added to storage at " + market.getName(),
                Misc.getTextColor(), num, null, Misc.getHighlightColor(), null);
    }

    protected String rankName() {
        return PersonnelRank.getRankForXP(getMarineXP()).name.toLowerCase();
    }

    @Override
    public boolean wantsToUseSpecialItem(SpecialItemData data) {
        return BIOFACTORY_EMBRYO.equals(data.getId()) || CRYOARITHMETIC_ENGINE.equals(data.getId());
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        float opad = 10f, pad = 3f;
        tooltip.addSectionHeading("Effects", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, opad);
        String rank = PersonnelRank.getRankForXP(getMarineXP()).name;
        tooltip.addPara(String.format("Marines to storage: %d every 30 days (%s)", getMarinesPerBatch(), rank),
                Misc.getPositiveHighlightColor(), opad);
        if (isFunctional() && mode == IndustryTooltipMode.NORMAL) {
            int left = Math.max(1, Math.round(DAYS_PER_BATCH - daysSinceBatch));
            tooltip.addPara("Next batch in " + left + (left == 1 ? " day" : " days"), Misc.getGrayColor(), pad);
        }
        float organs = getOrganSupplyFraction();
        if (organs < 1f) {
            tooltip.addPara(String.format("Harvested organs shortage: output -%d%%", Math.round((1f - organs) * 100f)),
                    Misc.getNegativeHighlightColor(), pad);
        }
        tooltip.addPara("Officer cloning available at this colony", Misc.getHighlightColor(), pad);
    }

    @Override
    protected String getCoreEffectText(int tier) {
        int xp = coreXP(tier);
        return xp > 0 ? "Marines %s, experience %s." : "Marines %s.";
    }

    @Override
    protected String[] coreHighlights(int tier) {
        int xp = coreXP(tier);
        if (xp > 0) return new String[]{"+" + coreBonusPercent(tier) + "%", "+" + xp};
        return new String[]{"+" + coreBonusPercent(tier) + "%"};
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        info.addPara("Marines %s.", 0f, Misc.getHighlightColor(), "+" + s("marineImproveBonus", 25) + "%");
        info.addSpacer(10f);
        super.addImproveDesc(info, mode);
    }
}
