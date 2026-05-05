package videogoose.betterfactions.manager.cache;

import videogoose.betterfactions.manager.UpdateManager;


public interface CacheInterface {
	void updateCache(UpdateManager.UpdateType updateType, Object... args);
}
