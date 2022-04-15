package thederpgamer.betterfactions.manager;

import api.mod.config.PersistentObjectUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import thederpgamer.betterfactions.data.PersistentData;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/12/2022]
 */
public class FederationManager extends DataManager<Federation> {

	public static FederationManager instance;

	private final LoadingCache<Integer, Federation> cache;

	public FederationManager() {
		instance = this;
		cache = CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS).build(new CacheLoader<Integer, Federation>() {
			@Override
			public Federation load(Integer key) {
				if(NetworkUtils.onServer()) return loadData(key.intValue());
				else {
					requestDataFromServer(key.intValue());
					return null;
				}
			}
		});
	}

	@Override
	public LoadingCache<Integer, Federation> getCache() {
		return cache;
	}

	@Override
	public Class<Federation> getType() {
		return Federation.class;
	}

	@Override
	public Federation loadData(int id) {
		if(NetworkUtils.onServer()) {
			ArrayList<Object> dataList = PersistentObjectUtil.getObjects(modInstance, Federation.class);
			for(Object obj : dataList) if(((Federation) obj).getId() == id) return (Federation) obj;
			return createNewData(id);
		}
		return null;
	}

	@Override
	public Federation createNewData(Object... from) {
		Federation federationData = null;
		try {
			if(from.length == 3) {
				String name = (String) from[0];
				FactionData faction1 = (FactionData) ((from[1] instanceof FactionData) ? from[1] : FactionDataManager.instance.getCache().get(Integer.parseInt((String) from[1])));
				FactionData faction2 = (FactionData) ((from[2] instanceof FactionData) ? from[2] : FactionDataManager.instance.getCache().get(Integer.parseInt((String) from[2])));
				if(!canFormFederation(faction1, faction2)) throw new IllegalArgumentException("Faction " + faction1.getName() + " can't form a federation with itself!");
				else federationData = new Federation(generateNewId(), name, faction1, faction2);
			}
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FederationData", exception);
		}
		if(federationData != null) PersistentObjectUtil.addObject(modInstance, federationData);
		PersistentObjectUtil.save(modInstance);
		return federationData;
	}

	@Override
	public void putIntoClientCache(PersistentData data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (Federation) data);
	}

	public boolean canFormFederation(FactionData faction1, FactionData faction2) {
		if(!faction1.equals(faction2))
	}
}
