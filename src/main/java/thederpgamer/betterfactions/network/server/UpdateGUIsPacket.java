package thederpgamer.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.BetterFactions;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/14/2021]
 */
public class UpdateGUIsPacket extends Packet {

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {

    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {

    }

    @Override
    public void processPacketOnClient() {
        BetterFactions.getInstance().newFactionPanel.updateTabs();
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
