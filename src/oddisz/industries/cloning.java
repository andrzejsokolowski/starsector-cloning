package oddisz.industries;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class cloning extends BaseIndustry implements MarketImmigrationModifier {

    // Added to PopulationComposition weight each growth cycle.
    // CoreImmigrationPluginImpl.GROWTH_NO_INDUSTRIES is ~0.1, so 0.5 is a massive boost.
    private static final float GROWTH_WEIGHT_BONUS = 0.5f;

    @Override
    public void apply() {
        super.apply(false); // handles income/upkeep via BaseIndustry

        MarketAPI market = getMarket();
        int size = market.getSize();

        // Register this industry as a growth modifier
        market.addImmigrationModifier(this);

        // Stability penalty that scales with colony size —
        // more clones produced means more violence and disease
        market.getStability().modifyFlat(getModId(), -size, "Cloning Industry");
    }

    @Override
    public void unapply() {
        super.unapply();

        MarketAPI market = getMarket();
        market.removeImmigrationModifier(this);
        market.getStability().unmodify(getModId());
    }

    // Called each growth cycle — boost incoming population weight
    @Override
    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        incoming.getWeight().modifyFlat(getModId(), GROWTH_WEIGHT_BONUS, "Cloning Industry");
    }

    @Override
    public boolean isAvailableToBuild() {
        return true;
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        float opad = 10f;
        float pad = 3f;
        int size = getMarket().getSize();

        tooltip.addSectionHeading("Effects",
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                Alignment.MID, opad);

        tooltip.addPara("Population growth: massively increased", Misc.getPositiveHighlightColor(), opad);
        tooltip.addPara("Stability: -" + size + " (scales with colony size)",
                Misc.getNegativeHighlightColor(), pad);
    }
}
