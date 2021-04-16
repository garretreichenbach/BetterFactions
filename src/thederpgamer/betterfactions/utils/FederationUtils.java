package thederpgamer.betterfactions.utils;

import api.common.GameClient;
import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.federation.Federation;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * FederationUtils.java
 * <Description>
 *
 * @since 01/31/2021
 * @author TheDerpGamer
 */
public class FederationUtils {

    private static HashMap<Integer, Federation> federations = new HashMap<>();

    public static void joinFederation(FactionData faction, Federation federation) {
        federations.get(federation.getId()).getMembers().add(faction);
        faction.setFederationId(federation.getId());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationJoinNews(federation, faction));
        saveData();
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(FactionUtils.inFaction(GameClient.getClientPlayerState()) && FactionUtils.getPlayerFactionData().getFederationId() == faction.getFederationId()) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
        //Todo: Update permissions and relations
    }

    public static void leaveFederation(FactionData faction) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationLeaveNews(faction.getFederation(), faction));
        boolean updateGUI = FactionUtils.inFaction(GameClient.getClientPlayerState()) && FactionUtils.getPlayerFactionData().getFederationId() == faction.getFederationId();
        federations.get(faction.getFederationId()).getMembers().remove(faction);
        faction.setFederationId(-1);
        saveData();
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(updateGUI) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
        //Todo: Update permissions and relations
    }

    public static void createNewFederation(String federationName, final FactionData fromFaction, final FactionData toFaction) {
        Federation federation = new Federation(federationName, fromFaction, toFaction);
        federations.put(federation.getId(), federation);
        fromFaction.setFederationId(federation.getId());
        toFaction.setFederationId(federation.getId());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationCreateNews(federation));
        BetterFactions.getInstance().updateClientData();
        saveData();
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(FactionUtils.inFaction(GameClient.getClientPlayerState()) && (FactionUtils.getPlayerFactionData().getFederationId() == federation.getId())) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
    }

    public static void disbandFederation(Federation federation) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationDisbandNews(federation));
        boolean updateGUI = FactionUtils.inFaction(GameClient.getClientPlayerState()) && FactionUtils.getPlayerFactionData().getFederationId() == federation.getId();
        for(FactionData factionData : federation.getMembers()) factionData.setFederationId(-1);
        federations.remove(federation.getId());
        saveData();
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(updateGUI) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
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
