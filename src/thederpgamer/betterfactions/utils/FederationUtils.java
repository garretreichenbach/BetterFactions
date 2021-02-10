package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.game.faction.Federation;
import thederpgamer.betterfactions.network.client.CreateNewFederationPacket;
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

    private static HashMap<Integer, Federation> federations = new HashMap<>();

    public static void joinFederation(FactionData faction, Federation federation) {
        federations.get(federation.getId()).getMembers().add(faction);
        faction.setFederationId(federation.getId());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationJoinNews(federation, faction));
        saveData();
        //Todo: Update permissions and relations
    }

    public static void leaveFederation(FactionData faction) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationLeaveNews(faction.getFederation(), faction));
        federations.get(faction.getFederationId()).getMembers().remove(faction);
        faction.setFederationId(-1);
        saveData();
        //Todo: Update permissions and relations
    }

    public static void createNewFederation(String federationName, FactionData fromFaction, FactionData toFaction) {
        if(GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer()) {
            PacketUtil.sendPacketToServer(new CreateNewFederationPacket(federationName, fromFaction, toFaction));
            new StarRunnable() {
                @Override
                public void run() {
                    BetterFactions.getInstance().redrawFactionPane();
                }
            }.runLater(BetterFactions.getInstance(), 15);
        }

        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            Federation federation = new Federation(federationName, fromFaction, toFaction);
            federations.put(federation.getId(), federation);
            fromFaction.setFederationId(federation.getId());
            toFaction.setFederationId(federation.getId());
            FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationCreateNews(federation));
            BetterFactions.getInstance().updateClientData();
            saveData();
        }
    }

    public static void disbandFederation(Federation federation) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationDisbandNews(federation));
        for(FactionData factionData : federation.getMembers()) factionData.setFederationId(-1);
        federations.remove(federation.getId());
        saveData();
    }

    public static boolean federationExists(String name) {
        for(Federation federation : federations.values()) {
            if(federation.getName().equals(name)) return true;
        }
        return false;
    }

    public static HashMap<Integer, Federation> getAllFederations() {
        return federations;
    }

    public static Federation getFederation(FactionData factionData) {
        if(factionData.getFederationId() != -1) {
            return federations.get(factionData.getFederationId());
        } else {
            return null;
        }
    }

    public static int getNewId(Federation federation) {
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
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
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            ArrayList<Object> objects = PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), Federation.class);
            for(Object object : objects) {
                Federation federation = (Federation) object;
                federations.put(federation.getId(), federation);
            }
        }
    }

    public static void saveData() {
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            for(Federation federation : federations.values()) {
                PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), federation);
            }
            PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
        }
    }

    public static void updateFromServer(ArrayList<Federation> federationsList) {
        if(GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer()) {
            for(Federation fed : federationsList) federations.put(fed.getId(), fed);
        }
    }
}
