package thederpgamer.betterfactions.utils;

import api.utils.textures.StarLoaderTexture;
import thederpgamer.betterfactions.BetterFactions;
import org.schema.schine.graphicsengine.forms.Sprite;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ImageUtils.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class ImageUtils {

    private final static ConcurrentHashMap<String, Sprite> imgCache = new ConcurrentHashMap<>();
    private final static ConcurrentLinkedQueue<String> downloadingImages = new ConcurrentLinkedQueue<>();

    @Nullable
    public static Sprite getImage(String url) {
        try {
            Sprite bufferedImage = imgCache.get(url);
            if (bufferedImage != null) {
                return scaleSprite(bufferedImage, 200, 200);
            } else {
                fetchImage(url);
                return scaleSprite(getDefaultLogo(), 200, 200);
            }
        } catch (Exception ignored) { }
        return scaleSprite(getDefaultLogo(), 200, 200);
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
                            Sprite sprite = StarLoaderTexture.newSprite(bufferedImage, BetterFactions.getInstance(), url + System.currentTimeMillis());
                            imgCache.put(url, sprite);
                        }
                    });
                    downloadingImages.remove(url);
                }
            }.start();
        }
    }

    private static BufferedImage fromURL(String u) {
        BufferedImage image = null;
        try {
            URL url = new URL(u);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "NING/1.0");
            InputStream stream = urlConnection.getInputStream();
            image = ImageIO.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public static Sprite getDefaultLogo() {
        try {
            BufferedImage bufferedImage = ImageIO.read(BetterFactions.getInstance().getJarResource("resources/images/temp-logo.png"));
            Sprite sprite = StarLoaderTexture.newSprite(bufferedImage, BetterFactions.getInstance(), "tempLogo");
            scaleSprite(sprite, 200, 200);
            return sprite;
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
