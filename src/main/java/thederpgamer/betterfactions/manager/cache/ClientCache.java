package thederpgamer.betterfactions.manager.cache;

import api.utils.other.HashList;
import thederpgamer.betterfactions.data.diplomacy.war.WarData;
import thederpgamer.betterfactions.manager.UpdateManager;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ClientCache implements CacheInterface {

	private final HashList<Class<?>, Object> cache = new HashList<>();

	@Override
	public void updateCache(UpdateManager.UpdateType updateType, Object... args) {
		ArrayList<Object> objects;
		switch(updateType) {
			case UPDATE_WAR_DATA:
				objects = cache.get(WarData.class);
				WarData warData = (WarData) args[0];
				for(Object obj : objects) {
					WarData data = (WarData) obj;
					if(data.getId() == warData.getId()) {
						removeObject(data);
						addObject(warData);
						break;
					}
				}
		}
	}

	public ArrayList<Object> getObjects(Class<?> type) {
		return cache.get(type);
	}

	public void addObject(Object object) {
		cache.add(object.getClass(), object);
	}

	public void removeObject(Object object) {
		cache.getList(object.getClass()).remove(object);
	}
}
