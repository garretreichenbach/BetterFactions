package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import api.common.GameCommon;
import api.utils.gui.SimplePlayerTextInput;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIOverlay;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.gui.elements.data.GUIPositioningInterface;
import thederpgamer.betterfactions.utils.ImageUtils;
import javax.vecmath.Vector2f;
import java.util.Locale;

/**
 * FactionLogoOverlay.java
 * <Description>
 * ==================================================
 * Created 02/03/2021
 * @author TheDerpGamer
 */
public class FactionLogoOverlay extends GUIOverlay implements GUICallback {

    private GUIPositioningInterface posInterface;
    private Faction faction;
    private GUITextOverlay[] cornerPosText;

    public FactionLogoOverlay(Sprite sprite, InputState inputState) {
        super(sprite, inputState);
        faction = null;
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

    @Override
    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
        if(faction != null && getClientFaction() != null && faction.getIdFaction() == getClientFaction().getIdFaction()) { //Todo: Faction permissions check
            new SimplePlayerTextInput("Change Faction Logo", "Link to image (check server rules)") {
                @Override
                public boolean onInput(String s) {
                    if((s.startsWith("https://") || s.startsWith("http://")) && (s.toLowerCase(Locale.ROOT).endsWith(".png")
                            || s.toLowerCase(Locale.ROOT).endsWith(".jpg") || s.toLowerCase(Locale.ROOT).endsWith(".jpeg"))) {
                        setSprite(ImageUtils.getImage(s));
                        return true;
                    } else {
                        return false;
                    }
                }
            };
        }
    }

    @Override
    public boolean isOccluded() {
        return false;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public Faction getClientFaction() {
        int factionId = GameClient.getClientPlayerState().getFactionId();
        if(factionId != 0) {
            return GameCommon.getGameState().getFactionManager().getFaction(factionId);
        } else {
            return null;
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
