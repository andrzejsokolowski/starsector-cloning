package oddisz.industries;

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
        return true;
    }
}
