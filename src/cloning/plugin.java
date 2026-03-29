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
import oddisz.industries.BaseCloning;
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
    private static final String MOD_ID = "oddisz_cloning";

    @Override
    public void onApplicationLoad() throws Exception {
        String suffix = ", " + String.join(", ", CLONING_INDUSTRY_IDS);
        for (String itemId : new String[]{ BIOFACTORY_EMBRYO, PRISTINE_NANOFORGE }) {
            SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(itemId);
            if (spec != null) {
                spec.setParams(spec.getParams() + suffix);
            }
        }

        wrapEffect(BIOFACTORY_EMBRYO);
        wrapEffect(PRISTINE_NANOFORGE);

        if (!LunaSettings.hasSettingsListenerOfClass(ResetListener.class)) {
            LunaSettings.addSettingsListener(new ResetListener());
        }
    }

    private static class ResetListener implements LunaSettingsListener {
        @Override
        public void settingsChanged(String modId) {
            if (!MOD_ID.equals(modId)) return;
            Boolean reset = LunaSettings.getBoolean(MOD_ID, "resetToDefault");
            if (reset == null || !reset) return;

            try {
                Map<String, JSONUtils.CommonDataJSONObject> settingsMap = LunaSettingsLoader.getSettings();
                JSONUtils.CommonDataJSONObject json = settingsMap.get(MOD_ID);
                if (json != null) {
                    json.put("baseGrowthTier1", 50);
                    json.put("baseGrowthTier2", 125);
                    json.put("baseGrowthTier3", 250);
                    json.put("scalingFactor", 0.15);
                    json.put("globalMult", 1.0);
                    json.put("resetToDefault", false);
                    json.save();
                }
            } catch (Exception e) {
                Global.getLogger(ResetListener.class).warn("Failed to reset cloning settings", e);
            }
            LunaSettings.reportSettingsChanged(MOD_ID);
        }
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

        @Override
        public void apply(Industry industry) {
            vanilla.apply(industry);
        }

        @Override
        public void unapply(Industry industry) {
            vanilla.unapply(industry);
        }

        @Override
        public void addItemDescription(Industry industry, TooltipMakerAPI tooltip,
                                       SpecialItemData data,
                                       InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode) {
            if (!(industry instanceof BaseCloning)) {
                vanilla.addItemDescription(industry, tooltip, data, mode);
                return;
            }
            float opad = 10f;
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
