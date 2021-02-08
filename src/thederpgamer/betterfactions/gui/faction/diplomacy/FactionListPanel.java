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

    public FactionListPanel(InputState inputState, GUIAncor anchor) {
        super(inputState);
        anchor.attach(this);
        setWidth(anchor.getWidth());
        setHeight(anchor.getHeight());
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    @Override
    public void draw() {
        super.draw();
    }
}
