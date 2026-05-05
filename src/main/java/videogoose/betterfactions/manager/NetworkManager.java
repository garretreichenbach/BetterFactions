package videogoose.betterfactions.manager;

import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacy;
import videogoose.betterfactions.network.FactionDiplomacyPacket;

/**
 * Handles sending network packets to players.
 */
public class NetworkManager {

	public static void sendToPlayer(PlayerState playerState, FactionDiplomacy factionDiplomacy) {
		PacketUtil.sendPacket(playerState, new FactionDiplomacyPacket(factionDiplomacy));
	}
}
