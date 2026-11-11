package oddisz.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.hullmods.BaseLogisticsHullMod;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Clone Vat (logistics).
 * Installed: after a battle, gives back a share of the crew the fleet lost. Every Clone Vat in the
 * fleet adds its share (10/25/50/100 percent by hull size), capped at 100 percent.
 * Built in (S-mod or part of the hull): every day turns food into crew, 2 food per crew,
 * 1/2/4/8 crew per ship by hull size, while there is food and room for crew.
 * The campaign side lives in oddisz.cloning.CloneVatTracker.
 */
public class CloneVat extends BaseLogisticsHullMod {

    public static final String ID = "oddisz_clone_vat";
    public static final int FOOD_PER_CREW = 2;

    public static float getRestoreFraction(HullSize size) {
        switch (size) {
            case FRIGATE: return 0.10f;
            case DESTROYER: return 0.25f;
            case CRUISER: return 0.50f;
            case CAPITAL_SHIP: return 1.00f;
            default: return 0f;
        }
    }

    public static int getDailyCrew(HullSize size) {
        switch (size) {
            case FRIGATE: return 1;
            case DESTROYER: return 2;
            case CRUISER: return 4;
            case CAPITAL_SHIP: return 8;
            default: return 0;
        }
    }

    public static boolean hasVat(FleetMemberAPI member) {
        ShipVariantAPI v = member.getVariant();
        return v != null && v.hasHullMod(ID);
    }

    /** Built in with a story point, or part of the hull itself. */
    public static boolean isBuiltIn(FleetMemberAPI member) {
        ShipVariantAPI v = member.getVariant();
        if (v == null || !v.hasHullMod(ID)) return false;
        if (v.getSMods().contains(ID)) return true;
        if (v.getSModdedBuiltIns().contains(ID)) return true;
        return v.getHullSpec() != null && v.getHullSpec().isBuiltInMod(ID);
    }

    /** Share of lost crew the whole fleet gives back after a battle, 0..1. */
    public static float getFleetRestoreFraction(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) return 0f;
        float total = 0f;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (m.isFighterWing() || !hasVat(m)) continue;
            total += getRestoreFraction(m.getHullSpec().getHullSize());
        }
        return Math.min(1f, total);
    }

    private static String pct(float f) {
        return Math.round(f * 100f) + "%";
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return pct(getRestoreFraction(HullSize.FRIGATE));
        if (index == 1) return pct(getRestoreFraction(HullSize.DESTROYER));
        if (index == 2) return pct(getRestoreFraction(HullSize.CRUISER));
        if (index == 3) return pct(getRestoreFraction(HullSize.CAPITAL_SHIP));
        if (index == 4) return "100%";
        return null;
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + getDailyCrew(HullSize.FRIGATE) * FOOD_PER_CREW;
        if (index == 1) return "" + getDailyCrew(HullSize.DESTROYER) * FOOD_PER_CREW;
        if (index == 2) return "" + getDailyCrew(HullSize.CRUISER) * FOOD_PER_CREW;
        if (index == 3) return "" + getDailyCrew(HullSize.CAPITAL_SHIP) * FOOD_PER_CREW;
        if (index == 4) return "" + getDailyCrew(HullSize.FRIGATE);
        if (index == 5) return "" + getDailyCrew(HullSize.DESTROYER);
        if (index == 6) return "" + getDailyCrew(HullSize.CRUISER);
        if (index == 7) return "" + getDailyCrew(HullSize.CAPITAL_SHIP);
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (isForModSpec || ship == null || !isInPlayerFleet(ship)) return;
        CampaignFleetAPI fleet = com.fs.starfarer.api.Global.getSector() != null
                ? com.fs.starfarer.api.Global.getSector().getPlayerFleet() : null;
        if (fleet == null) return;
        tooltip.addPara("Crew restored by this fleet: %s.", 10f, Misc.getHighlightColor(),
                pct(getFleetRestoreFraction(fleet)));
    }
}
