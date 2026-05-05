package videogoose.betterfactions.network.client;

import api.common.GameCommon;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.manager.FederationManager;

import java.io.IOException;

/**
 * CreateNewFederationPacket.java
 * <Description>
 * ==================================================
 * Created 02/08/2021
 * [Client -> Server]
 * @author TheDerpGamer
 */
public class CreateNewFederationPacket extends Packet {

    private String federationName;
    private int fromFactionId;
    private int toFactionId;

    public CreateNewFederationPacket() {

    }

    public CreateNewFederationPacket(String federationName, Faction fromFaction, Faction toFaction) {
        this.federationName = federationName;
        this.fromFactionId = fromFaction.getIdFaction();
        this.toFactionId = toFaction.getIdFaction();
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            federationName = packetReadBuffer.readString();
            fromFactionId = packetReadBuffer.readInt();
            toFactionId = packetReadBuffer.readInt();
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        if(GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer()) {
            packetWriteBuffer.writeString(federationName);
            packetWriteBuffer.writeInt(fromFactionId);
            packetWriteBuffer.writeInt(toFactionId);
        }
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        Faction fromFaction = GameCommon.getGameState().getFactionManager().getFaction(fromFactionId);
        Faction toFaction = GameCommon.getGameState().getFactionManager().getFaction(toFactionId);
        if (fromFaction == null || toFaction == null) return;
        FederationManager.createNewFederation(federationName, fromFactionId, toFactionId);
    }
}
