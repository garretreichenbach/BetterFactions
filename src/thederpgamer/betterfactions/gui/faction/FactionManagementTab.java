package thederpgamer.betterfactions.gui.faction;

import thederpgamer.betterfactions.gui.NewFactionPanel;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;

/**
 * FactionManagementTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionManagementTab extends GUIContentPane {

    private NewFactionPanel guiPanel;

    public FactionManagementTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("FACTION MANAGEMENT"));
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    @Override
    public void draw() {
        super.draw();
        if(guiPanel.isInFaction()) {

        } else {
            cleanUp();
        }
    }
}
