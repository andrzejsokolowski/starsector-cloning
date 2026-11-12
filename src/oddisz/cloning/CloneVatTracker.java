package oddisz.cloning;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import oddisz.hullmods.CloneVat;

/**
 * Campaign side of the Clone Vat hull mod. Added as a transient script and listener on every load.
 *
 * Battle refund: the game reports a player engagement before it takes the crew losses, so the
 * crew count at that moment is the count before the battle. Once the battle dialog has closed
 * (crew from disabled ships recovered, over-capacity crew removed), the difference is what the
 * fleet lost, and the Clone Vats give back their share of it.
 *
 * Daily cloning: every built-in Clone Vat turns food into crew once a day.
 */
public class CloneVatTracker extends BaseCampaignEventListener implements EveryFrameScript {

    private boolean pending = false;
    private int crewBefore = 0;
    private float restoreFraction = 0f;
    private float days = 0f;

    public CloneVatTracker() {
        super(false);
    }

    public static void register() {
        if (Global.getSector().hasTransientScript(CloneVatTracker.class)) return;
        CloneVatTracker tracker = new CloneVatTracker();
        Global.getSector().addTransientScript(tracker);
        Global.getSector().addTransientListener(tracker);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (pending) return; // several engagements in one encounter: keep the first count
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        float fraction = CloneVat.getFleetRestoreFraction(fleet);
        if (fraction <= 0f) return;
        pending = true;
        crewBefore = fleet.getCargo().getTotalCrew();
        restoreFraction = fraction;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;

        if (pending && Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null
                && fleet.getBattle() == null) {
            pending = false;
            refundBattleLosses(fleet);
        }

        days += Global.getSector().getClock().convertToDays(amount);
        int ticks = 0;
        while (days >= 1f && ticks < 5) {
            days -= 1f;
            ticks++;
            cloneDaily(fleet);
        }
        if (days >= 1f) days = 0f;
    }

    private static int roomForCrew(CargoAPI cargo) {
        return Math.max(0, (int) cargo.getMaxPersonnel() - cargo.getTotalPersonnel());
    }

    private void refundBattleLosses(CampaignFleetAPI fleet) {
        CargoAPI cargo = fleet.getCargo();
        int lost = crewBefore - cargo.getTotalCrew();
        if (lost <= 0) return;
        int refund = Math.round(lost * restoreFraction);
        fleet.getFleetData().updateCargoCapacities();
        refund = Math.min(refund, roomForCrew(cargo));
        if (refund <= 0) return;
        cargo.addCrew(refund);
        String num = Misc.getWithDGS(refund);
        Global.getSector().getCampaignUI().addMessage(
                "Clone Vats restored " + num + " crew", Misc.getTextColor(),
                num, null, Misc.getHighlightColor(), null);
    }

    private static void cloneDaily(CampaignFleetAPI fleet) {
        int wanted = 0;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (m.isFighterWing() || !CloneVat.isBuiltIn(m)) continue;
            wanted += CloneVat.getDailyCrew(m.getHullSpec().getHullSize());
        }
        if (wanted <= 0) return;

        CargoAPI cargo = fleet.getCargo();
        int food = (int) cargo.getCommodityQuantity(Commodities.FOOD);
        int foodPerCrew = CloneVat.getFoodPerCrew();
        int crew = foodPerCrew > 0 ? Math.min(wanted, food / foodPerCrew) : wanted;
        crew = Math.min(crew, roomForCrew(cargo));
        if (crew <= 0) return;

        if (foodPerCrew > 0) cargo.removeCommodity(Commodities.FOOD, crew * foodPerCrew);
        cargo.addCrew(crew);
    }
}
