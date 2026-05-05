package videogoose.betterfactions.network;

import api.common.GameCommon;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacy;
import videogoose.betterfactions.manager.FactionDiplomacyManager;

import java.io.IOException;

/**
 * Syncs FactionDiplomacy data between server and clients.
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
		int factionId = packetReadBuffer.readInt();
		factionDiplomacy = new FactionDiplomacy(
			GameCommon.getGameState().getFactionManager().getFaction(factionId)
		);
		factionDiplomacy.fromNetwork(packetReadBuffer);
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(factionDiplomacy.faction.getIdFaction());
		factionDiplomacy.toNetwork(packetWriteBuffer);
	}

	@Override
	public void processPacketOnClient() {
		if (factionDiplomacy == null || factionDiplomacy.faction == null) return;
		int factionId = factionDiplomacy.faction.getIdFaction();
		FactionDiplomacyManager.invalidateCache(factionId);
		factionDiplomacy.onClientChanged();
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		if (factionDiplomacy == null || factionDiplomacy.faction == null) return;
		// Validate sender belongs to the faction
		if (playerState.getFactionId() != factionDiplomacy.faction.getIdFaction()) {
			BetterFactions.getInstance().logWarning(
				"Player " + playerState.getName() + " tried to send diplomacy data for faction " +
				factionDiplomacy.faction.getIdFaction() + " but belongs to faction " + playerState.getFactionId()
			);
			return;
		}
		int factionId = factionDiplomacy.faction.getIdFaction();
		FactionDiplomacyManager.invalidateCache(factionId);
		FactionDiplomacyManager.diplomacyChanged.add(factionDiplomacy);
	}
}
