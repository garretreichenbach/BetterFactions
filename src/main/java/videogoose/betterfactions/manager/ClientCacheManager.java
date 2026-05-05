package videogoose.betterfactions.manager;

import videogoose.betterfactions.data.serializeable.FactionEntityData;
import videogoose.betterfactions.data.serializeable.war.WarData;

import java.util.concurrent.ConcurrentHashMap;

public class ClientCacheManager {

    public static final ConcurrentHashMap<String, FactionEntityData> factionAssets = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, WarData> factionWars = new ConcurrentHashMap<>();
}
