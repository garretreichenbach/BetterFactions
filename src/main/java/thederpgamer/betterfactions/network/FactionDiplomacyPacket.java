package thederpgamer.betterfactions.network;

import api.common.GameCommon;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacy;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class FactionDiplomacyPacket extends Packet {

	private FactionDiplomacy factionDiplomacy;

	public FactionDiplomacyPacket() {
	}

	public FactionDiplomacyPacket(FactionDiplomacy factionDiplomacy) {
		this.factionDiplomacy = factionDiplomacy;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		factionDiplomacy = new FactionDiplomacy(GameCommon.getGameState().getFactionManager().getFaction(packetReadBuffer.readInt()));
		factionDiplomacy.fromNetwork(packetReadBuffer);
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(factionDiplomacy.faction.getIdFaction());
		factionDiplomacy.toNetwork(packetWriteBuffer);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {

	}
}
