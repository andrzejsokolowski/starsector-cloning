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
 * Marine Sim-Cloning: a branch upgrade of Clone Vat Experiments, one level. Every 30 days (less
 * with AI cores) puts marines into the colony's storage. Without upgrades they are Regulars; the
 * improvement, Alpha/Omega cores and a Cryoarithmetic Engine add experience, which is recorded in
 * vanilla's personnel tracker so they keep it when taken aboard. Needs harvested organs like the other
 * cloning buildings. Officer cloning (port menu) is offered at colonies that have this building.
 */
public class MarineSimCloning extends BaseCloningBranch {

    public static final String ID = "oddisz_marine_cloning";
    public static final float BASE_DAYS_PER_BATCH = 30f;

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

    /** Batch time cut by an AI core of this tier, in percent. */
    protected int coreSpeedPercent(int tier) {
        switch (tier) {
            case CORE_GAMMA: return s("marineGammaFaster", 10);
            case CORE_BETA: return s("marineBetaFaster", 20);
            case CORE_ALPHA: return s("marineAlphaFaster", 33);
            case CORE_OMEGA: return s("marineOmegaFaster", 66);
            default: return 0;
        }
    }

    /** Days between batches with the installed AI core (30, Alpha 20, Omega 10 by default). */
    public float getDaysPerBatch() {
        int pct = Math.max(0, Math.min(95, coreSpeedPercent(getCoreTier())));
        return Math.max(1f, Math.round(BASE_DAYS_PER_BATCH * (1f - pct / 100f)));
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

    /**
     * Experience of new marines, 0..1 (1 is the most a marine can have). Starts at 0 (Regular);
     * only upgrades add to it: 25 is Experienced, 50 Veteran, 75 Elite.
     */
    public float getMarineXP() {
        int xp = 0;
        if (isImproved()) xp += s("marineImproveXP", 10);
        if (hasItem(CRYOARITHMETIC_ENGINE)) {
            if (market.hasCondition(Conditions.VERY_HOT)) xp += s("marineCryoVeryHotExtraXP", 75);
            else if (market.hasCondition(Conditions.HOT)) xp += s("marineCryoHotExtraXP", 40);
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
        float every = getDaysPerBatch();
        daysSinceBatch += Global.getSector().getClock().convertToDays(amount);
        if (daysSinceBatch < every) return;
        daysSinceBatch -= every;
        if (daysSinceBatch > every) daysSinceBatch = 0f;
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
        tooltip.addPara(String.format("Marines to storage: %d every %d days (%s)", getMarinesPerBatch(),
                Math.round(getDaysPerBatch()), rank), Misc.getPositiveHighlightColor(), opad);
        if (isFunctional() && mode == IndustryTooltipMode.NORMAL) {
            int left = Math.max(1, Math.round(getDaysPerBatch() - daysSinceBatch));
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
        return xp > 0 ? "Marines %s, experience %s, a batch every %s days." : "Marines %s, a batch every %s days.";
    }

    @Override
    protected String[] coreHighlights(int tier) {
        int xp = coreXP(tier);
        int pct = Math.max(0, Math.min(95, coreSpeedPercent(tier)));
        String days = "" + Math.max(1, Math.round(BASE_DAYS_PER_BATCH * (1f - pct / 100f)));
        if (xp > 0) return new String[]{"+" + coreBonusPercent(tier) + "%", "+" + xp, days};
        return new String[]{"+" + coreBonusPercent(tier) + "%", days};
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        info.addPara("Marines %s, experience %s.", 0f, Misc.getHighlightColor(),
                "+" + s("marineImproveBonus", 25) + "%", "+" + s("marineImproveXP", 10));
        info.addSpacer(10f);
        super.addImproveDesc(info, mode);
    }
}
