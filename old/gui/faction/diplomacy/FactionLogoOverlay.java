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
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.manager.ResourceManager;
import thederpgamer.betterfactions.manager.data.FactionDataManager;

/**
 * FactionLogoOverlay.java
 * <Description>
 *
 * @since 02/03/2021
 * @author TheDerpGamer
 */
public class FactionLogoOverlay extends GUIOverlay implements GUICallback {

    private Faction faction;
    private final FactionInfoPanel infoPanel;

    public FactionLogoOverlay(Sprite sprite, FactionInfoPanel infoPanel, InputState inputState) {
        super(sprite, inputState);
        super.setCallback(this);
        this.infoPanel = infoPanel;
        this.faction = null;
    }

    @Override
    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
        if(mouseEvent.pressedLeftMouse()) {
            if (faction != null && getClientFaction() != null && faction.getIdFaction() == getClientFaction().getIdFaction()) { //Todo: Faction permissions check
                new SimplePlayerTextInput("Change Faction Logo", "Link to image (No inappropriate images!)") {
                    @Override
                    public boolean onInput(String s) {
                        if((s.startsWith("https://")) && (s.toLowerCase().endsWith(".png") || s.toLowerCase().endsWith(".jpg") || s.toLowerCase().endsWith(".jpeg"))) {
                            Sprite sprite = ResourceManager.getSprite(s);
                            FactionData factionData = FactionDataManager.instance.getFactionData(faction.getIdFaction());
                            factionData.setFactionLogo(s);
                            infoPanel.updateLogo(sprite);
                            return true;
                        } else return false;
                    }
                }.setText("");
            }
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
        if (factionId != 0) {
            return GameCommon.getGameState().getFactionManager().getFaction(factionId);
        } else {
            return null;
        }
    }
}
