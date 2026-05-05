package videogoose.betterfactions.manager;

import api.common.GameCommon;
import api.common.GameServer;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import videogoose.betterfactions.data.persistent.PersistentData;
import videogoose.betterfactions.data.persistent.diplomacy.DiplomaticDataOld;
import videogoose.betterfactions.data.persistent.federation.FederationData;
import videogoose.betterfactions.network.server.ServerSyncDataPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages and syncs data from the server to clients.
 * Faction data is now stored via game's native Faction + mixin accessors.
 * Only FederationData and DiplomaticData need network sync.
 *
 * @author TheDerpGamer
 * @version 2.0 - [09/15/2021]
 */
public class NetworkSyncManager {

    //Data Types
    public static final short FEDERATION_DATA = 1;
    public static final short DIPLOMATIC_DATA = 2;

    //Modification Types
    public static final short UPDATE = 0; //Updates any matching data in the client's data cache with the updated version.
    public static final short ADD = 1;    //Adds data to the client's data cache.
    public static final short REMOVE = 2; //Removes data from the client's data cache.

    //Serverside
    /**
     * Sends a packet to modify persistent data in the client data cache.
     * @param client The client to send the data to
     * @param modType The type of modification
     * @param data The data to send
     */
    public static void sendToClient(PlayerState client, short modType, PersistentData... data) {
        assert onServer() : "Clients cannot be here.";
        PacketUtil.sendPacket(client, new ServerSyncDataPacket(modType, data));
    }

    /**
     * Sends all updated persistent data to clients.
     */
    public static void sendServerModifications() {
        ArrayList<PersistentData> updateQueue = new ArrayList<>(getDataUpdateQueue().values());
        PersistentData[] updateArray = new PersistentData[updateQueue.size()];
        for(int i = 0; i < updateArray.length; i ++) updateArray[i] = updateQueue.get(i);
        for(PlayerState playerState : getPlayers()) sendToClient(playerState, UPDATE, updateArray);
    }

    public static ArrayList<PlayerState> getPlayers() {
        return new ArrayList<>(GameServer.getServerState().getPlayerStatesByName().values());
    }

    public static HashMap<Integer, PersistentData> getDataUpdateQueue() {
        HashMap<Integer, PersistentData> updateQueue = new HashMap<>();

        for(Map.Entry<Integer, FederationData> entry : FederationManager.getFederationDataMap().entrySet()) {
            if(entry.getValue().needsUpdate()) updateQueue.put(entry.getKey(), entry.getValue());
        }

        //Todo: Diplomatic data update queue
        return updateQueue;
    }

    //Clientside
    private static final ConcurrentHashMap<Integer, PersistentData> clientDataCache = new ConcurrentHashMap<>();

    /**
     * Returns a map of all the Federation Data currently stored in the client data cache.
     * @return A map of all stored Federation Data
     */
    public static HashMap<Integer, FederationData> getFederationDataCache() {
        assert onClient() : "Server cannot be here.";
        HashMap<Integer, FederationData> dataCache = new HashMap<>();
        for(Map.Entry<Integer, PersistentData> entry : clientDataCache.entrySet()) {
            if(entry.getValue().getDataType() == FEDERATION_DATA && entry.getValue() instanceof FederationData federationData) {
                dataCache.put(entry.getKey(), federationData);
            }
        }
        return dataCache;
    }

    /**
     * Returns a map of all the Diplomatic Data currently stored in the client data cache.
     * @return A map of all stored Diplomatic Data
     */
    public static HashMap<Integer, DiplomaticDataOld> getDiplomaticDataCache() {
        assert onClient() : "Server cannot be here.";
        HashMap<Integer, DiplomaticDataOld> dataCache = new HashMap<>();
        for(Map.Entry<Integer, PersistentData> entry : clientDataCache.entrySet()) {
            if(entry.getValue().getDataType() == DIPLOMATIC_DATA && entry.getValue() instanceof DiplomaticDataOld diplomaticData) {
                dataCache.put(entry.getKey(), diplomaticData);
            }
        }
        return dataCache;
    }

    /**
     * Processes Persistent Data modifications sent from the server and syncs the client data cache accordingly.
     * @param modType The type of modification
     * @param data The Persistent Data being modified / updated
     */
    public static void processSyncData(int modType, PersistentData... data) {
        assert onClient() : "Server cannot be here.";
        for(PersistentData persistentData : data) {
            int dataId = persistentData.getDataId();
            switch(modType) {
                case UPDATE -> {
                    if(!clientDataCache.containsKey(dataId)) addSyncData(persistentData);
                    else updateSyncData(persistentData);
                }
                case ADD -> addSyncData(persistentData);
                case REMOVE -> removeSyncData(persistentData);
            }
        }
    }

    private static void updateSyncData(PersistentData data) {
        clientDataCache.replace(data.getDataId(), data);
    }

    private static void addSyncData(PersistentData data) {
        clientDataCache.remove(data.getDataId());
        clientDataCache.put(data.getDataId(), data);
    }

    private static void removeSyncData(PersistentData data) {
        clientDataCache.remove(data.getDataId());
    }

    //Common
    /**
     * Checks if the game state is a dedicated or local server (single player).
     * @return If the game state is a server
     */
    public static boolean onServer() {
        return GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer();
    }

    /**
     * Checks if the game state is a client connected to a dedicated or local server (single player).
     * @return If the game state is a client
     */
    public static boolean onClient() {
        return GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer();
    }
}
