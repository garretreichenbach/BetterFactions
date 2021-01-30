package dovtech.betterfactions.gui.faction;

import dovtech.betterfactions.data.faction.Federation;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.schine.input.InputState;

/**
 * NewFactionPanel.java
 * Improved verion of FactionPanelNew
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class NewFactionPanel extends FactionPanelNew {

    private InputState inputState;

    private FactionNewsTab factionNewsTab;
    private FactionDiplomacyTab factionDiplomacyTab;
    private FactionManagementTab factionManagementTab;
    private FederationDiplomacyTab federationDiplomacyTab;
    private FederationManagementTab federationManagementTab;
    private FactionOptionsTab optionsTab;

    public NewFactionPanel(InputState inputState) {
        super(inputState);
        this.inputState = inputState;
    }

    private Federation getFederation() {
        //Todo
        return null;
    }

    private boolean isInFederation() {
        return getFederation() != null;
    }

    private boolean isInFaction() {
        return getOwnFaction() != null;
    }

    @Override
    public void recreateTabs() {
        Object beforeTab = null;
        if (factionPanel.getSelectedTab() < factionPanel.getTabs().size()) {
            beforeTab = factionPanel.getTabs().get(factionPanel.getSelectedTab()).getTabName();
        }
        factionPanel.clearTabs();

        (factionNewsTab = new FactionNewsTab(inputState, factionPanel, this)).onInit();
        factionPanel.getTabs().add(factionNewsTab);

        (factionDiplomacyTab = new FactionDiplomacyTab(inputState, factionPanel, this)).onInit();
        factionPanel.getTabs().add(factionDiplomacyTab);

        if(isInFaction()) {
            (factionManagementTab = new FactionManagementTab(inputState, factionPanel, this)).onInit();
            factionPanel.getTabs().add(factionManagementTab);

            (federationDiplomacyTab = new FederationDiplomacyTab(inputState, factionPanel, this)).onInit();
            factionPanel.getTabs().add(federationDiplomacyTab);

            if(isInFederation()) {
                (federationManagementTab = new FederationManagementTab(inputState, factionPanel, this)).onInit();
                factionPanel.getTabs().add(federationManagementTab);
            }
        }

        factionPanel.activeInterface = this;
        if (beforeTab != null) {
            for (int i = 0; i < factionPanel.getTabs().size(); i++) {
                if (factionPanel.getTabs().get(i).getTabName().equals(beforeTab)) {
                    factionPanel.setSelectedTab(i);
                    break;
                }
            }
        }
    }
}
