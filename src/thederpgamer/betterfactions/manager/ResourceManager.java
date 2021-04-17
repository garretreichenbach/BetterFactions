package thederpgamer.betterfactions.manager;

/**
 * ResourceManager
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/01/2021
 */
public interface ResourceManager {
    void initialize();
    Object getResource(String name);
    void addResource(Object resource, String name);
}
