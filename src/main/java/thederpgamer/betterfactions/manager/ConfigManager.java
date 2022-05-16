package thederpgamer.betterfactions.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.betterfactions.BetterFactions;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [08/23/2021]
 */
public class ConfigManager {

    //Main Config
    private static FileConfiguration mainConfig;
    public static final String[] defaultMainConfig = {
            "debug-mode: false",
            "save-interval: 12000",
            "max-news-backup: 30",
            "log-queue-size: 3"
    };

    public static void initialize(BetterFactions instance) {
        mainConfig = instance.getConfig("config");
        mainConfig.saveDefault(defaultMainConfig);
    }

    public static FileConfiguration getMainConfig() {
        return mainConfig;
    }
}
