package thederpgamer.betterfactions.gui.faction.diplomacy;

import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;

/**
 * FactionListPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionListPanel extends GUIInnerTextbox {

    private GUIAncor anchor;

    public FactionListPanel(InputState inputState, GUIAncor anchor) {
        super(inputState);
        this.anchor = anchor;
    }
}
