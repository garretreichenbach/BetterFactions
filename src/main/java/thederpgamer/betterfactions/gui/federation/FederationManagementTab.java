package thederpgamer.betterfactions.gui.federation;

import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.NewFactionPanel;

/**
 * FederationManagementTab.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class FederationManagementTab extends GUIContentPane {

    private NewFactionPanel guiPanel;

    public FederationManagementTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, "FEDERATION");
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    public void updateTab() {

    }

}
