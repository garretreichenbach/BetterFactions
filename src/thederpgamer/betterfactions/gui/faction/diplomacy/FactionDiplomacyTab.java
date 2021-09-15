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
import thederpgamer.betterfactions.manager.ResourceManager;
import thederpgamer.betterfactions.manager.FactionManager;

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
        super(state, window, Lng.str("DIPLOMACY"));
        this.selectedFaction = null;
    }

    @Override
    public void onInit() {
        super.onInit();
        setTextBoxHeightLast(270);
        addNewTextBox(0, 80);
        addDivider(248);

        setContent(0, 0, infoPanel = new FactionInfoPanel(getState()));
        setContent(0, 1, actionsPanel = new FactionActionsPanel(getState(), 248, 300));
        getContent(1, 0).attach(factionList = new FactionDiplomacyList(getState(), getContent(1, 0), this));

        infoPanel.onInit();
        actionsPanel.onInit();
        factionList.onInit();

        if(selectedFaction != null && FactionManager.inFaction(GameClient.getClientPlayerState()) && FactionManager.getFaction(GameClient.getClientPlayerState()).getIdFaction() != selectedFaction.getIdFaction()) {
            infoPanel.setFaction(selectedFaction);
            String relation = Lng.str(FactionManager.getFactionData(selectedFaction).getRelationString());
            if(relation.equals(Lng.str("Own Faction")) || relation.equals(Lng.str("Allied")) || relation.equals(Lng.str("In Federation"))) {
                infoPanel.setNameText(selectedFaction.getName(), Color.green);
            } else if(relation.equals(Lng.str("Neutral"))) {
                infoPanel.setNameText(selectedFaction.getName(), Color.blue);
            } else if(relation.equals(Lng.str("At War")) || relation.equals(Lng.str("Personal Enemy"))) {
                infoPanel.setNameText(selectedFaction.getName(), Color.red);
            } else infoPanel.setNameText(selectedFaction.getName());
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
        super.draw();
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
