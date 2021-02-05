package thederpgamer.betterfactions.gui.faction.diplomacy;

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

    private NewFactionPanel guiPanel;
    private FactionInfoPanel infoPanel;
    private FactionActionsPanel actionsPanel;
    private FactionListPanel listPanel;

    public FactionDiplomacyTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("FACTION DIPLOMACY"));
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();

        setTextBoxHeightLast(270);
        addDivider(224);
        infoPanel = new FactionInfoPanel(getState());
        addNewTextBox(0, 70);
        actionsPanel = new FactionActionsPanel(getState());
        listPanel = new FactionListPanel(getState());

        infoPanel.onInit();
        actionsPanel.onInit();
        listPanel.onInit();

        if(guiPanel.isInFaction()) {
            FactionData factionData = FactionUtils.getFactionData(guiPanel.getOwnFaction());
            infoPanel.setFaction(guiPanel.getOwnFaction());
            infoPanel.setNameText(guiPanel.getOwnFaction().getName());
            infoPanel.setInfoText(factionData.getInfoString());
            infoPanel.updateLogo(factionData.getFactionLogo());
        } else {
            infoPanel.setNameText("No Faction");
        }
    }
}
