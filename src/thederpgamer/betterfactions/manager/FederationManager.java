package thederpgamer.betterfactions.manager;

import api.common.GameClient;
import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <Description>
 *
 * @version 1.0 - [01/31/2021]
 * @author TheDerpGamer
 */
public class FederationManager {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    private static final ConcurrentLinkedQueue<Federation> clientDataCache = new ConcurrentLinkedQueue<>();

    public static void createNewFederation(String federationName, final FactionData fromFaction, final FactionData toFaction) {
        Federation federation = new Federation(federationName, fromFaction, toFaction);
        fromFaction.setFederationId(federation.getId());
        toFaction.setFederationId(federation.getId());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationCreateNews(federation));
        PersistentObjectUtil.addObject(instance, federation);
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(FactionManager.inFaction(GameClient.getClientPlayerState()) && (Objects.requireNonNull(FactionManager.getPlayerFactionData(GameClient.getClientPlayerState().getName())).getFederationId() == federation.getId())) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
    }

    public static void removeFederation(Federation federation) {
        PersistentObjectUtil.removeObject(instance, federation);
    }

    public static boolean federationExists(String name) {
        for(Federation federation : getFederationMap().values()) if(federation.getName().equals(name)) return true;
        return false;
    }

    public static HashMap<Integer, Federation> getFederationMap() {
        HashMap<Integer, Federation> federationMap = new HashMap<>();
        if(GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer()) {
            for(Federation federation : clientDataCache) federationMap.put(federation.id, federation);
        } else {
            for(Object federationObject : PersistentObjectUtil.getObjects(instance, Federation.class)) {
                Federation federation = (Federation) federationObject;
                federationMap.put(federation.id, federation);
            }
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

    public static void updateFromServer(ArrayList<Federation> federations) {
        if(GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer()) {
            clientDataCache.clear();
            clientDataCache.addAll(federations);
            //Todo: Maybe only send the data that actually needs updating
        }
    }
}
