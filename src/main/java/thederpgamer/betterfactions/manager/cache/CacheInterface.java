package thederpgamer.betterfactions.manager.cache;

import thederpgamer.betterfactions.manager.UpdateManager;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public interface CacheInterface {
	void updateCache(UpdateManager.UpdateType updateType, Object... args);
}
