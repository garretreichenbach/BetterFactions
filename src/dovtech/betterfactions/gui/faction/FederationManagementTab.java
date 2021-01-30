package dovtech.betterfactions.gui.faction;

import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;

/**
 * FederationManagementTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FederationManagementTab extends GUIContentPane {

    private NewFactionPanel guiPanel;

    public FederationManagementTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("FEDERATION MANAGEMENT"));
        this.guiPanel = guiPanel;
    }
}