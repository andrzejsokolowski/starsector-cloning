package oddisz.industries;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import oddisz.cloning.CloningPaths;

public class CloneVatExperiments extends BaseCloning {

    @Override
    protected String getBaseGrowthFieldId() {
        return "baseGrowthTier1";
    }

    @Override
    protected int getDefaultBaseGrowth() {
        return 50;
    }

    @Override
    protected String getIndustryName() {
        return "Clone Vat Experiments";
    }

    @Override
    public boolean isAvailableToBuild() {
        return oddisz.cloning.CloningUnlock.isUnlocked();
    }

    // --- Branching upgrade: the colony's chosen path (Cloning, Organ Flashcloning, Marine Sim-Cloning) ---

    @Override
    public boolean canUpgrade() {
        CloningPaths.syncUpgrade(market);
        return super.canUpgrade();
    }

    @Override
    public void startUpgrading() {
        CloningPaths.syncUpgrade(market);
        super.startUpgrading();
    }

    @Override
    public void createTooltip(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {
        CloningPaths.syncUpgrade(market);
        super.createTooltip(mode, tooltip, expanded);
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        super.addRightAfterDescriptionSection(tooltip, mode);
        if (market == null || mode == IndustryTooltipMode.ADD_INDUSTRY) return;
        tooltip.addPara("Upgrade path: %s (set at the colony's Cloning facilities)", 3f,
                Misc.getHighlightColor(), CloningPaths.name(CloningPaths.get(market)));
    }
}
