package thederpgamer.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.serializeable.FactionEntityData;
import thederpgamer.betterfactions.manager.ClientCacheManager;

import java.io.IOException;
import java.util.ArrayList;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public class UpdateClientCachePacket extends Packet {

    private ArrayList<FactionEntityData> factionAssets;

    public UpdateClientCachePacket() {
        this.factionAssets = new ArrayList<>();
    }

    public UpdateClientCachePacket(ArrayList<FactionEntityData> factionAssets) {
        this.factionAssets = factionAssets;
    }

    @Override
    public void readPacketData(PacketReadBuffer readBuffer) throws IOException {
        int size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) factionAssets.add(new FactionEntityData(readBuffer));
    }

    @Override
    public void writePacketData(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(factionAssets.size());
        for(FactionEntityData entityData : factionAssets) entityData.serialize(writeBuffer);
    }

    @Override
    public void processPacketOnClient() {
        for(FactionEntityData entityData : factionAssets) ClientCacheManager.factionAssets.put(entityData.name, entityData);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
