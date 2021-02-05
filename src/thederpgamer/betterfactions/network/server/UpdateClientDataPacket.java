package thederpgamer.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.other.HashList;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.game.faction.Federation;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * UpdateClientDataPacket.java
 * Updates client data periodically
 * [Server -> Client]
 * ==================================================
 * Created 01/31/2021
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

    private HashList<DataType, Object> dataMap;

    public UpdateClientDataPacket() {

    }

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
        ArrayList<FactionData> factionData = new ArrayList<>();
        for(Object faction : dataMap.get(DataType.FACTION_DATA)) {
            factionData.add((FactionData) faction);
        }
        FactionUtils.updateFromServer(factionData);

        ArrayList<Federation> federations = new ArrayList<>();
        for(Object federation : dataMap.get(DataType.FEDERATION_DATA)) {
            federations.add((Federation) federation);
        }
        FederationUtils.updateFromServer(federations);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        dataMap = new HashList<>();

        HashMap<Integer, FactionData> factionDataMap = FactionUtils.getAllFactions();
        if(factionDataMap != null) {
            ArrayList<Object> factionDataList = new ArrayList<>();
            factionDataList.addAll(factionDataMap.values());
            dataMap.put(DataType.FACTION_DATA, factionDataList);
        }

        HashMap<Integer, Federation> federationMap = FederationUtils.getAllFederations();
        if(federationMap != null) {
            ArrayList<Object> federationList = new ArrayList<>();
            federationList.addAll(federationMap.values());
            dataMap.put(DataType.FEDERATION_DATA, federationList);
        }
        BetterFactions.getInstance().lastClientUpdate = 0;
    }
}
