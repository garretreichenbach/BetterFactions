package videogoose.betterfactions.manager.cache;

import api.common.GameCommon;


public class CacheManager {

	private static CacheInterface cache;

	public static void initialize() {
		if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) cache = new ServerCache();
		else cache = new ClientCache();
	}

	public static CacheInterface getInstance() {
		return cache;
	}
}
