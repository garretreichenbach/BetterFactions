package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import api.common.GameCommon;
import org.newdawn.slick.Color;
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
    private FactionDiplomacyList factionList;

    public FactionDiplomacyTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("DIPLOMACY"));
        this.guiPanel = guiPanel;
        this.selectedFaction = null;
    }

    @Override
    public void onInit() {
        super.onInit();
        setTextBoxHeightLast(270);
        addNewTextBox(0, 80);
        addDivider(250);

        (infoPanel = new FactionInfoPanel(getState(), getContent(0, 0))).onInit();
        (actionsPanel = new FactionActionsPanel(getState(), getContent(0, 1))).onInit();
        (factionList = new FactionDiplomacyList(getState(), getContent(1, 0), this)).onInit();

        if(selectedFaction != null && FactionUtils.inFaction(GameClient.getClientPlayerState()) && FactionUtils.getFaction(GameClient.getClientPlayerState()).getIdFaction() != selectedFaction.getIdFaction()) {
            infoPanel.setFaction(selectedFaction);
            String relation = Lng.str(FactionUtils.getFactionData(selectedFaction).getRelationString());
            if(relation.equals(Lng.str("Own Faction")) || relation.equals(Lng.str("Allied")) || relation.equals(Lng.str("In Federation"))) {
                infoPanel.setNameText(selectedFaction.getName(), Color.green);
            } else if(relation.equals(Lng.str("Neutral"))) {
                infoPanel.setNameText(selectedFaction.getName(), Color.blue);
            } else if(relation.equals(Lng.str("At War")) || relation.equals(Lng.str("Personal Enemy"))) {
                infoPanel.setNameText(selectedFaction.getName(), Color.red);
            } else {
                infoPanel.setNameText(selectedFaction.getName());
            }
            FactionData factionData = FactionUtils.getFactionData(selectedFaction);
            if(factionData != null) {
                infoPanel.setInfoText(factionData.getInfoString());
                infoPanel.updateLogo(factionData.getFactionLogo());
            }
            actionsPanel.setFaction(selectedFaction);
        } else if(GameClient.getClientPlayerState().getFactionId() != 0) {
            Faction faction = GameCommon.getGameState().getFactionManager().getFaction(GameClient.getClientPlayerState().getFactionId());
            infoPanel.setFaction(faction);
            infoPanel.setNameText(faction.getName(), Color.green);
            FactionData factionData = FactionUtils.getFactionData(faction);
            if(factionData != null) {
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
        infoPanel.setFaction(selectedFaction);
        infoPanel.setNameText(selectedFaction.getName());
        FactionData factionData = FactionUtils.getFactionData(selectedFaction);
        if(factionData != null) {
            infoPanel.setInfoText(factionData.getInfoString());
            infoPanel.updateLogo(factionData.getFactionLogo());
        }
        actionsPanel.setFaction(selectedFaction);
        actionsPanel.onInit();
    }
}
