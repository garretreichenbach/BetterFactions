package thederpgamer.betterfactions.gui.faction.diplomacy;

import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.gui.elements.data.GUIPositioningInterface;
import javax.vecmath.Vector2f;

/**
 * FactionListPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionListPanel extends GUIInnerTextbox {

    private GUIPositioningInterface posInterface;
    private GUITextOverlay[] cornerPosText;

    public FactionListPanel(InputState inputState) {
        super(inputState);
    }

    @Override
    public void onInit() {
        super.onInit();
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

    public GUIPositioningInterface getPosInterface() {
        if(posInterface == null) createPosInterface();
        return posInterface;
    }

    public void createPosInterface() {
        posInterface = new GUIPositioningInterface() {
            @Override
            public Vector2f[] getCorners() {
                Vector2f[] corners = new Vector2f[5];
                corners[0] = new Vector2f(getPos().x - (getWidth() / 2), getPos().y - (getHeight() / 2));
                corners[1] = new Vector2f(getPos().x + (getWidth() / 2), getPos().y - (getHeight() / 2));
                corners[2] = new Vector2f(getPos().x - (getWidth() / 2), getPos().y + (getHeight() / 2));
                corners[3] = new Vector2f(getPos().x + (getWidth() / 2), getPos().y + (getHeight() / 2));
                corners[4] = new Vector2f(getPos().x, getPos().y);
                return corners;
            }

            @Override
            public void createCornerPosText() {
                if(BetterFactions.getInstance().debugMode) {
                    cornerPosText = new GUITextOverlay[5];
                    for(int i = 0; i < cornerPosText.length; i ++) {
                        Vector2f corner = getCorners()[i];
                        GUITextOverlay cornerText = new GUITextOverlay(10, 10, getState());
                        cornerText.onInit();
                        cornerText.setPos(corner.x, corner.y, getPos().z);
                        attach(cornerText);
                        cornerPosText[i] = cornerText;
                    }
                }
            }
        };
        posInterface.createCornerPosText();
    }
}