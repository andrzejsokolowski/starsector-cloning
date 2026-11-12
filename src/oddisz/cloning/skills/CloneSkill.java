package oddisz.cloning.skills;

import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import oddisz.cloning.CloningSettings;

/** The "Clone" skill every cloned officer carries: a dimwitted pilot. Hidden from level-up picks (npc_only). */
public class CloneSkill {

    public static int maneuverPenalty() {
        return Math.max(0, Math.min(90, CloningSettings.getInt("cloneSkillManeuver", 15)));
    }

    public static int autofirePenalty() {
        return Math.max(0, Math.min(100, CloningSettings.getInt("cloneSkillAutofire", 25)));
    }

    public static class Level1 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
            float p = -maneuverPenalty();
            stats.getAcceleration().modifyPercent(id, p);
            stats.getDeceleration().modifyPercent(id, p);
            stats.getTurnAcceleration().modifyPercent(id, p);
            stats.getMaxTurnRate().modifyPercent(id, p);
        }

        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "-" + maneuverPenalty() + "% maneuverability";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class Level2 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
            stats.getAutofireAimAccuracy().modifyFlat(id, -autofirePenalty() * 0.01f);
        }

        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getAutofireAimAccuracy().unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "-" + autofirePenalty() + "% autofire accuracy";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }
}
