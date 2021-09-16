package thederpgamer.betterfactions.utils;

import api.utils.textures.StarLoaderTexture;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.manager.ResourceManager;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ImageUtils.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class ImageUtils {

    private final static ConcurrentLinkedQueue<String> downloadingImages = new ConcurrentLinkedQueue<>();

    @Nullable
    public static Sprite getImage(String url) {
        try {
            fetchImage(url);
            return scaleSprite(getDefaultLogo(), ResourceManager.SPRITE_WIDTH, ResourceManager.SPRITE_HEIGHT);
        } catch(Exception ignored) { }
        return scaleSprite(getDefaultLogo(), ResourceManager.SPRITE_WIDTH, ResourceManager.SPRITE_HEIGHT);
    }

    public static Sprite scaleSprite(Sprite sprite, int width, int height) {
        sprite.setWidth(width);
        sprite.setHeight(height);
        return sprite;
    }

    private static void fetchImage(final String url) {
        if (!downloadingImages.contains(url)) {
            new Thread() {
                @Override
                public void run() {
                    downloadingImages.add(url);
                    final BufferedImage bufferedImage = fromURL(url);
                    StarLoaderTexture.runOnGraphicsThread(new Runnable() {
                        @Override
                        public void run() {
                            Sprite sprite = StarLoaderTexture.newSprite(bufferedImage, BetterFactions.getInstance(), url);
                            sprite.setName(url);
                            sprite.setPositionCenter(false);
                            ResourceManager.addSprite(sprite);
                        }
                    });
                    downloadingImages.remove(url);
                }
            }.start();
        }
    }

    private static BufferedImage fromURL(String s) {
        BufferedImage image = null;
        try {
            URL url = new URL(s);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "NING/1.0");
            InputStream stream = urlConnection.getInputStream();
            image = ImageIO.read(stream);
        } catch(IOException exception) {
            LogManager.logException("Something went wrong while trying to fetch an image from url \"" + s + "\"", exception);
        }
        return image;
    }

    public static Sprite getDefaultLogo() {
        return ResourceManager.getSprite("default-logo");
    }
}
