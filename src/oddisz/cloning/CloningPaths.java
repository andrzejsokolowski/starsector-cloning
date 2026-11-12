package oddisz.cloning;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

/**
 * Clone Vat Experiments can upgrade into one of three buildings. The game only knows one upgrade
 * per building type, so the colony remembers its choice and the building points the shared
 * upgrade at it right before the game reads it.
 */
public final class CloningPaths {

    public static final String TIER1 = "oddisz_clone_vat_experiments";
    public static final String CLONING = "oddisz_cloning";
    public static final String ORGANS = "oddisz_organ_flashcloning";
    public static final String MARINES = "oddisz_marine_cloning";
    public static final String[] ALL = {CLONING, ORGANS, MARINES};

    private static final String KEY = "$oddisz_cloning_path";

    private CloningPaths() {}

    public static String get(MarketAPI market) {
        if (market == null) return CLONING;
        String v = market.getMemoryWithoutUpdate().getString(KEY);
        for (String id : ALL) {
            if (id.equals(v)) return id;
        }
        return CLONING;
    }

    public static void set(MarketAPI market, String id) {
        if (market == null) return;
        market.getMemoryWithoutUpdate().set(KEY, id);
    }

    public static String name(String id) {
        IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(id);
        return spec != null ? spec.getName() : id;
    }

    /** Points the shared Clone Vat Experiments upgrade at this colony's choice. */
    public static void syncUpgrade(MarketAPI market) {
        IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(TIER1);
        if (spec == null) return;
        String path = get(market);
        if (!path.equals(spec.getUpgrade())) spec.setUpgrade(path);
    }
}
