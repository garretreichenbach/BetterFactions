package thederpgamer.betterfactions.gui;

import thederpgamer.betterfactions.game.faction.Federation;
import thederpgamer.betterfactions.gui.faction.FactionManagementTab;
import thederpgamer.betterfactions.gui.faction.FactionNewsTab;
import thederpgamer.betterfactions.gui.faction.FactionOptionsTab;
import thederpgamer.betterfactions.gui.faction.diplomacy.FactionDiplomacyTab;
import thederpgamer.betterfactions.gui.federation.FederationDiplomacyTab;
import thederpgamer.betterfactions.gui.federation.FederationManagementTab;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;
import java.util.Objects;

/**
 * NewFactionPanel.java
 * Improved version of FactionPanelNew
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

    public Federation getFederation() {
        if(getOwnFaction() != null) {
            return FederationUtils.getFederation(Objects.requireNonNull(FactionUtils.getFactionData(getOwnFaction())));
        } else {
            return null;
        }
    }

    public boolean isInFederation() {
        return getFederation() != null;
    }

    public boolean isInFaction() {
        return getOwnFaction() != null;
    }

    @Override
    public void recreateTabs() {
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

        (optionsTab = new FactionOptionsTab(inputState, factionPanel, this)).onInit();
        factionPanel.getTabs().add(optionsTab);

        factionPanel.activeInterface = this;
    }
}
