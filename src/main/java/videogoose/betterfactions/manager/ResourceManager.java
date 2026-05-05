package videogoose.betterfactions.manager;

import api.utils.textures.StarLoaderTexture;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.resource.ResourceLoader;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.utils.DataUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ResourceManager {

    private static final BetterFactions instance = BetterFactions.getInstance();

    private static final HashMap<String, Sprite> spriteMap = new HashMap<>();

    public static void loadResources(ResourceLoader loader) {
        loadSprite("default_logo");
        loadSprite("pirates_logo");
        loadSprite("traders_logo");
    }

    private static void loadSprite(String name) {
        Sprite sprite = StarLoaderTexture.newSprite(instance.getJarBufferedImage("sprites/" + name + ".png"), instance, name);
        sprite.setPositionCenter(true);
        sprite.setName(name);
        spriteMap.put(name, sprite);
    }

    /**
     * Fetches an image from the web and caches it on the client.
     */
    public static CompletableFuture<Sprite> fetchImage(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URLConnection connection = new URI(url).toURL().openConnection();
                connection.setRequestProperty("User-Agent", "NING/1.0");
                try(InputStream inputStream = connection.getInputStream()) {
                    BufferedImage image = ImageIO.read(inputStream);
                    Sprite sprite = StarLoaderTexture.newSprite(image, instance, url);
                    sprite.setName(url);
                    sprite.setPositionCenter(true);
                    spriteMap.put(url, sprite);
                    saveImage(url, image);
                    return sprite;
                }
            } catch(Exception exception) {
                BetterFactions.getInstance().logDebug("Failed to fetch image: " + url + " - " + exception.getMessage());
                return getDefaultLogo();
            }
        });
    }

    private static void saveImage(String url, BufferedImage sprite) throws Exception {
        // Save the image to the mod's config directory for caching
        String fileName = url.replaceAll("[^a-zA-Z0-9]", "_") + ".png";
        Path path = Paths.get(Objects.requireNonNull(DataUtils.getWorldDataPath()), "cached_images", fileName);
        if(Files.exists(path)) return; // Don't overwrite existing cached images
        Files.createDirectories(path.getParent());
        ImageIO.write(sprite, "png", path.toFile());
    }

    public static Sprite getSprite(String name) {
        return spriteMap.getOrDefault(name, getDefaultLogo());
    }

    public static Sprite getDefaultLogo() {
        return spriteMap.get("default_logo");
    }
}
