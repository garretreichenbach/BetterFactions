package thederpgamer.betterfactions.utils;

import api.common.GameClient;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.federation.Federation;
import java.util.HashMap;

/**
 * FederationUtils.java
 * <Description>
 *
 * @since 01/31/2021
 * @author TheDerpGamer
 */
public class FederationUtils {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    public static void createNewFederation(String federationName, final FactionData fromFaction, final FactionData toFaction) {
        Federation federation = new Federation(federationName, fromFaction, toFaction);
        fromFaction.setFederationId(federation.getId());
        toFaction.setFederationId(federation.getId());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationCreateNews(federation));
        PersistentObjectUtil.addObject(instance, federation);
        saveData();
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(FactionUtils.inFaction(GameClient.getClientPlayerState()) && (FactionUtils.getPlayerFactionData().getFederationId() == federation.getId())) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
    }

    public static void removeFederation(Federation federation) {
        PersistentObjectUtil.removeObject(instance, federation);
        saveData();
    }

    public static boolean federationExists(String name) {
        for(Federation federation : getFederationMap().values()) if(federation.getName().equals(name)) return true;
        return false;
    }

    public static HashMap<Integer, Federation> getFederationMap() {
        HashMap<Integer, Federation> federationMap = new HashMap<>();
        for(Object federationObject : PersistentObjectUtil.getObjects(instance, Federation.class)) {
            Federation federation = (Federation) federationObject;
            federationMap.put(federation.id, federation);
        }
        return federationMap;
    }

    public static Federation getFederation(FactionData factionData) {
        return getFederationMap().get(factionData.federationId);
    }

    public static int getNewId() {
        HashMap<Integer, Federation> federationMap = getFederationMap();
        for(int i = 0; i < federationMap.keySet().size(); i ++) {
            if(!federationMap.containsKey(i)) return i;
        }
        return -1;
    }

    public static void saveData() {
        PersistentObjectUtil.save(instance);
    }
}
