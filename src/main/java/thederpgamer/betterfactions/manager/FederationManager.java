package thederpgamer.betterfactions.manager;

import api.common.GameClient;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.persistent.federation.FederationData;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * <Description>
 *
 * @version 1.0 - [01/31/2021]
 * @author TheDerpGamer
 */
public class FederationManager {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    public static void createNewFederation(String federationName, final FactionData fromFaction, final FactionData toFaction) {
        ArrayList<FederationData> toRemove = new ArrayList<>();
        for(Object obj : PersistentObjectUtil.getObjects(instance, FactionData.class)) {
            if(((FederationData) obj).getName() == federationName) toRemove.add((FederationData) obj);
        }
        for(FederationData oldData : toRemove) PersistentObjectUtil.removeObject(instance, oldData);

        FederationData federationData = new FederationData(federationName, fromFaction, toFaction);
        fromFaction.setFederationId(federationData.getId());
        toFaction.setFederationId(federationData.getId());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationCreateNews(federationData));
        PersistentObjectUtil.addObject(instance, federationData);
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(FactionManager.inFaction(GameClient.getClientPlayerState()) && (Objects.requireNonNull(FactionManager.getPlayerFactionData(GameClient.getClientPlayerState().getName())).getFederationId() == federationData.getId())) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
    }

    public static void removeFederation(FederationData federationData) {
        PersistentObjectUtil.removeObject(instance, federationData);
    }

    public static boolean federationExists(String name) {
        for(FederationData federationData : getFederationDataMap().values()) if(federationData.getName().equals(name)) return true;
        return false;
    }

    public static HashMap<Integer, FederationData> getFederationDataMap() {
        HashMap<Integer, FederationData> federationDataMap = new HashMap<>();
        if(NetworkSyncManager.onClient()) federationDataMap = NetworkSyncManager.getFederationDataCache();
        else {
            for(Object factionDataObject : PersistentObjectUtil.getObjects(instance, FederationData.class)) {
                FederationData federationData = (FederationData) factionDataObject;
                federationDataMap.put(federationData.getId(), federationData);
            }
        }
        return federationDataMap;
    }

    public static FederationData getFederation(FactionData factionData) {
        return getFederationDataMap().get(factionData.getFederationId());
    }

    public static int getNewId() {
        HashMap<Integer, FederationData> federationMap = getFederationDataMap();
        for(int i = 0; i < federationMap.keySet().size(); i ++) {
            if(!federationMap.containsKey(i)) return i + 100000;
        }
        return -1;
    }
}
