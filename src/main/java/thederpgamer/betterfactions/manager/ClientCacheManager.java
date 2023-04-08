package thederpgamer.betterfactions.manager;

import thederpgamer.betterfactions.data.serializeable.FactionEntityData;
import thederpgamer.betterfactions.data.serializeable.war.WarData;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public class ClientCacheManager {

    public static final ConcurrentHashMap<String, FactionEntityData> factionAssets = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, WarData> factionWars = new ConcurrentHashMap<>();
}
