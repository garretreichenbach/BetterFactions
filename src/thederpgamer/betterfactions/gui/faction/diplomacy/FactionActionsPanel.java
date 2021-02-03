package thederpgamer.betterfactions.gui.faction.diplomacy;

import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;

/**
 * FactionActionsPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIInnerTextbox {

    private GUIAncor anchor;

    public FactionActionsPanel(InputState inputState, GUIAncor anchor) {
        super(inputState);
        this.anchor = anchor;
    }
}
