package thederpgamer.betterfactions.utils;

import api.common.GameCommon;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class NetworkUtils {

	public static boolean onServer() {
		return GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer();
	}

	/**
	 * Checks if the game state is a client connected to a dedicated or local server (single player).
	 * @return If the game state is a client
	 */
	public static boolean onClient() {
		return GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer();
	}
}
