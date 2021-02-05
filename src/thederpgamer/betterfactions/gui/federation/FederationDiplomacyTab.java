package thederpgamer.betterfactions.gui.federation;

import thederpgamer.betterfactions.gui.NewFactionPanel;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;

/**
 * FederationDiplomacyTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FederationDiplomacyTab extends GUIContentPane {

    private NewFactionPanel guiPanel;

    public FederationDiplomacyTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("FEDERATION DIPLOMACY"));
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
