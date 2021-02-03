package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * FactionUtils.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionUtils {

    private static final boolean isClient = GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer();
    private static final boolean isServer = GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer();
    private static HashMap<Integer, FactionData> factionData = new HashMap<>();

    public static HashMap<Integer, FactionData> getAllFactions() {
        if(isServer) {
            return factionData;
        } else {
            return null;
        }
    }

    public static FactionData getFactionData(Faction faction) {
        if(factionData.containsKey(faction.getIdFaction())) {
            return factionData.get(faction.getIdFaction());
        } else {
            return null;
        }
    }

    public static void loadData() {
        if(isServer) {
            ArrayList<Object> objects = PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), FactionData.class);
            for(Object object : objects) {
                FactionData fData = (FactionData) object;
                factionData.put(fData.getFactionId(), fData);
            }
        }
    }

    public static void saveData() {
        if(isServer) {
            for(FactionData fData : factionData.values()) {
                PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), fData);
            }
            PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
        }
    }

    public static void updateFromServer(ArrayList<FactionData> factionDataList) {
        if(isClient) {
            for(FactionData fData : factionDataList) factionData.put(fData.getFactionId(), fData);
        }
    }
}
