package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import oddisz.cloning.CloningFacilitiesDialog;

/**
 * rules.csv command for the "Cloning facilities" port option.
 * "CLN_CloningFacilities available" is the condition, "CLN_CloningFacilities open" opens the dialog.
 * Lives in this package because that is where the rules engine looks commands up by plain name.
 */
public class CLN_CloningFacilities extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getInteractionTarget() == null) return false;
        MarketAPI market = dialog.getInteractionTarget().getMarket();
        String action = params != null && !params.isEmpty() ? params.get(0).getString(memoryMap) : "available";

        if ("available".equals(action)) {
            return CloningFacilitiesDialog.isAvailable(market);
        }
        if ("open".equals(action)) {
            try {
                CloningFacilitiesDialog.open(dialog, memoryMap);
                return true;
            } catch (Exception e) {
                Global.getLogger(CLN_CloningFacilities.class).error("Cloning: could not open the facilities", e);
                return false;
            }
        }
        return false;
    }
}
