package oddisz.cloning;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

/** The three buildings Clone Vat Experiments can upgrade into (their downgrade is Clone Vat Experiments). */
public final class CloningPaths {

    public static final String TIER1 = "oddisz_clone_vat_experiments";
    public static final String CLONING = "oddisz_cloning";
    public static final String ORGANS = "oddisz_organ_flashcloning";
    public static final String MARINES = "oddisz_marine_cloning";
    public static final String[] ALL = {CLONING, ORGANS, MARINES};

    private CloningPaths() {}

    public static String name(String id) {
        IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(id);
        return spec != null ? spec.getName() : id;
    }
}
