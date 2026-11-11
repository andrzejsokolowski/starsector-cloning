package oddisz.cloning;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import oddisz.hullmods.CloneVat;

/** The campaign flag set by the Inhuman Experiments mission. */
public final class CloningUnlock {

    public static final String FLAG = "$oddisz_cloning_unlocked";

    public static final String[] INDUSTRY_IDS = {
        "oddisz_clone_vat_experiments",
        "oddisz_cloning",
        "oddisz_cloning_megafactory"
    };

    private CloningUnlock() {}

    public static boolean isUnlocked() {
        if (Global.getSector() == null) return true;
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        return mem.getBoolean(FLAG);
    }

    /** Unlocks the colony buildings and teaches the Clone Vat hull mod. */
    public static void unlock() {
        if (Global.getSector() == null) return;
        Global.getSector().getMemoryWithoutUpdate().set(FLAG, true);
        if (!Global.getSector().getCharacterData().knowsHullMod(CloneVat.ID)) {
            Global.getSector().getCharacterData().addHullMod(CloneVat.ID);
        }
    }

    /** Saves from before the unlock existed: a player colony that already has a cloning building keeps it all. */
    public static void unlockIfAlreadyBuilt() {
        if (Global.getSector() == null || isUnlocked()) return;
        for (MarketAPI market : Misc.getPlayerMarkets(true)) {
            for (String id : INDUSTRY_IDS) {
                if (market.hasIndustry(id)) {
                    unlock();
                    return;
                }
            }
        }
    }
}
