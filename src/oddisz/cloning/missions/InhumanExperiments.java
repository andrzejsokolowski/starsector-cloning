package oddisz.cloning.missions;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerAction;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerActionContext;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import oddisz.cloning.CloningUnlock;

/**
 * "Inhuman Experiments": a bar rumor sends the player to a system where a Remnant Ordo waits.
 * Its flagship is captained by a clone of the player, its next ships by clones of the player's
 * officers (same names with "Clone of", portraits, levels, skills and personalities).
 * Beating the Ordo unlocks the cloning colony buildings and teaches the Clone Vat hull mod.
 *
 * The fleet stays in the system until it is beaten. Declining just lets the offer come back
 * at some later bar visit; the offer stops once cloning is unlocked.
 */
public class InhumanExperiments extends HubMissionWithBarEvent {

    public static final String REF = "$clnie_ref";
    public static final String WON_FLAG = "$clnie_won";
    public static final String DEFEAT_TRIGGER = "ClnIEOrdoDefeated";
    public static final String CLONE_FLAG = "$clnie_clone";

    public static enum Stage {
        DEFEAT,
        COMPLETED,
    }

    protected StarSystemAPI system;
    protected CampaignFleetAPI ordo;
    protected transient boolean refreshedThisVisit = false;

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        if (CloningUnlock.isUnlocked()) return false;

        if (barEvent) {
            setGiverRank(Ranks.CITIZEN);
            setGiverPost(pickOne(Ranks.POST_DOCTOR, Ranks.POST_SCIENTIST, Ranks.POST_ACADEMICIAN));
            setGiverImportance(pickImportance());
            findOrCreateGiver(createdAt, false, false);
        }

        PersonAPI person = getPerson();
        if (person == null) return false;

        if (!setGlobalReference(REF)) return false;
        if (!setPersonMissionRef(person, REF)) return false;

        requireSystemInterestingAndNotUnsafeOrCore();
        requireSystemNotHasPulsar();
        preferSystemTags(ReqMode.ANY, Tags.THEME_REMNANT);
        preferSystemUnexplored();
        preferSystemInDirectionOfOtherMissions();
        system = pickSystem();
        if (system == null) return false;

        setName("Inhuman Experiments");
        setStartingStage(Stage.DEFEAT);
        setSuccessStage(Stage.COMPLETED);
        setStageOnGlobalFlag(Stage.COMPLETED, WON_FLAG);
        setRepFactionChangesNone();
        setRepPersonChangesNone();

        beginStageTrigger(Stage.DEFEAT);
        triggerCustomAction(new CreateCloneOrdoAction(system, genRandom.nextLong()));
        triggerPickLocationAroundEntity(system.getCenter(), 2500f);
        triggerSpawnFleetAtPickedLocation();
        triggerOrderFleetPatrol(system, true, Tags.PLANET, Tags.JUMP_POINT);
        triggerFleetMakeImportant(null, Stage.DEFEAT);
        triggerFleetAddDefeatTrigger(DEFEAT_TRIGGER);
        endTrigger();

