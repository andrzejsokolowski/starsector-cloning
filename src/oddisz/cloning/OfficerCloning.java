package oddisz.cloning;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.Misc;

/**
 * Officer cloning at a colony with Marine Sim-Cloning. The clone is "Clone of <name>": same
 * portrait, personality, level and skills, but elite skills drop to normal (unless LunaLib says
 * otherwise) and it carries the Clone skill. Costs credits and harvested organs from the fleet.
 */
public final class OfficerCloning {

    public static final String CLONE_SKILL = "oddisz_clone";
    public static final String CLONE_FLAG = "$oddisz_officer_clone";

    private OfficerCloning() {}

    public static int getCreditCost(PersonAPI officer) {
        int base = CloningSettings.getInt("officerCloneCredits", 25000);
        int perLevel = CloningSettings.getInt("officerCloneCreditsPerLevel", 10000);
        return Math.max(0, base + perLevel * officer.getStats().getLevel());
    }

    public static int getOrganCost() {
        return Math.max(0, CloningSettings.getInt("officerCloneOrgans", 10));
    }

    public static boolean isClone(PersonAPI p) {
        return p != null && (p.getMemoryWithoutUpdate().getBoolean(CLONE_FLAG)
                || p.getStats().getSkillLevel(CLONE_SKILL) > 0);
    }

    /** Officers that can be cloned: not AI cores, not the player, not clones themselves. */
    public static List<PersonAPI> getCloneable(CampaignFleetAPI fleet) {
        List<PersonAPI> result = new ArrayList<PersonAPI>();
        if (fleet == null) return result;
        for (OfficerDataAPI od : fleet.getFleetData().getOfficersCopy()) {
            PersonAPI p = od.getPerson();
            if (p == null || p.isPlayer() || p.isAICore() || isClone(p)) continue;
            result.add(p);
        }
        return result;
    }

    public static boolean hasRoomForOfficer(CampaignFleetAPI fleet) {
        return Misc.getNumNonMercOfficers(fleet) < Misc.getMaxOfficers(fleet);
    }

    /** Null if it can be done, otherwise the reason. */
    public static String whyNot(CampaignFleetAPI fleet, PersonAPI officer) {
        if (!hasRoomForOfficer(fleet)) return "No room for another officer";
        float credits = fleet.getCargo().getCredits().get();
        if (credits < getCreditCost(officer)) return "Not enough credits";
        if (fleet.getCargo().getCommodityQuantity(Commodities.ORGANS) < getOrganCost()) {
            return "Not enough harvested organs in your cargo";
        }
        return null;
    }

    /** Pays and adds the clone to the fleet. Returns the clone, or null if it could not be done. */
    public static PersonAPI cloneOfficer(CampaignFleetAPI fleet, PersonAPI src) {
        if (whyNot(fleet, src) != null) return null;
        fleet.getCargo().getCredits().subtract(getCreditCost(src));
        if (getOrganCost() > 0) fleet.getCargo().removeCommodity(Commodities.ORGANS, getOrganCost());

        PersonAPI clone = makeClone(src);
        fleet.getFleetData().addOfficer(clone);
        return clone;
    }

    public static PersonAPI makeClone(PersonAPI src) {
        boolean keepElite = CloningSettings.getBool("officerCloneKeepElite", false);

        PersonAPI clone = Global.getFactory().createPerson();
        clone.setFaction(Factions.PLAYER);
        clone.setGender(src.getGender());
        FullName name = src.getName();
        clone.setName(new FullName("Clone of " + name.getFirst(), name.getLast(), src.getGender()));
        clone.setPortraitSprite(src.getPortraitSprite());
        clone.setRankId(Ranks.SPACE_LIEUTENANT);
        clone.setPostId(Ranks.POST_OFFICER);
        String personality = src.getPersonalityAPI() != null ? src.getPersonalityAPI().getId() : Personalities.STEADY;
        clone.setPersonality(personality);

        MutableCharacterStatsAPI from = src.getStats();
        MutableCharacterStatsAPI to = clone.getStats();
        int level = from.getLevel();
        to.setLevel(level);
        for (MutableCharacterStatsAPI.SkillLevelAPI sl : from.getSkillsCopy()) {
            float lv = sl.getLevel();
            if (lv <= 0) continue;
            if (!keepElite && lv > 1f) lv = 1f;
            to.setSkillLevel(sl.getSkill().getId(), lv);
        }
        to.setSkillLevel(CLONE_SKILL, 1f);

        try {
            OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
            to.setXP(plugin.getXPForLevel(level));
        } catch (Throwable ignored) {
            // XP only matters for the next level-up
        }
        clone.getMemoryWithoutUpdate().set(CLONE_FLAG, true);
        return clone;
    }

    /** Names of the skills that would drop from elite to normal. */
    public static List<String> eliteSkillsLost(PersonAPI src) {
        List<String> result = new ArrayList<String>();
        if (CloningSettings.getBool("officerCloneKeepElite", false)) return result;
        for (MutableCharacterStatsAPI.SkillLevelAPI sl : src.getStats().getSkillsCopy()) {
            if (sl.getLevel() > 1f) result.add(sl.getSkill().getName());
        }
        return result;
    }
}
