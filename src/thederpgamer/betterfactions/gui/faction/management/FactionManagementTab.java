package thederpgamer.betterfactions.gui.faction.management;

import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.NewFactionPanel;

/**
 * FactionManagementTab.java
 * <Description>
 *
 * @since  01/30/2021
 * @author TheDerpGamer
 */
public class FactionManagementTab extends GUIContentPane {

    private NewFactionPanel guiPanel;
    private FactionMembersList membersList;

    public FactionManagementTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, "FACTION");
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();
        setTextBoxHeightLast(300);
        addNewTextBox(0, 130);
        addDivider(300);
        addNewTextBox(1, 100);

        (membersList = new FactionMembersList(getState(), getContent(1, 0), this)).onInit();
    }

    public void updateTab() {
        membersList.redrawList();
    }
}
