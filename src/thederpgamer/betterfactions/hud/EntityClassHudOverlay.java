package thederpgamer.betterfactions.hud;

import api.common.GameCommon;
import javafx.scene.layout.Pane;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.NetworkListenerEntity;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.manager.SpriteManager;
import thederpgamer.betterfactions.utils.EntityUtils;

/**
 * EntityClassHudOverlay
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/01/2021
 */
public class EntityClassHudOverlay extends GUIElement {

    private ManagedUsableSegmentController<?> entity;
    private BlueprintClassification entityClass;
    private Sprite overlaySprite;

    public EntityClassHudOverlay(InputState inputState, Ship entity) {
        super(inputState);
        this.entity = entity;
        this.entityClass = EntityUtils.getEntityClassification(entity);
    }

    public EntityClassHudOverlay(InputState inputState, SpaceStation entity) {
        super(inputState);
        this.entity = entity;
        this.entityClass = EntityUtils.getEntityClassification(entity);
    }

    @Override
    public void onInit() {

    }

    @Override
    public void draw() {

    }

    @Override
    public void cleanUp() {

    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 0;
    }

    private Sprite getSprite() {
        return SpriteManager.getInstance().getResource("entity-class-hud-" + entityClass.toString().toLowerCase().replaceAll("_", "-"));
    }
}
