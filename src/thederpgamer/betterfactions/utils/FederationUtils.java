package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.game.faction.Federation;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * FederationUtils.java
 * <Description>
 * ==================================================
 * Created 01/31/2021
 * @author TheDerpGamer
 */
public class FederationUtils {

    private static final boolean isClient = GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer();
    private static final boolean isServer = GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer();
    private static HashMap<Integer, Federation> federations = new HashMap<>();

    public static HashMap<Integer, Federation> getAllFederations() {
        if(isServer) {
            return federations;
        } else {
            return null;
        }
    }

    public static Federation getFederation(FactionData factionData) {
        if(factionData.getFederationId() != -1) {
            return federations.get(factionData.getFederationId());
        } else {
            return null;
        }
    }

    public static int getNewId(Federation federation) {
        if(isServer) {
            for(int i = 0; i < federations.keySet().size(); i ++) {
                if(!federations.containsKey(i)) {
                    federations.put(i, federation);
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    public static void loadData() {
        if(isServer) {
            ArrayList<Object> objects = PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), Federation.class);
            for(Object object : objects) {
                Federation federation = (Federation) object;
                federations.put(federation.getId(), federation);
            }
        }
    }

    public static void saveData() {
        if(isServer) {
            for(Federation federation : federations.values()) {
                PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), federation);
            }
            PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
        }
    }

    public static void updateFromServer(ArrayList<Federation> federationsList) {
        if(isClient) {
            for(Federation fed : federationsList) federations.put(fed.getId(), fed);
        }
    }
}
