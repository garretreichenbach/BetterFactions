package dovtech.betterfactions.gui.faction;

import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;

/**
 * FactionNewsTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionNewsTab extends GUIContentPane {

    private NewFactionPanel guiPanel;

    public FactionNewsTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("NEWS"));
        this.guiPanel = guiPanel;
    }
}
