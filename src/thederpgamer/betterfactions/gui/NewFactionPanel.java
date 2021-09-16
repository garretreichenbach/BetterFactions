package thederpgamer.betterfactions.gui;

import api.common.GameClient;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.federation.FederationData;
import thederpgamer.betterfactions.gui.faction.diplomacy.FactionDiplomacyTab;
import thederpgamer.betterfactions.gui.faction.management.FactionManagementTab;
import thederpgamer.betterfactions.gui.faction.news.FactionNewsTab;
import thederpgamer.betterfactions.gui.federation.FederationManagementTab;
import thederpgamer.betterfactions.manager.FactionManager;
import thederpgamer.betterfactions.manager.FederationManager;

import java.util.Objects;

/**
 * Enhanced version of FactionPanelNew.
 *
 * @version 1.0 - [01/30/2021]
 * @author TheDerpGamer, Schema (original)
 */
public class NewFactionPanel extends FactionPanelNew {

    private final InputState inputState;

    public FactionNewsTab factionNewsTab;
    public FactionDiplomacyTab factionDiplomacyTab;
    public FactionManagementTab factionManagementTab;
    public FederationManagementTab federationManagementTab;

    public NewFactionPanel(InputState inputState) {
        super(inputState);
        this.inputState = inputState;
    }

    public FederationData getFederation() {
        if(getOwnFaction() != null) {
            return FederationManager.getFederation(Objects.requireNonNull(FactionManager.getFactionData(getOwnFaction())));
        } else {
            return null;
        }
    }

    public boolean isInFederation() {
        return isInFaction() && FactionManager.getFactionData(getOwnFaction()).getFederationId() != -1;
    }

    public boolean isInFaction() {
        return FactionManager.inFaction(GameClient.getClientPlayerState());
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    @Override
    public void recreateTabs() {
        int selectedTab = factionPanel.getSelectedTab();
        factionPanel.activeInterface = this;
        factionPanel.clearTabs();

        (factionNewsTab = new FactionNewsTab(inputState, factionPanel, this)).onInit();
        factionPanel.getTabs().add(factionNewsTab);

        (factionDiplomacyTab = new FactionDiplomacyTab(inputState, factionPanel)).onInit();
        factionPanel.getTabs().add(factionDiplomacyTab);

        if(isInFaction()) {
            (factionManagementTab = new FactionManagementTab(inputState, factionPanel, this)).onInit();
            factionPanel.getTabs().add(factionManagementTab);

            if(isInFederation()) {
                (federationManagementTab = new FederationManagementTab(inputState, factionPanel, this)).onInit();
                factionPanel.getTabs().add(federationManagementTab);
            }
        }
        if(selectedTab < factionPanel.getTabs().size()) factionPanel.setSelectedTab(selectedTab);
    }

    public void updateTabs() {
        if(factionNewsTab != null) factionNewsTab.updateTab();
        if(factionDiplomacyTab != null) factionDiplomacyTab.updateTab();
        if(factionManagementTab != null) factionManagementTab.updateTab();
        if(federationManagementTab != null) federationManagementTab.updateTab();
    }
}