        return true;
    }

    /** Builds a normal Remnant Ordo, then puts the clones in command. */
    public static class CreateCloneOrdoAction implements TriggerAction {
        protected StarSystemAPI system;
        protected long seed;

        public CreateCloneOrdoAction(StarSystemAPI system, long seed) {
            this.system = system;
            this.seed = seed;
        }

        public void doAction(TriggerActionContext context) {
            Random random = new Random(seed);
            // Same size as an Ordo from a working Remnant Nexus (vanilla: 8-24 points, Ordo above 16).
            int points = (20 + random.nextInt(5)) * 8;
            FleetParamsV3 params = new FleetParamsV3(
                    null,
                    system.getLocation(),
                    Factions.REMNANTS,
                    1f,
                    FleetTypes.PATROL_LARGE,
                    points,
                    0f, 0f, 0f, 0f, 0f, 0f);
            params.random = random;

            CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
            RemnantSeededFleetManager.initRemnantFleetProperties(random, fleet, false);
            fleet.setFacing(random.nextFloat() * 360f);
            fleet.getMemoryWithoutUpdate().set("$clnie_ordo", true);

            applyClones(fleet);

            context.fleet = fleet;
            context.allFleets.add(fleet);
            if (!fleet.hasScriptOfClass(MissionFleetAutoDespawn.class)) {
                fleet.addScript(new MissionFleetAutoDespawn(context.mission, fleet));
            }
            if (context.mission instanceof InhumanExperiments) {
                ((InhumanExperiments) context.mission).ordo = fleet;
            }
        }
    }

    /** Copies name, portrait, level, skills and personality onto a new Remnant captain. */
    public static PersonAPI makeClone(PersonAPI source, String personality) {
        PersonAPI clone = Global.getFactory().createPerson();
        clone.setFaction(Factions.REMNANTS);
        clone.setGender(source.getGender());
        FullName name = source.getName();
        clone.setName(new FullName("Clone of " + name.getFirst(), name.getLast(), source.getGender()));
        clone.setPortraitSprite(source.getPortraitSprite());
        clone.setRankId(Ranks.SPACE_CAPTAIN);
        clone.setPostId(Ranks.POST_OFFICER);
        clone.setPersonality(personality);

        MutableCharacterStatsAPI from = source.getStats();
        MutableCharacterStatsAPI to = clone.getStats();
        to.setLevel(from.getLevel());
        for (MutableCharacterStatsAPI.SkillLevelAPI sl : from.getSkillsCopy()) {
            if (sl.getLevel() <= 0) continue;
            to.setSkillLevel(sl.getSkill().getId(), sl.getLevel());
        }
        clone.getMemoryWithoutUpdate().set(CLONE_FLAG, true);
        return clone;
    }

    /** The largest ship gets the player's clone and becomes the flagship; the next biggest get the officers' clones. */
    public static void applyClones(CampaignFleetAPI fleet) {
        if (fleet == null || !fleet.isAlive()) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        PersonAPI playerPerson = Global.getSector().getPlayerPerson();
        if (playerPerson == null) return;

        FleetDataAPI data = fleet.getFleetData();
        List<FleetMemberAPI> ships = new ArrayList<FleetMemberAPI>();
        for (FleetMemberAPI m : data.getMembersListCopy()) {
            if (m.isFighterWing()) continue;
            ships.add(m);
        }
        if (ships.isEmpty()) return;
        Collections.sort(ships, new Comparator<FleetMemberAPI>() {
            public int compare(FleetMemberAPI a, FleetMemberAPI b) {
                int size = b.getHullSpec().getHullSize().compareTo(a.getHullSpec().getHullSize());
                if (size != 0) return size;
                return Float.compare(b.getFleetPointCost(), a.getFleetPointCost());
            }
        });

        // The player's clone always flies the largest ship, which becomes the flagship.
        FleetMemberAPI flagship = ships.get(0);
        ships.remove(flagship);

        PersonAPI commander = makeClone(playerPerson, Personalities.AGGRESSIVE);
        commander.setPostId(Ranks.POST_FLEET_COMMANDER);
        flagship.setCaptain(commander);
        fleet.setCommander(commander);
        for (FleetMemberAPI m : data.getMembersListCopy()) {
            m.setFlagship(m == flagship);
        }

        List<PersonAPI> officers = new ArrayList<PersonAPI>();
        if (player != null) {
            for (OfficerDataAPI od : player.getFleetData().getOfficersCopy()) {
                PersonAPI p = od.getPerson();
                if (p == null || p.isAICore() || p.isPlayer()) continue;
                officers.add(p);
            }
        }
        for (int i = 0; i < officers.size() && i < ships.size(); i++) {
            PersonAPI src = officers.get(i);
            String personality = src.getPersonalityAPI() != null ? src.getPersonalityAPI().getId() : Personalities.STEADY;
            ships.get(i).setCaptain(makeClone(src, personality));
        }

        data.setSyncNeeded();
        data.syncIfNeeded();
        for (FleetMemberAPI m : data.getMembersListCopy()) {
            m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        }
    }

    @Override
    protected void advanceImpl(float amount) {
        super.advanceImpl(amount);
        // The clones copy the player as they are when the player arrives, not only when the job was taken.
        if (ordo == null || !ordo.isAlive() || currentStage != Stage.DEFEAT) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        boolean here = player.getContainingLocation() == ordo.getContainingLocation();
        if (here && !refreshedThisVisit && ordo.getBattle() == null) {
            applyClones(ordo);
            refreshedThisVisit = true;
        } else if (!here) {
            refreshedThisVisit = false;
        }
    }

    @Override
    protected void updateInteractionDataImpl() {
        set("$clnie_barEvent", isBarEvent());
        set("$clnie_manOrWoman", getPerson().getManOrWoman());
        set("$clnie_systemName", system.getNameWithLowercaseTypeShort());
        set("$clnie_systemNameLong", system.getNameWithLowercaseType());
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map, Object stage) {
        if (ordo != null && ordo.isAlive() && currentStage == Stage.DEFEAT) return ordo;
        if (system != null) return system.getCenter();
        return super.getMapLocation(map, stage);
    }

    @Override
    protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CloningUnlock.unlock();
        CloneDiscoveryDialog.queue();
    }

    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.DEFEAT) {
            info.addPara("Investigate rumors of cloned people in the %s.", opad, h,
                    system.getNameWithLowercaseTypeShort());
        }
    }

    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.DEFEAT) {
            info.addPara("Investigate the %s", pad, tc, h, system.getNameWithLowercaseTypeShort());
            return true;
        }
        return false;
    }

    @Override
    public String getStageDescriptionText() {
        if (currentStage == Stage.COMPLETED) {
            return "You found the clones and destroyed their fleet. The cloning research they carried is yours.";
        }
        return null;
    }

    @Override
    public String getBaseName() {
        return "Inhuman Experiments";
    }
}
