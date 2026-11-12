package oddisz.cloning;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import oddisz.industries.CloneVatExperiments;

/**
 * Right-click menu entries on Clone Vat Experiments: "Upgrade to X..." for each of the three
 * cloning paths, and "Cancel upgrade..." while one is under way. The vanilla menu has no
 * branching upgrades and no cancel for them, so these replace it. Not added when Building Menu
 * Overhaul is enabled: its "Choose Upgrade" and cancel already cover the three paths.
 */
public class CloningUpgradeOptions extends BaseIndustryOptionProvider {

    public static final String BMO_ID = "bmo";
    private static final String CANCEL = "cln_cancel_upgrade";

    public static void register() {
        if (Global.getSector().getListenerManager().hasListenerOfClass(CloningUpgradeOptions.class)) return;
        Global.getSector().getListenerManager().addListener(new CloningUpgradeOptions(), true);
    }

    private static boolean bmoHandlesIt() {
        return Global.getSettings().getModManager().isModEnabled(BMO_ID);
    }

    private static int cost(Industry ind, String targetId) {
        Industry target = ind.getMarket().instantiateIndustry(targetId);
        return target == null ? 0 : (int) target.getBuildCost();
    }

    private static float credits() {
        return Global.getSector().getPlayerFleet().getCargo().getCredits().get();
    }

    @Override
    public List<IndustryOptionData> getIndustryOptions(Industry ind) {
        if (isUnsuitable(ind, true) || !(ind instanceof CloneVatExperiments)) return null;
        if (!ind.getMarket().isPlayerOwned() || bmoHandlesIt()) return null;
        CloneVatExperiments vat = (CloneVatExperiments) ind;
        List<IndustryOptionData> result = new ArrayList<IndustryOptionData>();

        if (vat.isUpgrading()) {
            result.add(new IndustryOptionData("Cancel upgrade...", CANCEL, ind, this));
            return result;
        }
        if (vat.isBuilding()) return null;

        for (String id : CloningPaths.ALL) {
            IndustryOptionData opt = new IndustryOptionData("Upgrade to " + CloningPaths.name(id) + "...", id, ind, this);
            Industry target = ind.getMarket().instantiateIndustry(id);
            opt.enabled = target != null && target.isAvailableToBuild() && credits() >= cost(ind, id);
            result.add(opt);
        }
        return result;
    }

    @Override
    public void createTooltip(IndustryOptionData opt, TooltipMakerAPI tooltip, float width) {
        if (CANCEL.equals(opt.id)) {
            String target = ((CloneVatExperiments) opt.ind).getUpgradeTargetId();
            if (target != null) {
                tooltip.addPara("Cancels the upgrade to %s and refunds %s.", 0f, Misc.getHighlightColor(),
                        CloningPaths.name(target), Misc.getDGSCredits(cost(opt.ind, target)));
            }
            return;
        }
        Industry target = opt.ind.getMarket().instantiateIndustry((String) opt.id);
        if (target != null) target.createTooltip(IndustryTooltipMode.UPGRADE, tooltip, false);
    }

    @Override
    public void optionSelected(final IndustryOptionData opt, DialogCreatorUI ui) {
        final CloneVatExperiments vat = (CloneVatExperiments) opt.ind;

        if (CANCEL.equals(opt.id)) {
            final String target = vat.getUpgradeTargetId();
            if (target == null) return;
            final int refund = cost(vat, target);
            ui.showDialog(600f, 90f, new BaseCustomDialogDelegate() {
                @Override
                public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
                    TooltipMakerAPI info = panel.createUIElement(600f, 90f, false);
                    info.setParaInsigniaLarge();
                    info.addPara("Cancel the upgrade to %s? You get %s back.", 0f, Misc.getHighlightColor(),
                            CloningPaths.name(target), Misc.getDGSCredits(refund));
                    panel.addUIElement(info).inTL(0, 0);
                }

                @Override
                public boolean hasCancelButton() {
                    return true;
                }

                @Override
                public String getConfirmText() {
                    return "Cancel upgrade";
                }

                @Override
                public String getCancelText() {
                    return "Never mind";
                }

                @Override
                public void customDialogConfirm() {
                    if (!vat.isUpgrading()) return;
                    vat.cancelUpgrade();
                    Global.getSector().getPlayerFleet().getCargo().getCredits().add(refund);
                    Misc.addCreditsMessage("Received %s", refund);
                    Global.getSoundPlayer().playUISound("ui_cancel_construction_or_upgrade_industry", 1f, 1f);
                }
            });
            return;
        }

        final String target = (String) opt.id;
        final int price = cost(vat, target);
        final Industry preview = vat.getMarket().instantiateIndustry(target);
        final int days = preview == null ? 0 : Math.round(preview.getBuildTime());
        ui.showDialog(600f, 90f, new BaseCustomDialogDelegate() {
            @Override
            public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
                TooltipMakerAPI info = panel.createUIElement(600f, 90f, false);
                info.setParaInsigniaLarge();
                info.addPara("Upgrade Clone Vat Experiments to %s for %s? It takes %s days.", 0f,
                        Misc.getHighlightColor(), CloningPaths.name(target), Misc.getDGSCredits(price), "" + days);
                panel.addUIElement(info).inTL(0, 0);
            }

            @Override
            public boolean hasCancelButton() {
                return true;
            }

            @Override
            public String getConfirmText() {
                return "Upgrade";
            }

            @Override
            public void customDialogConfirm() {
                MutableValue c = Global.getSector().getPlayerFleet().getCargo().getCredits();
                if (vat.isBuilding() || c.get() < price) return;
                c.subtract(price);
                vat.startUpgradingTo(target);
                Global.getSoundPlayer().playUISound("ui_upgrade_industry", 1f, 1f);
            }
        });
    }
}
