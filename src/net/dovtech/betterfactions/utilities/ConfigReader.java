package net.dovtech.betterfactions.utilities;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.io.InputStream;

public class ConfigReader {

    private static String configPath = "/SMMods/BetterFactions/config.yml";
    private static Yaml config = new Yaml(new Constructor(Configuration.class));

    public static Configuration getConfig() {
        InputStream inputStream = ConfigReader.class.getClassLoader().getResourceAsStream(configPath);
        Configuration configuration = config.load(inputStream);
        return configuration;
    }
}
