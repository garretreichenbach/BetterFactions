package thederpgamer.betterfactions.manager;

import api.utils.textures.StarLoaderTexture;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
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

    private final String spritesPath = "thederpgamer/betterfactions/resources/sprites/";
    private final String[] spriteNames = {"default-logo", "pirates-logo", "traders-logo"};
    private HashMap<String, Sprite> spriteMap;

    public static SpriteManager getInstance() {
        return instance;
    }

    @Override
    public void initialize() {
        instance = this;
        spriteMap = new HashMap<>();
        for(String spriteName : spriteNames) {
            try {
                Sprite sprite = StarLoaderTexture.newSprite(ImageIO.read(BetterFactions.getInstance().getJarResource( spritesPath + spriteName + ".png")), BetterFactions.getInstance(), spriteName);
                sprite.setName(spriteName);
                spriteMap.put(spriteName, sprite);
            } catch(IOException exception) {
                exception.printStackTrace();
            }
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

    public static Sprite getFactionLogo(FactionData factionData) {
        String spriteName = factionData.factionName.replace(" ", "-") + "-logo";
        if(getInstance().spriteMap.containsKey(spriteName)) return getSprite(spriteName);
        else return getSprite("default-logo");
    }
}
