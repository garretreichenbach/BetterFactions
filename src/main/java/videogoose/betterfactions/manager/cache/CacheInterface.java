package videogoose.betterfactions.manager.cache;

import videogoose.betterfactions.manager.UpdateManager;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public interface CacheInterface {
	void updateCache(UpdateManager.UpdateType updateType, Object... args);
}
