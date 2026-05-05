package videogoose.betterfactions.manager;

import api.utils.textures.StarLoaderTexture;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.font.effects.OutlineEffect;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.resource.ResourceLoader;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.utils.ImageUtils;

import java.awt.*;
import java.util.HashMap;

/**
 * Manages mod resources.
 *
 * @author TheDerpGamer
 * @version 2.0 - [09/07/2021]
 */
public class ResourceManager {

    public static final int SPRITE_WIDTH = 200;
    public static final int SPRITE_HEIGHT = 200;

    private static final BetterFactions instance = BetterFactions.getInstance();

    private static final String[] spriteNames = {
            "default-logo",
            "pirates-logo",
            "traders-logo"
    };

    private static final String[] fontNames = {
            "Monda-Regular",
            "Monda-Bold"
    };

    private static final HashMap<String, Sprite> spriteMap = new HashMap<>();
    private static final HashMap<String, Font> fontMap = new HashMap<>();

    public static void loadResources(final ResourceLoader loader) {

        StarLoaderTexture.runOnGraphicsThread(() -> {

            //Load Sprites
            for(String spriteName : spriteNames) {
                try {
                    Sprite sprite = StarLoaderTexture.newSprite(instance.getJarBufferedImage("videogoose/betterfactions/resources/sprites/" + spriteName + ".png"), instance, spriteName);
                    sprite.setPositionCenter(false);
                    sprite.setName(spriteName);
                    spriteMap.put(spriteName, sprite);
                } catch(Exception exception) {
                    BetterFactions.getInstance().logWarning("Failed to load sprite \"" + spriteName + "\": " + exception.getMessage());
                }
            }

            //Load fonts
            for(String fontName : fontNames) {
                try {
                    fontMap.put(fontName, Font.createFont(Font.TRUETYPE_FONT, instance.getJarResource("videogoose/betterfactions/resources/fonts/" + fontName + ".ttf")));
                } catch(Exception exception) {
                    BetterFactions.getInstance().logWarning("Failed to load font \"" + fontName + "\": " + exception.getMessage());
                }
            }
        });
    }

    public static Sprite getSprite(String name) {
        Sprite sprite = spriteMap.get(name);
        if(sprite == null) {
            if((name.startsWith("https://")) && (name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"))) {
                sprite = ImageUtils.getImage(name);
                if(sprite != null) spriteMap.put(sprite.getName(), sprite);
            }
        }
        if(sprite == null) sprite = getSprite("default-logo");
        sprite.setWidth(SPRITE_WIDTH);
        sprite.setHeight(SPRITE_HEIGHT);
        return sprite;
    }

    public static void addSprite(Sprite sprite) {
        addSprite(sprite.getName(), sprite, true);
    }

    public static void addSprite(String name, Sprite sprite, boolean resize) {
        sprite.setName(name);
        if(resize) {
            sprite.setWidth(SPRITE_WIDTH);
            sprite.setHeight(SPRITE_HEIGHT);
        }
        spriteMap.put(name, sprite);
    }

    @SuppressWarnings("unchecked")
    public static UnicodeFont getFont(String fontName, int size, Color color, Color outlineColor, int outlineSize) {
        try {
            Font font = fontMap.get(fontName).deriveFont((float) size);
            UnicodeFont unicodeFont = new UnicodeFont(font);
            unicodeFont.getEffects().add(new OutlineEffect(outlineSize, outlineColor));
            unicodeFont.getEffects().add(new ColorEffect(color));
            unicodeFont.addGlyphs(0x4E00, 0x9FBF);
            unicodeFont.addAsciiGlyphs();
            unicodeFont.loadGlyphs();
            return unicodeFont;
        } catch(Exception exception) {
            BetterFactions.getInstance().logWarning("Failed to load font \"" + fontName + "\": " + exception.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static UnicodeFont getFont(String fontName, int size, Color color) {
        try {
            Font font = fontMap.get(fontName).deriveFont((float) size);
            UnicodeFont unicodeFont = new UnicodeFont(font);
            unicodeFont.getEffects().add(new ColorEffect(color));
            unicodeFont.addGlyphs(0x4E00, 0x9FBF);
            unicodeFont.addAsciiGlyphs();
            unicodeFont.loadGlyphs();
            return unicodeFont;
        } catch(Exception exception) {
            BetterFactions.getInstance().logWarning("Failed to load font \"" + fontName + "\": " + exception.getMessage());
        }
        return null;
    }
}
