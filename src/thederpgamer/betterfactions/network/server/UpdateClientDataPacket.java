package thederpgamer.betterfactions.network.server;

import api.common.GameCommon;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import java.io.IOException;
import java.lang.reflect.Array;
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

    private boolean isServer = GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer();
    private HashMap<Class, ArrayList<Object>> dataMap;

    public UpdateClientDataPacket() { }

    public UpdateClientDataPacket(HashMap<Class, ArrayList<Object>> dataMap) {
        if(isServer) this.dataMap = dataMap;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {

    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        for(Class type : dataMap.keySet()) {
            String[] s = type.getName().split("\\.");
            String typeName = s[s.length - 1];
            packetWriteBuffer.writeString("typeName + ");
        }
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
