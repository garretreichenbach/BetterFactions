package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import thederpgamer.betterfactions.utils.FactionUtils;

/**
 * FactionDiplomacyTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionDiplomacyTab extends GUIContentPane {

    private Faction selectedFaction;
    private NewFactionPanel guiPanel;
    private FactionInfoPanel infoPanel;
    private FactionActionsPanel actionsPanel;
    private FactionListPanel listPanel;

    public FactionDiplomacyTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("FACTION DIPLOMACY"));
        this.guiPanel = guiPanel;
        this.selectedFaction = null;
    }

    @Override
    public void onInit() {
        super.onInit();
        setTextBoxHeightLast(270);
        addNewTextBox(0, 70);
        addDivider(224);

        (infoPanel = new FactionInfoPanel(getState())).onInit();
        getContent(0, 0).attach(infoPanel);

        (actionsPanel = new FactionActionsPanel(getState(), guiPanel)).onInit();
        getContent(0, 1).attach(actionsPanel);

        (listPanel = new FactionListPanel(getState())).onInit();
        getContent(1, 0).attach(listPanel);

        if (selectedFaction != null && FactionUtils.inFaction(GameClient.getClientPlayerState()) && FactionUtils.getFaction(GameClient.getClientPlayerState()).getIdFaction() != selectedFaction.getIdFaction()) {
            infoPanel.setFaction(selectedFaction);
            infoPanel.setNameText(selectedFaction.getName());
            FactionData factionData = FactionUtils.getFactionData(selectedFaction);
            if (factionData != null) {
                infoPanel.setInfoText(factionData.getInfoString());
                infoPanel.updateLogo(factionData.getFactionLogo());
            }
            actionsPanel.setFaction(selectedFaction);
        } else if (GameClient.getClientPlayerState().getFactionId() != 0) {
            Faction faction = GameCommon.getGameState().getFactionManager().getFaction(GameClient.getClientPlayerState().getFactionId());
            infoPanel.setFaction(faction);
            infoPanel.setNameText(faction.getName());
            FactionData factionData = FactionUtils.getFactionData(faction);
            if (factionData != null) {
                infoPanel.setInfoText(factionData.getInfoString());
                infoPanel.updateLogo(factionData.getFactionLogo());
            }
            actionsPanel.setFaction(faction);
        } else {
            infoPanel.setNameText("No Faction");
        }
    }

    public void setSelectedFaction(Faction selectedFaction) {
        this.selectedFaction = selectedFaction;
    }
}
