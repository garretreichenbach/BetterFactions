package videogoose.betterfactions.manager;

import api.common.GameClient;
import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.persistent.federation.FederationData;
import videogoose.betterfactions.mixin.BetterFactionAccessor;
import videogoose.betterfactions.utils.FactionNewsUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * <Description>
 *
 * @version 1.0 - [01/31/2021]
 * @author TheDerpGamer
 */
public class FederationManager {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    public static void createNewFederation(String federationName, final Faction fromFaction, final Faction toFaction) {
        ArrayList<FederationData> toRemove = new ArrayList<>();
        for(Object obj : PersistentObjectUtil.getObjects(instance, FederationData.class)) {
            if(((FederationData) obj).getName() == federationName) toRemove.add((FederationData) obj);
        }
        for(FederationData oldData : toRemove) PersistentObjectUtil.removeObject(instance, oldData);

        FederationData federationData = new FederationData(federationName, fromFaction, toFaction);
        ((BetterFactionAccessor) fromFaction).setFederationId(federationData.getId());
        ((BetterFactionAccessor) toFaction).setFederationId(federationData.getId());
        FactionManager.saveStore(fromFaction.getIdFaction());
        FactionManager.saveStore(toFaction.getIdFaction());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationCreateNews(federationData));
        PersistentObjectUtil.addObject(instance, federationData);
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        Faction playerFaction = FactionManager.getFaction(GameClient.getClientPlayerState());
        if(FactionManager.inFaction(GameClient.getClientPlayerState()) && playerFaction != null && ((BetterFactionAccessor) playerFaction).getFederationId() == federationData.getId()) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
    }

    public static void createNewFederation(String federationName, int fromFactionId, int toFactionId) {
        Faction fromFaction = GameCommon.getGameState().getFactionManager().getFaction(fromFactionId);
        Faction toFaction = GameCommon.getGameState().getFactionManager().getFaction(toFactionId);
        if (fromFaction == null || toFaction == null) return;
        createNewFederation(federationName, fromFaction, toFaction);
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

    public static FederationData getFederation(Faction faction) {
        return getFederationDataMap().get(((BetterFactionAccessor) faction).getFederationId());
    }

    public static FederationData getFederation(int federationId) {
        return getFederationDataMap().get(federationId);
    }

    public static int getNewId() {
        HashMap<Integer, FederationData> federationMap = getFederationDataMap();
        if(federationMap.isEmpty()) return 100000;
        int maxId = Collections.max(federationMap.keySet());
        return maxId + 1;
    }
}
