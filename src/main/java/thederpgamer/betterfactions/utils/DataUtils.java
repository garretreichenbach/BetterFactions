package thederpgamer.betterfactions.utils;

import api.common.GameClient;
import api.common.GameCommon;
import thederpgamer.betterfactions.BetterFactions;

import java.util.logging.Level;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class DataUtils {

	public static String getResourcesPath() {
		return BetterFactions.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}

	public static String getWorldDataPath() {
		String universeName = GameCommon.getUniqueContextId();
		if(!universeName.contains(":")) return getResourcesPath() + "/data/" + universeName;
		else {
			try {
				BetterFactions.log.log(Level.WARNING,"Client " + GameClient.getClientPlayerState().getName() + " attempted to illegally access server data.");
			} catch(Exception ignored) { }
			return null;
		}
	}
}