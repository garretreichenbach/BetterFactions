package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.utils.StarRunnable;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.gui.elements.PositionableGUITextOverlay;
import thederpgamer.betterfactions.gui.elements.data.GUIPositioningInterface;
import thederpgamer.betterfactions.gui.elements.data.Orientation;
import thederpgamer.betterfactions.utils.GUIUtils;
import thederpgamer.betterfactions.utils.ImageUtils;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import javax.vecmath.Vector2f;
import java.util.Objects;

/**
 * FactionInfoPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionInfoPanel extends GUIInnerTextbox {

    private GUIPositioningInterface posInterface;
    private FactionLogoOverlay factionLogo;
    private PositionableGUITextOverlay nameOverlay;
    private PositionableGUITextOverlay infoOverlay;
    private GUITextOverlay[] cornerPosText;

    public FactionInfoPanel(InputState inputState) {
        super(inputState);
    }

    @Override
    public void onInit() {
        super.onInit();

        (factionLogo = new FactionLogoOverlay(ImageUtils.getImage(BetterFactions.getInstance().defaultLogo), getState())).onInit();
        attach(factionLogo);

        (nameOverlay = new PositionableGUITextOverlay(getState())).onInit();
        attach(nameOverlay);

        (infoOverlay = new PositionableGUITextOverlay(getState())).onInit();
        attach(infoOverlay);

        orientateElements();
    }

    public void orientateElements() {
        GUIUtils.orientateInsideFrame(factionLogo, factionLogo.getPosInterface(), Orientation.Horizontal.MIDDLE, Orientation.Vertical.TOP);
        GUIUtils.orientateInsideFrame(nameOverlay, nameOverlay.getPosInterface(), Orientation.Horizontal.MIDDLE, Orientation.Vertical.MIDDLE);
        GUIUtils.orientateInsideFrame(infoOverlay, infoOverlay.getPosInterface(), Orientation.Horizontal.MIDDLE, Orientation.Vertical.BOTTOM);
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

    public void updateLogo(final String newPath) {
        factionLogo.setSprite(ImageUtils.getImage(newPath));
        if(factionLogo.getSprite().getName().equals("tempLogo")) {
            new StarRunnable() {
                @Override
                public void run() {
                    if(!factionLogo.getSprite().getName().equals("tempLogo")) {
                        cancel();
                    } else {
                        factionLogo.setSprite(ImageUtils.getImage(newPath));
                    }
                }
            }.runTimer(BetterFactions.getInstance(), 1000);
        }
    }

    public void setNameText(String nameText) {
        nameOverlay.setTextSimple(nameText);
        nameOverlay.setFont(FontLibrary.FontSize.BIG.getFont());
    }

    public void setInfoText(String newText) {
        infoOverlay.setTextSimple(newText);
        infoOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
    }

    public void setFaction(Faction faction) {
        factionLogo.setFaction(faction);
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
