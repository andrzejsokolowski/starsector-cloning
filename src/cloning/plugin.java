package cloning;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import oddisz.cloning.CloningSettings;
import oddisz.industries.BaseCloning;
import oddisz.industries.BaseCloningBranch;
import oddisz.industries.MarineSimCloning;
import oddisz.industries.OrganFlashcloning;
import org.lazywizard.lazylib.JSONUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class plugin extends BaseModPlugin {

    private static final String[] CLONING_INDUSTRY_IDS = {
        "oddisz_clone_vat_experiments",
        "oddisz_cloning",
        "oddisz_cloning_megafactory"
    };

    private static final String BIOFACTORY_EMBRYO = "biofactory_embryo";
    private static final String PRISTINE_NANOFORGE = "pristine_nanoforge";
    private static final String CRYOARITHMETIC_ENGINE = "cryoarithmetic_engine";
    private static final String MOD_ID = "oddisz_cloning";

    @Override
    public void onApplicationLoad() throws Exception {
        String chain = String.join(", ", CLONING_INDUSTRY_IDS);
        addInstallableIn(BIOFACTORY_EMBRYO, chain + ", " + OrganFlashcloning.ID + ", " + MarineSimCloning.ID);
        addInstallableIn(PRISTINE_NANOFORGE, chain + ", " + OrganFlashcloning.ID);
        addInstallableIn(CRYOARITHMETIC_ENGINE, MarineSimCloning.ID);

        wrapEffect(BIOFACTORY_EMBRYO);
        wrapEffect(PRISTINE_NANOFORGE);
        wrapEffect(CRYOARITHMETIC_ENGINE);

        if (!LunaSettings.hasSettingsListenerOfClass(ResetListener.class)) {
            LunaSettings.addSettingsListener(new ResetListener());
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        oddisz.cloning.CloneVatTracker.register();
        oddisz.cloning.CloningUpgradeOptions.register();
        oddisz.cloning.CloningUnlock.unlockIfAlreadyBuilt();
        oddisz.cloning.CloningUnlock.unlockIfMissionOff();
    }

    private static class ResetListener implements LunaSettingsListener {
        @Override
        public void settingsChanged(String modId) {
            if (!MOD_ID.equals(modId)) return;
            oddisz.cloning.CloningUnlock.unlockIfMissionOff();
            Boolean reset = LunaSettings.getBoolean(MOD_ID, "resetToDefault");
            if (reset == null || !reset) return;

            try {
                Map<String, JSONUtils.CommonDataJSONObject> settingsMap = LunaSettingsLoader.getSettings();
                JSONUtils.CommonDataJSONObject json = settingsMap.get(MOD_ID);
                if (json != null) {
                    // Every default comes from this mod's LunaSettings.csv, so new settings reset too.
                    org.json.JSONArray rows = Global.getSettings().loadCSV("data/config/LunaSettings.csv", MOD_ID);
                    for (int i = 0; i < rows.length(); i++) {
                        org.json.JSONObject row = rows.getJSONObject(i);
                        if (!MOD_ID.equals(row.optString("modID"))) continue;
                        String field = row.optString("fieldID");
                        String type = row.optString("fieldType");
                        String def = row.optString("defaultValue");
                        if (field.isEmpty() || def.isEmpty()) continue;
                        if ("Int".equals(type)) json.put(field, Integer.parseInt(def.trim()));
                        else if ("Double".equals(type)) json.put(field, Double.parseDouble(def.trim()));
                        else if ("Boolean".equals(type)) json.put(field, Boolean.parseBoolean(def.trim()));
                    }
                    json.put("resetToDefault", false);
                    json.save();
                }
            } catch (Exception e) {
                Global.getLogger(ResetListener.class).warn("Failed to reset cloning settings", e);
            }
            LunaSettings.reportSettingsChanged(MOD_ID);
        }
    }

    /** Adds buildings to the list a colony item can be installed in. */
    private static void addInstallableIn(String itemId, String ids) {
        SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(itemId);
        if (spec == null) return;
        String params = spec.getParams();
        spec.setParams(params == null || params.trim().isEmpty() ? ids : params + ", " + ids);
    }

    private static void wrapEffect(String itemId) {
        InstallableItemEffect vanilla = ItemEffectsRepo.ITEM_EFFECTS.get(itemId);
        if (vanilla == null) return;
        ItemEffectsRepo.ITEM_EFFECTS.put(itemId, new CloningItemEffectWrapper(itemId, vanilla));
    }

    private static class CloningItemEffectWrapper implements InstallableItemEffect {

        private final String itemId;
        private final InstallableItemEffect vanilla;

        CloningItemEffectWrapper(String itemId, InstallableItemEffect vanilla) {
            this.itemId = itemId;
            this.vanilla = vanilla;
        }

        // The branch buildings read their items themselves; the vanilla effect (fleet size, farming output) stays off there.
        @Override
        public void apply(Industry industry) {
            if (industry instanceof BaseCloningBranch) return;
            vanilla.apply(industry);
        }

        @Override
        public void unapply(Industry industry) {
            if (industry instanceof BaseCloningBranch) return;
            vanilla.unapply(industry);
        }

        @Override
        public void addItemDescription(Industry industry, TooltipMakerAPI tooltip,
                                       SpecialItemData data,
                                       InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode) {
            float opad = 10f;
            if (industry instanceof OrganFlashcloning) {
                String organs = BIOFACTORY_EMBRYO.equals(itemId) ? "organEmbryoOrgans" : "organNanoforgeOrgans";
                String stab = BIOFACTORY_EMBRYO.equals(itemId) ? "organEmbryoStability" : "organNanoforgeStability";
                int dOrgans = BIOFACTORY_EMBRYO.equals(itemId) ? 2 : 3;
                tooltip.addPara("Harvested organs %s, stability %s.", opad, Misc.getHighlightColor(),
                        "+" + CloningSettings.getInt(organs, dOrgans), "+" + CloningSettings.getInt(stab, 1));
                return;
            }
            if (industry instanceof MarineSimCloning) {
                if (BIOFACTORY_EMBRYO.equals(itemId)) {
                    tooltip.addPara("Marines %s.", opad, Misc.getHighlightColor(),
                            "+" + CloningSettings.getInt("marineEmbryoBonus", 50) + "%");
                } else {
                    tooltip.addPara("Marine experience %s on hot worlds and %s on worlds with extreme heat.", opad,
                            Misc.getHighlightColor(),
                            "+" + CloningSettings.getInt("marineCryoHotExtraXP", 40),
                            "+" + CloningSettings.getInt("marineCryoVeryHotExtraXP", 75));
                }
                return;
            }
            if (!(industry instanceof BaseCloning)) {
                vanilla.addItemDescription(industry, tooltip, data, mode);
                return;
            }
            if (BIOFACTORY_EMBRYO.equals(itemId)) {
                tooltip.addPara("Increases base population growth by %s.", opad, Misc.getHighlightColor(), "25%");
            } else if (PRISTINE_NANOFORGE.equals(itemId)) {
                tooltip.addPara("Growth from cloning now scales with colony size. Scaling factor: %s.", opad, Misc.getHighlightColor(), "+0.10");
            }
        }

        @Override
        public List<String> getUnmetRequirements(Industry industry) {
            return vanilla.getUnmetRequirements(industry);
        }

        @Override
        public List<String> getUnmetRequirements(Industry industry, boolean checkChain) {
            return vanilla.getUnmetRequirements(industry, checkChain);
        }

        @Override
        public List<String> getRequirements(Industry industry) {
            return vanilla.getRequirements(industry);
        }

        @Override
        public Set<String> getConditionsRelatedToRequirements(Industry industry) {
            return vanilla.getConditionsRelatedToRequirements(industry);
        }
    }
}
