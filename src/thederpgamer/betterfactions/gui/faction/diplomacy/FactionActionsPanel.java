package thederpgamer.betterfactions.gui.faction.diplomacy;

import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.utils.GUIUtils;
import javax.vecmath.Vector2f;

/**
 * FactionActionsPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIInnerTextbox {

    private GUITextOverlay[] cornerPosText;

    public FactionActionsPanel(InputState inputState) {
        super(inputState);
    }

    @Override
    public void onInit() {
        super.onInit();
        if(BetterFactions.getInstance().debugMode) {
            cornerPosText = new GUITextOverlay[5];
            for(int i = 0; i < cornerPosText.length; i ++) {
                Vector2f corner = GUIUtils.getCorners(this)[i];
                GUITextOverlay cornerText = new GUITextOverlay(10, 10, getState());
                cornerText.onInit();
                cornerText.setPos(corner.x, corner.y, getPos().z);
                attach(cornerText);
                cornerPosText[i] = cornerText;
            }
        }
    }

    @Override
    public void draw() {
        super.draw();

        if(BetterFactions.getInstance().debugMode) {
            if(BetterFactions.getInstance().showDebugText) {
                for(GUITextOverlay posTextOverlay : cornerPosText) {
                    posTextOverlay.setVisibility(1);
                    posTextOverlay.draw();
                }
            } else {
                for(GUITextOverlay posTextOverlay : cornerPosText) {
                    posTextOverlay.setVisibility(2);
                    posTextOverlay.cleanUp();
                }
            }
        }
    }
}
