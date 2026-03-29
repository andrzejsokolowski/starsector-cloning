package oddisz.industries;

public class Cloning extends BaseCloning {

    @Override
    protected String getBaseGrowthFieldId() {
        return "baseGrowthTier2";
    }

    @Override
    protected int getDefaultBaseGrowth() {
        return 125;
    }

    @Override
    protected String getIndustryName() {
        return "Cloning";
    }

    @Override
    public boolean isAvailableToBuild() {
        return true;
    }
}
