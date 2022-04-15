package thederpgamer.betterfactions.network.client;

import api.common.GameCommon;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.old.federation.FactionMessage;
import thederpgamer.betterfactions.manager.FactionManagerOld;
import thederpgamer.betterfactions.utils.FactionMessageUtils;

import java.io.IOException;

/**
 * Sends a Faction Message to server.
 * <p>[CLIENT] -> [SERVER]<p/>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/21/2021]
 */
public class SendFactionMessagePacket extends Packet {

    private int fromId;
    private int toId;
    private String title;
    private String message;
    private String messageType;

    public SendFactionMessagePacket() {

    }

    public SendFactionMessagePacket(FactionMessage factionMessage) {
        fromId = factionMessage.fromId;
        toId = factionMessage.toId;
        title = factionMessage.title;
        message = factionMessage.message;
        messageType = factionMessage.messageType.toString();
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        fromId = packetReadBuffer.readInt();
        toId = packetReadBuffer.readInt();
        title = packetReadBuffer.readString();
        message = packetReadBuffer.readString();
        messageType = packetReadBuffer.readString();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeInt(fromId);
        packetWriteBuffer.writeInt(toId);
        packetWriteBuffer.writeString(title);
        packetWriteBuffer.writeString(message);
        packetWriteBuffer.writeString(messageType);
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        Faction from = GameCommon.getGameState().getFactionManager().getFaction(fromId);
        Faction to = GameCommon.getGameState().getFactionManager().getFaction(toId);
        if(org.schema.game.common.data.player.faction.FactionManager.isNPCFactionOrPirateOrTrader(toId)) {
            String response = FactionMessageUtils.getResponseMessage(FactionMessage.MessageType.valueOf(messageType), to, from);
            FactionMessage factionMessage = new FactionMessage(to, from, "Reply from " + to.getName(), response, FactionMessage.MessageType.REPLY);
            FactionManagerOld.getFactionData(from).addMessage(factionMessage);
        } else {
            FactionMessage factionMessage = new FactionMessage(from, to, title, message, FactionMessage.MessageType.valueOf(messageType));
            FactionManagerOld.getFactionData(to).addMessage(factionMessage);
        }
    }
}
