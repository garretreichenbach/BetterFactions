package thederpgamer.betterfactions.manager;

import api.common.GameCommon;
import api.common.GameServer;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.network.ClientUpdatePacket;
import thederpgamer.betterfactions.network.ServerUpdatePacket;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class UpdateManager {

	public enum UpdateType {
		UPDATE_WAR_DATA
	}

	public static void sendUpdate(UpdateType updateType, Object... args) {
		if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
			for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) PacketUtil.sendPacket(playerState, new ServerUpdatePacket(updateType, args));
		} else PacketUtil.sendPacketToServer(new ClientUpdatePacket(updateType, args));
	}
}
