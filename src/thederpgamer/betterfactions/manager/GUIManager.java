package thederpgamer.betterfactions.manager;

import api.common.GameServer;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.network.server.UpdateGUIsPacket;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/14/2021]
 */
public class GUIManager {

    public static void updateTabs() {
        if(GameServer.getServerState() != null) {
            for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                PacketUtil.sendPacket(playerState, new UpdateGUIsPacket());
            }
        }
    }
}
