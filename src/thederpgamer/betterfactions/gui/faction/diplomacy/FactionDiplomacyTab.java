package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import api.common.GameCommon;
import org.newdawn.slick.Color;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.manager.FactionManager;
import thederpgamer.betterfactions.manager.ResourceManager;

/**
 * <Description>
 *
 * @version 1.0 - [01/30/2021]
 * @author TheDerpGamer
 */
public class FactionDiplomacyTab extends GUIContentPane {

    private Faction selectedFaction;
    private FactionInfoPanel infoPanel;
    private FactionActionsPanel actionsPanel;
    private FactionDiplomacyList factionList;

    public FactionDiplomacyTab(InputState state, GUIWindowInterface window) {
        super(state, window, "DIPLOMACY");
        this.selectedFaction = null;
    }

    @Override
    public void onInit() {
        super.onInit();
        setTextBoxHeightLast(270);
        addNewTextBox(0, 80);
        addDivider(250);

        setContent(0, 0, infoPanel = new FactionInfoPanel(getState()));
        setContent(0, 1, actionsPanel = new FactionActionsPanel(getState(), 250, 300));
        getContent(1, 0).attach(factionList = new FactionDiplomacyList(getState(), getContent(1, 0), this));

        infoPanel.onInit();
        actionsPanel.onInit();
        factionList.onInit();

        if(selectedFaction != null && FactionManager.inFaction(GameClient.getClientPlayerState()) && FactionManager.getFaction(GameClient.getClientPlayerState()).getIdFaction() != selectedFaction.getIdFaction()) {
            infoPanel.setFaction(selectedFaction);
            String relation = FactionManager.getFactionData(selectedFaction).getRelationString();
            if(relation.equals("Own Faction") || relation.equals("Allied") || relation.equals("In Federation")) {
                infoPanel.setNameText(selectedFaction.getName(), Color.green);
            } else if(relation.equals("Neutral")) {
                infoPanel.setNameText(selectedFaction.getName(), Color.blue);
            } else if(relation.equals("At War") || relation.equals("Personal Enemy")) {
                infoPanel.setNameText(selectedFaction.getName(), Color.red);
            } else infoPanel.setNameText(selectedFaction.getName(), Color.white);
            FactionData factionData = FactionManager.getFactionData(selectedFaction);
            if(factionData != null) {
                infoPanel.setInfoText(factionData.getInfoString());
                infoPanel.updateLogo(factionData.getFactionLogo());
            }
            actionsPanel.setFaction(selectedFaction);
            actionsPanel.recreateButtonPane();
        } else if(GameClient.getClientPlayerState().getFactionId() != 0) {
            Faction faction = GameCommon.getGameState().getFactionManager().getFaction(GameClient.getClientPlayerState().getFactionId());
            infoPanel.setFaction(faction);
            infoPanel.setNameText(faction.getName(), Color.green);
            FactionData factionData = FactionManager.getFactionData(faction);
            if(factionData != null) {
                infoPanel.setInfoText(factionData.getInfoString());
                infoPanel.updateLogo(factionData.getFactionLogo());
            }
            actionsPanel.setFaction(faction);
            actionsPanel.recreateButtonPane();
        } else {
            infoPanel.setNameText("No Faction");
            infoPanel.updateLogo(ResourceManager.getSprite("default-logo"));
            actionsPanel.setFaction(null);
            actionsPanel.recreateButtonPane();
        }
    }

    @Override
    public void draw() {
        try {
            super.draw();
        } catch(Exception ignored) { }
    }

    public void updateTab() {
        factionList.flagDirty();
        factionList.handleDirty();
        actionsPanel.recreateButtonPane();
    }

    public void setSelectedFaction(Faction selectedFaction) {
        this.selectedFaction = selectedFaction;
        infoPanel.setFaction(selectedFaction);
        infoPanel.setNameText(selectedFaction.getName());
        FactionData factionData = FactionManager.getFactionData(selectedFaction);
        if(factionData != null) {
            infoPanel.setInfoText(factionData.getInfoString());
            infoPanel.updateLogo(factionData.getFactionLogo());
        }
        actionsPanel.setFaction(selectedFaction);
        actionsPanel.recreateButtonPane();
    }
}
