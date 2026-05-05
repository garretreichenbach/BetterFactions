package videogoose.betterfactions.utils;

import api.common.GameClient;
import api.common.GameCommon;
import videogoose.betterfactions.BetterFactions;


/**
 * Utility class for data paths and resource locations.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class DataUtils {

	public static String getResourcesPath() {
		return BetterFactions.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}

	public static String getWorldDataPath() {
		String universeName = GameCommon.getUniqueContextId();
		if (!universeName.contains(":")) return getResourcesPath() + "/data/" + universeName;
		try {
			BetterFactions.getInstance().logWarning("Client " + GameClient.getClientPlayerState().getName() + " attempted to illegally access server data.");
		} catch (Exception ignored) { }
		return null;
	}
}