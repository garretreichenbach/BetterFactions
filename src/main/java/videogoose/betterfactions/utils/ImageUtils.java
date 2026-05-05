package videogoose.betterfactions.utils;

import api.utils.textures.StarLoaderTexture;
import org.schema.schine.graphicsengine.forms.Sprite;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.manager.ResourceManager;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for loading and managing images.
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class ImageUtils {

    private static final Set<String> downloadingImages = ConcurrentHashMap.newKeySet();

    @Nullable
    public static Sprite getImage(String url) {
        fetchImage(url);
        return scaleSprite(getDefaultLogo(), ResourceManager.SPRITE_WIDTH, ResourceManager.SPRITE_HEIGHT);
    }

    public static Sprite scaleSprite(Sprite sprite, int width, int height) {
        sprite.setWidth(width);
        sprite.setHeight(height);
        return sprite;
    }

    private static void fetchImage(String url) {
        if (!downloadingImages.add(url)) return; // Already downloading
        Thread thread = new Thread(() -> {
            try {
                BufferedImage bufferedImage = fromURL(url);
                if (bufferedImage != null) {
                    StarLoaderTexture.runOnGraphicsThread(() -> {
                        Sprite sprite = StarLoaderTexture.newSprite(bufferedImage, BetterFactions.getInstance(), url);
                        sprite.setName(url);
                        sprite.setPositionCenter(false);
                        ResourceManager.addSprite(sprite);
                    });
                }
            } finally {
                downloadingImages.remove(url);
            }
        });
        thread.setDaemon(true);
        thread.setName("BetterFactions-ImageFetch");
        thread.start();
    }

    @Nullable
    private static BufferedImage fromURL(String s) {
        try {
            URLConnection conn = URI.create(s).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "NING/1.0");
            try (InputStream stream = conn.getInputStream()) {
                return ImageIO.read(stream);
            }
        } catch (IOException e) {
            BetterFactions.getInstance().logWarning("Failed to fetch image from \"" + s + "\": " + e.getMessage());
            return null;
        }
    }

    public static Sprite getDefaultLogo() {
        return ResourceManager.getSprite("default-logo");
    }
}
