package videogoose.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import videogoose.betterfactions.data.persistent.PersistentData;
import videogoose.betterfactions.data.persistent.diplomacy.DiplomaticDataOld;
import videogoose.betterfactions.data.persistent.federation.FederationData;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.manager.NetworkSyncManager;

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
            data[i] = switch(dataType) {
                case NetworkSyncManager.FEDERATION_DATA -> packetReadBuffer.readObject(FederationData.class);
                case NetworkSyncManager.DIPLOMATIC_DATA -> packetReadBuffer.readObject(DiplomaticDataOld.class);
                default -> {
                    PersistentData fallback = packetReadBuffer.readObject(PersistentData.class);
                    BetterFactions.getInstance().logWarning("Incoming persistent data doesn't have a specific type: " + fallback);
                    yield fallback;
                }
            };
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeInt(modType);
        packetWriteBuffer.writeInt(data.length);
        for(PersistentData persistentData : data) {
            packetWriteBuffer.writeInt(persistentData.getDataType());
            packetWriteBuffer.writeObject(persistentData);
        }
    }

    @Override
    public void processPacketOnClient() {
        NetworkSyncManager.processSyncData(modType, data);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
