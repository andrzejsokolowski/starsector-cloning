package oddisz.industries;

public class CloningMegafactory extends BaseCloning {

    @Override
    protected String getBaseGrowthFieldId() {
        return "baseGrowthTier3";
    }

    @Override
    protected int getDefaultBaseGrowth() {
        return 250;
    }

    @Override
    protected String getIndustryName() {
        return "Mass Cloning Megafactory";
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }
}
