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
        try {
            spriteMap.put("default-logo", StarLoaderTexture.newSprite(ImageIO.read(BetterFactions.getInstance().getJarResource("thederpgamer/betterfactions/resources/sprites/default-logo.png")), BetterFactions.getInstance(), "default-logo"));
            spriteMap.put("pirates-logo", StarLoaderTexture.newSprite(ImageIO.read(BetterFactions.getInstance().getJarResource("thederpgamer/betterfactions/resources/sprites/pirates-logo.png")), BetterFactions.getInstance(), "pirates-logo"));
            spriteMap.put("traders-logo", StarLoaderTexture.newSprite(ImageIO.read(BetterFactions.getInstance().getJarResource("thederpgamer/betterfactions/resources/sprites/traders-logo.png")), BetterFactions.getInstance(), "traders-logo"));
        } catch(IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public Sprite getResource(String name) {
        return spriteMap.get(name);
    }

    @Override
    public void addResource(Object resource, String name) {
        spriteMap.put(name, (Sprite) resource);
    }

    public static void addSprite(Sprite sprite) {
        getInstance().addResource(sprite, sprite.getName());
    }

    public static Sprite getSprite(String name) {
        return getInstance().getResource(name);
    }
}
