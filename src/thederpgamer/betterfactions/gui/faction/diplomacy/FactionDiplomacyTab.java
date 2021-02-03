package thederpgamer.betterfactions.gui.faction.diplomacy;

import thederpgamer.betterfactions.gui.NewFactionPanel;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
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
        addDivider(330);
        infoPanel = new FactionInfoPanel(getState(), getContent(0, 0));
        addNewTextBox(0, 70);
        actionsPanel = new FactionActionsPanel(getState(), getContent(0, 1));
        listPanel = new FactionListPanel(getState(), getContent(1, 0));

        infoPanel.onInit();
        actionsPanel.onInit();
        listPanel.onInit();

        if(guiPanel.isInFaction()) {
            infoPanel.setNameText(guiPanel.getOwnFaction().getName());
            infoPanel.setInfoText(FactionUtils.getFactionData(guiPanel.getOwnFaction()).getInfoString());
        }
    }
}
