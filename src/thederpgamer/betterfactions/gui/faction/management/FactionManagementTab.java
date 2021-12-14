package thederpgamer.betterfactions.gui.faction.management;

import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.NewFactionPanel;

/**
 * <Description>
 *
 * @version 1.0 - [01/30/2021]
 * @author TheDerpGamer
 */
public class FactionManagementTab extends GUIContentPane {

    private NewFactionPanel guiPanel;
    private FactionMessageScrollableList messageList;
    private FactionMembersList membersList;

    public FactionManagementTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, "FACTION");
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();
        setTextBoxHeightLast((int) (GLFrame.getHeight() / 2.5));
        addNewTextBox(0, (int) (GLFrame.getHeight() / 2.5));
        addDivider((int) (GLFrame.getWidth() / 2.5));
        addNewTextBox(1, (int) (GLFrame.getHeight() / 2.5));
        setTextBoxHeight(1, 0, 300);

        (messageList = new FactionMessageScrollableList(getState(), getContent(0, 0))).onInit();
        (membersList = new FactionMembersList(getState(), getContent(1, 0), this)).onInit();
    }

    public void updateTab() {
        messageList.redrawList();
        membersList.redrawList();
    }
}
