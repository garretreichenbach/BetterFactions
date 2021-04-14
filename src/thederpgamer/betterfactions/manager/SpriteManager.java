package thederpgamer.betterfactions.manager;

import api.utils.textures.StarLoaderTexture;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.BetterFactions;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashMap;

/**
 * SpriteManager
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/01/2021
 */
public class SpriteManager implements ResourceManager {

    private static SpriteManager instance;
    private HashMap<String, Sprite> spriteMap;

    public static SpriteManager getInstance() {
        return instance;
    }

    @Override
    public void initialize() {
        instance = this;
        spriteMap = new HashMap<>();
        for(String s : getResourceList()) {
            try {
                spriteMap.put(s, StarLoaderTexture.newSprite(ImageIO.read(BetterFactions.getInstance().getJarResource("thederpgamer/betterfactions/resources/sprites/" + s + ".png")), BetterFactions.getInstance(), s));
            } catch(IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public String[] getResourceList() {
        return new String[] {
                "entity-class-hud-none",
                "entity-class-hud-mining",
                "entity-class-hud-support",
                "entity-class-hud-cargo",
                "entity-class-hud-attack",
                "entity-class-hud-defense",
                "entity-class-hud-carrier",
                "entity-class-hud-scout",
                "entity-class-hud-scavenger",

                "entity-class-hud-none-station",
                "entity-class-hud-shipyard-station",
                "entity-class-hud-outpost-station",
                "entity-class-hud-defense-station",
                "entity-class-hud-mining-station",
                "entity-class-hud-factory-station",
                "entity-class-hud-trade-station",
                "entity-class-hud-waypoint-station",
                "entity-class-hud-shopping-station"
        };
    }

    @Override
    public Sprite getResource(String name) {
        return spriteMap.get(name);
    }
}
