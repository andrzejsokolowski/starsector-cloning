package oddisz.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

/**
 * Tier 1 of cloning. It has no fixed upgrade in industries.csv (like VoK's Light Industry):
 * the three buildings whose downgrade is this one are its upgrades. Building Menu Overhaul
 * shows them under "Choose Upgrade"; without it, CloningUpgradeOptions adds the menu entries.
 */
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

    /** Starts upgrading into the given building, the same way vanilla's startUpgrading does. */
    public void startUpgradingTo(String industryId) {
        IndustrySpecAPI target = Global.getSettings().getIndustrySpec(industryId);
        if (target == null) return;
        building = true;
        buildProgress = 0;
        upgradeId = industryId;
        buildTime = target.getBuildTime();
    }

    /** The building this one is turning into, or null. */
    public String getUpgradeTargetId() {
        return isUpgrading() ? upgradeId : null;
    }
}
