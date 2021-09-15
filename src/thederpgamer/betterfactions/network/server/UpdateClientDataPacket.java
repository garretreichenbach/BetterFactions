package thederpgamer.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.other.HashList;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.manager.FactionManager;
import thederpgamer.betterfactions.manager.FederationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Updates client data periodically
 *
 * version 1.0 - [01/31/2021]
 * @author TheDerpGamer
 */
public class UpdateClientDataPacket extends Packet {

    public enum DataType {
        FACTION_DATA("[FACTION DATA]: "),
        FEDERATION_DATA("[FEDERATION DATA]: ");

        public String prefix;

        DataType(String prefix) {
            this.prefix = prefix;
        }

        public static DataType fromPrefix(String search) {
            for(DataType dataType : values()) {
                if(dataType.prefix.equals(search)) return dataType;
            }
            return null;
        }
    }

    private final HashList<DataType, Object> dataMap = new HashList<>();

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        int keyCount = packetReadBuffer.readInt();
        for(int k = 0; k < keyCount; k ++) {
            String typeString = packetReadBuffer.readString();
            int dataCount = packetReadBuffer.readInt();
            ArrayList<Object> list = new ArrayList<>();
            DataType dataType = DataType.fromPrefix(typeString);
            if(dataType != null) {
                for(int i = 0; i < dataCount; i ++) {
                    if(dataType.equals(DataType.FACTION_DATA)) {
                        FactionData factionData = packetReadBuffer.readObject(FactionData.class);
                        list.add(factionData);
                    } else if(dataType.equals(DataType.FEDERATION_DATA)) {
                        Federation federation = packetReadBuffer.readObject(Federation.class);
                        list.add(federation);
                    }
                }
                dataMap.put(dataType, list);
            }
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeInt(dataMap.keySet().size());
        for(DataType type : dataMap.keySet()) {
            switch(type) {
                case FACTION_DATA:
                    packetWriteBuffer.writeString(DataType.FACTION_DATA.prefix);
                    break;
                case FEDERATION_DATA:
                    packetWriteBuffer.writeString(DataType.FEDERATION_DATA.prefix);
                    break;
            }
            packetWriteBuffer.writeInt(dataMap.get(type).size());
            for(Object object : dataMap.get(type)) {
                if(object instanceof FactionData) {
                    FactionData factionData = (FactionData) object;
                    packetWriteBuffer.writeObject(factionData);
                } else if(object instanceof Federation) {
                    Federation federation = (Federation) object;
                    packetWriteBuffer.writeObject(federation);
                }
            }
        }
    }

    @Override
    public void processPacketOnClient() {
        if(dataMap.containsKey(DataType.FACTION_DATA)) {
            ArrayList<FactionData> factionData = new ArrayList<>();
            for(Object faction : dataMap.get(DataType.FACTION_DATA)) factionData.add((FactionData) faction);
            FactionManager.updateFromServer(factionData);
        }

        if(dataMap.containsKey(DataType.FEDERATION_DATA)) {
            ArrayList<Federation> federations = new ArrayList<>();
            for(Object federation : dataMap.get(DataType.FEDERATION_DATA)) federations.add((Federation) federation);
            FederationManager.updateFromServer(federations);
        }
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        HashMap<Integer, FactionData> factionDataMap = FactionManager.getFactionDataMap();
        ArrayList<Object> factionDataList = new ArrayList<Object>(factionDataMap.values());
        dataMap.put(DataType.FACTION_DATA, factionDataList);

        HashMap<Integer, Federation> federationMap = FederationManager.getFederationMap();
        ArrayList<Object> federationList = new ArrayList<Object>(federationMap.values());
        dataMap.put(DataType.FEDERATION_DATA, federationList);
    }
}
