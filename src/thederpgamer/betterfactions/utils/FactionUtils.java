package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
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

    private static final boolean isClient = BetterFactions.getInstance().isClient();
    private static final boolean isServer = BetterFactions.getInstance().isServer();
    private static final HashMap<Integer, FactionData> factionData = new HashMap<>();

    public static boolean inFaction(PlayerState playerState) {
        return playerState.getFactionId() != 0;
    }

    public static Faction getFaction(PlayerState playerState) {
        if(playerState.getFactionId() == 0) {
            return null;
        } else {
            return GameCommon.getGameState().getFactionManager().getFaction(playerState.getFactionId());
        }
    }

    public static HashMap<Integer, FactionData> getAllFactions() {
        if(isServer) {
            return factionData;
        } else {
            return null;
        }
    }

    public static FactionData getFactionData(Faction faction) {
        loadData();
        if(factionData.containsKey(faction.getIdFaction())) {
            return factionData.get(faction.getIdFaction());
        } else {
            if(isServer) {
                FactionData fData = new FactionData(faction);
                factionData.put(fData.getFactionId(), fData);
                saveData();
                return fData;
            } else {
                return null;
            }
        }
    }

    public static void loadData() {
        if(isServer) {
            ArrayList<Object> objects = PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), FactionData.class);
            for(Object object : objects) {
                FactionData fData = (FactionData) object;
                factionData.put(fData.getFactionId(), fData);
            }
            saveData();
        }
    }

    public static void saveData() {
        if(isServer) {
            ArrayList<FactionData> toDelete = new ArrayList<>();
            for(FactionData fData : factionData.values()) {
                if(GameCommon.getGameState().getFactionManager().getFactionMap().containsKey(fData.getFactionId())) {
                    PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), fData);
                } else {
                    toDelete.add(fData);
                }
            }
            for(FactionData fData : toDelete) factionData.remove(fData.getFactionId());
            PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
        }
    }

    public static void updateFromServer(ArrayList<FactionData> factionDataList) {
        if(isClient) {
            for(FactionData fData : factionDataList) factionData.put(fData.getFactionId(), fData);
        }
    }
}
