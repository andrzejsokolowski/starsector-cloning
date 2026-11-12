package oddisz.cloning.missions;

import java.awt.Color;
import java.util.Map;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * The scene after the clone Ordo is beaten: the salvage crews find the cloning lab on its flagship.
 * Shown by {@link Opener} once the battle dialog has closed, so it is a dialog of its own.
 * The unlock itself already happened when the mission ended; this only tells the player.
 */
public class CloneDiscoveryDialog implements InteractionDialogPlugin {

    private static final String OPT_LOOK = "look";
    private static final String OPT_LEAVE = "leave";

    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;

    /** Waits until no other dialog or battle is on screen, then opens the scene once. */
    public static class Opener implements EveryFrameScript {
        private boolean done = false;

        public boolean isDone() {
            return done;
        }

        public boolean runWhilePaused() {
            return false;
        }

        public void advance(float amount) {
            if (done) return;
            if (Global.getCurrentState() != GameState.CAMPAIGN) return;
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player == null || player.getBattle() != null) return;
            CampaignUIAPI ui = Global.getSector().getCampaignUI();
            if (ui.getCurrentInteractionDialog() != null || ui.isShowingDialog() || ui.isShowingMenu()) return;
            if (ui.showInteractionDialog(new CloneDiscoveryDialog(), player)) {
                done = true;
            }
        }
    }

    public static void queue() {
        Global.getSector().addScript(new Opener());
    }

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        dialog.getVisualPanel().showImagePortion("illustrations", "cryosleeper_interior", 640, 400, 0, 0, 480, 300);

        text.addPara("\"Captain, we found something!\"");
        text.addPara("The salvage team's leader is out of breath on the comm. The clone flagship carried "
                + "a full lab behind its reactor: rows of growth vats, most of them empty, fed by a specialized "
                + "nanoforge. It does not print hull plating. It weaves living tissue, cell by cell. "
                + "A data core next to it is packed with medical logs, neural maps and growth cycles.");
        text.addPara("\"Some of these maps are yours, Captain. And the officers'. That's how they made them.\"");

        options.clearOptions();
        options.addOption("Let's see what they used", OPT_LOOK);
    }

    public void optionSelected(String optionText, Object optionData) {
        if (optionText != null) text.addParagraph(optionText, Global.getSettings().getColor("buttonText"));

        if (OPT_LOOK.equals(optionData)) {
            text.addPara("Your science team spends the next day pulling the data core apart. The nanoforge "
                    + "weaves biological matter the way a normal one weaves alloys, and the vats grow it "
                    + "into an adult body in weeks. The neural maps do the rest. The method copies over "
                    + "cleanly to colony-scale equipment, and a smaller version fits aboard a ship.");

            Color good = Misc.getPositiveHighlightColor();
            text.setFontSmallInsignia();
            text.addPara("Gained cloning colony buildings", good);
            text.addPara("Gained hull mod: Clone Vat", good);
            text.setFontInsignia();
            Global.getSoundPlayer().playUISound("ui_cargo_special_tech_drop", 1f, 1f);

            text.addPara("Your chief medical officer stares at the empty vats for a long moment.");
            text.addPara("\"So this lets us clone... people?\"");

            options.clearOptions();
            options.addOption("Leave", OPT_LEAVE);
            return;
        }

        if (OPT_LEAVE.equals(optionData)) {
            dialog.dismiss();
        }
    }

    public void optionMousedOver(String optionText, Object optionData) {
    }

    public void advance(float amount) {
    }

    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    public Object getContext() {
        return null;
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
