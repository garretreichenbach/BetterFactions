package dovtech.betterfactions.gui.faction;

import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;

/**
 * FactionOptionsTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionOptionsTab extends GUIContentPane {

    private NewFactionPanel guiPanel;

    public FactionOptionsTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("OPTIONS"));
        this.guiPanel = guiPanel;
    }
}
