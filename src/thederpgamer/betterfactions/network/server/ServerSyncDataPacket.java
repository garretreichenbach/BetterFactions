package thederpgamer.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.persistent.PersistentData;
import thederpgamer.betterfactions.data.persistent.diplomacy.DiplomaticData;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.persistent.federation.FederationData;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.manager.NetworkSyncManager;

import java.io.IOException;

/**
 * Packet to sync data from the server to the client.
 * <p>[SERVER] -> [CLIENT]</p>
 *
 * @version 1.0 - [09/15/2021]
 * @author TheDerpGamer
 */
public class ServerSyncDataPacket extends Packet {

    private int modType;
    private PersistentData[] data;

    public ServerSyncDataPacket() {

    }

    public ServerSyncDataPacket(int modType, PersistentData... data) {
        this.modType = modType;
        this.data = data;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        modType = packetReadBuffer.readInt();
        data = new PersistentData[packetReadBuffer.readInt()];
        for(int i = 0; i < data.length; i ++) {
            int dataType = packetReadBuffer.readInt();
            switch(dataType) {
                case NetworkSyncManager.FACTION_DATA:
                    data[i] = packetReadBuffer.readObject(FactionData.class);
                    break;
                case NetworkSyncManager.FEDERATION_DATA:
                    data[i] = packetReadBuffer.readObject(FederationData.class);
                    break;
                case NetworkSyncManager.DIPLOMATIC_DATA:
                    data[i] = packetReadBuffer.readObject(DiplomaticData.class);
                    break;
                default:
                    data[i] = packetReadBuffer.readObject(PersistentData.class);
                    LogManager.logWarning("Incoming persistent data doesn't have a specific type: " + data[i].toString(), null);
                    break;
            }
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeInt(modType);
        packetWriteBuffer.writeInt(data.length);
        packetWriteBuffer.writeObject(data);
    }

    @Override
    public void processPacketOnClient() {
        NetworkSyncManager.processSyncData(modType, data);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
