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

    //Diplomacy Config
    private static FileConfiguration diploConfig;
    public static final String[] defaultDiploConfig = {
            "white-peace-base-cost: 15.0",
            "white-peace-opposing-side-progress-limit: 60.0"
    };

    public static void initialize(BetterFactions instance) {
        mainConfig = instance.getConfig("config");
        mainConfig.saveDefault(defaultMainConfig);

        diploConfig = instance.getConfig("diplo-config");
        diploConfig.saveDefault(defaultDiploConfig);
    }

    public static FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public static FileConfiguration getDiploConfig() {
        return diploConfig;
    }
}
