package thederpgamer.betterfactions.gui.faction.management;

import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.NewFactionPanel;

import javax.vecmath.Vector3f;

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
    private FactionOptionsPane optionsPane;
    private FactionAssetsList assetsList;

    public FactionManagementTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, "FACTION");
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();
        setTextBoxHeightLast((int) (GLFrame.getHeight() / 2.5));
        addNewTextBox(0, (int) (GLFrame.getHeight() / 2.5));
        addDivider((int) (getWidth() / 1.8f));
        addNewTextBox(1, (int) (GLFrame.getHeight() / 2.5));
        (messageList = new FactionMessageScrollableList(getState(), getContent(0, 0), this)).onInit();
        (membersList = new FactionMembersList(getState(), getContent(1, 0), this)).onInit();
        optionsPane = new FactionOptionsPane(getState(), this);
        setContent(1, 1, optionsPane);
        optionsPane.onInit();
        optionsPane.setPos(new Vector3f(1.0f, 1.0f, 0.0f));
        //addNewTextBox(1, (int) ((GLFrame.getHeight() / 2.5) - optionsPane.getHeight()));
        setTextBoxHeight(1, 1, 28 * 3);
        (assetsList = new FactionAssetsList(getState(), getContent(0, 1), this)).onInit();
    }

    public void updateTab() {
        messageList.redrawList();
        membersList.redrawList();
    }
}
