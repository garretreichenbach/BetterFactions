package thederpgamer.betterfactions.network.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.manager.FactionManager;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public class ModifyFactionMessagePacket extends Packet {

    private FactionMessage message;
    private int mode;

    public ModifyFactionMessagePacket() {

    }

    public ModifyFactionMessagePacket(FactionMessage message, int mode) {
        this.message = message;
        this.mode = mode;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        message = packetReadBuffer.readObject(FactionMessage.class);
        mode = packetReadBuffer.readInt();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeObject(message);
        packetWriteBuffer.writeInt(mode);
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        FactionData factionData = FactionManager.getFactionData(message.toId);
        switch(mode) {
            case FactionMessage.MARK_READ:
                message.read = true;
                FactionManager.updateData(message);
                break;
            case FactionMessage.MARK_UNREAD:
                message.read = false;
                FactionManager.updateData(message);
                break;
            case FactionMessage.DELETE:
                factionData.removeMessage(message);
                FactionManager.updateData(factionData);
                break;
        }
    }
}
