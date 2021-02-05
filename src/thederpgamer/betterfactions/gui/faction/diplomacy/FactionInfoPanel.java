package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.utils.StarRunnable;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.gui.elements.data.Orientation;
import thederpgamer.betterfactions.utils.GUIUtils;
import thederpgamer.betterfactions.utils.ImageUtils;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import javax.vecmath.Vector2f;

/**
 * FactionInfoPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionInfoPanel extends GUIInnerTextbox {

    private FactionLogoOverlay factionLogo;
    private GUITextOverlay nameOverlay;
    private GUITextOverlay infoOverlay;
    private GUITextOverlay[] cornerPosText;

    public FactionInfoPanel(InputState inputState) {
        super(inputState);
    }

    @Override
    public void onInit() {
        super.onInit();

        (factionLogo = new FactionLogoOverlay(ImageUtils.getImage(BetterFactions.getInstance().defaultLogo), getState())).onInit();
        attach(factionLogo);
        GUIUtils.orientateInsideFrame(factionLogo, Orientation.Horizontal.MIDDLE, Orientation.Vertical.TOP);

        (nameOverlay = new GUITextOverlay(10, 10, getState())).onInit();
        attach(nameOverlay);
        GUIUtils.orientateInsideFrame(nameOverlay, Orientation.Horizontal.MIDDLE, Orientation.Vertical.MIDDLE);

        (infoOverlay = new GUITextOverlay(10, 10, getState())).onInit();
        attach(infoOverlay);
        GUIUtils.orientateInsideFrame(infoOverlay, Orientation.Horizontal.MIDDLE, Orientation.Vertical.BOTTOM);

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
}
