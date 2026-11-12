package oddisz.cloning;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.util.Misc;

/**
 * "Cloning facilities" at a player colony with Marine Sim-Cloning: clone an officer. Opened from rules.csv by CLN_CloningFacilities.
 */
public class CloningFacilitiesDialog implements InteractionDialogPlugin {

    private static final String OPT_OFFICERS = "cln_officers";
    private static final String OPT_PICK = "cln_pick:";
    private static final String OPT_CONFIRM = "cln_confirm";
    private static final String OPT_MAIN = "cln_main";
    private static final String OPT_LEAVE = "cln_leave";

    private final Map<String, MemoryAPI> memoryMap;
    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private MarketAPI market;
    private PersonAPI picked;

    public CloningFacilitiesDialog(Map<String, MemoryAPI> memoryMap) {
        this.memoryMap = memoryMap;
    }

    public static boolean isAvailable(MarketAPI market) {
        if (market == null || !market.isPlayerOwned()) return false;
        return market.hasIndustry(CloningPaths.MARINES);
    }

    public static void open(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CloningFacilitiesDialog plugin = new CloningFacilitiesDialog(memoryMap);
        dialog.setPlugin(plugin);
        plugin.init(dialog);
    }

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        this.market = dialog.getInteractionTarget() != null ? dialog.getInteractionTarget().getMarket() : null;
        showMain();
    }

    private CampaignFleetAPI fleet() {
        return Global.getSector().getPlayerFleet();
    }

    private void showMain() {
        options.clearOptions();
        picked = null;
        if (market == null) {
            options.addOption("Leave", OPT_LEAVE);
            return;
        }

        if (market.hasIndustry(CloningPaths.MARINES)) {
            options.addOption("Clone an officer", OPT_OFFICERS);
            if (OfficerCloning.getCloneable(fleet()).isEmpty()) {
                options.setEnabled(OPT_OFFICERS, false);
                options.setTooltip(OPT_OFFICERS, "No officer in your fleet can be cloned.");
            }
        }
        options.addOption("Back", OPT_LEAVE);
    }

    private void showOfficers() {
        options.clearOptions();
        CampaignFleetAPI fleet = fleet();
        text.addPara("Clones keep the original's portrait, personality, level and skills. "
                + "Elite skills become normal ones, and every clone has the Clone skill.");
        text.addPara("Each clone costs credits, more for higher levels, and %s harvested organs from your cargo.",
                Misc.getHighlightColor(), "" + OfficerCloning.getOrganCost());
        if (!OfficerCloning.hasRoomForOfficer(fleet)) {
            text.addPara("Your fleet has no room for another officer.", Misc.getNegativeHighlightColor());
        }
        List<PersonAPI> list = OfficerCloning.getCloneable(fleet);
        int shown = 0;
        for (PersonAPI p : list) {
            if (shown >= 12) break;
            String opt = OPT_PICK + p.getId();
            options.addOption(p.getNameString() + ", level " + p.getStats().getLevel() + " ("
                    + Misc.getDGSCredits(OfficerCloning.getCreditCost(p)) + ")", opt);
            String why = OfficerCloning.whyNot(fleet, p);
            if (why != null) {
                options.setEnabled(opt, false);
                options.setTooltip(opt, why + ".");
            }
            shown++;
        }
        options.addOption("Back", OPT_MAIN);
    }

    private void showConfirm(PersonAPI p) {
        options.clearOptions();
        picked = p;
        dialog.getVisualPanel().showPersonInfo(p, true);
        text.addPara("Clone %s for %s and %s harvested organs?", Misc.getHighlightColor(),
                p.getNameString(), Misc.getDGSCredits(OfficerCloning.getCreditCost(p)),
                "" + OfficerCloning.getOrganCost());
        List<String> lost = OfficerCloning.eliteSkillsLost(p);
        if (!lost.isEmpty()) {
            text.addPara("No longer elite on the clone: " + Misc.getAndJoined(lost) + ".", Misc.getGrayColor());
        }
        options.addOption("Grow the clone", OPT_CONFIRM);
        options.addOption("Back", OPT_OFFICERS);
    }

    public void optionSelected(String optionText, Object optionData) {
        if (optionText != null) text.addParagraph(optionText, Global.getSettings().getColor("buttonText"));
        String opt = optionData instanceof String ? (String) optionData : "";

        if (OPT_OFFICERS.equals(opt)) {
            dialog.getVisualPanel().fadeVisualOut();
            showOfficers();
        } else if (opt.startsWith(OPT_PICK)) {
            String id = opt.substring(OPT_PICK.length());
            for (PersonAPI p : OfficerCloning.getCloneable(fleet())) {
                if (p.getId().equals(id)) {
                    showConfirm(p);
                    return;
                }
            }
            showOfficers();
        } else if (OPT_CONFIRM.equals(opt) && picked != null) {
            PersonAPI clone = OfficerCloning.cloneOfficer(fleet(), picked);
            if (clone != null) {
                dialog.getVisualPanel().showPersonInfo(clone, true);
                text.setFontSmallInsignia();
                text.addPara("Officer joined your fleet: " + clone.getNameString(), Misc.getPositiveHighlightColor());
                text.addPara("Lost " + Misc.getDGSCredits(OfficerCloning.getCreditCost(picked)) + " and "
                        + OfficerCloning.getOrganCost() + " harvested organs", Misc.getNegativeHighlightColor());
                text.setFontInsignia();
                Global.getSoundPlayer().playUISound("ui_cargo_special_tech_drop", 1f, 1f);
            } else {
                text.addPara(OfficerCloning.whyNot(fleet(), picked) + ".", Misc.getNegativeHighlightColor());
            }
            picked = null;
            showMain();
        } else if (OPT_MAIN.equals(opt)) {
            dialog.getVisualPanel().fadeVisualOut();
            showMain();
        } else if (OPT_LEAVE.equals(opt)) {
            backToPort();
        }
    }

    private void backToPort() {
        RuleBasedInteractionDialogPluginImpl port = new RuleBasedInteractionDialogPluginImpl();
        dialog.setPlugin(port);
        port.init(dialog);
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
        return memoryMap;
    }
}
