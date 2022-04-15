package thederpgamer.betterfactions.manager;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.PersistentData;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class FactionDataManager extends DataManager<FactionData> {

	public static FactionDataManager instance;

	private final LoadingCache<Integer, FactionData> cache;

	public FactionDataManager() {
		instance = this;
		cache = CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS).build(
				new CacheLoader<Integer, FactionData>() {
					@Override
					public FactionData load(Integer key) {
						if(NetworkUtils.onServer()) return loadData(key.intValue());
						else {
							requestDataFromServer(key.intValue());
							return null;
						}
					}
				}
		);
	}

	@Override
	public LoadingCache<Integer, FactionData> getCache() {
		return cache;
	}

	@Override
	public Class<FactionData> getType() {
		return FactionData.class;
	}

	@Override
	public FactionData loadData(int id) {
		if(NetworkUtils.onServer()) {
			ArrayList<Object> dataList = PersistentObjectUtil.getObjects(modInstance, FactionData.class);
			for(Object obj : dataList) if(((FactionData) obj).getId() == id) return (FactionData) obj;
			return createNewData(id);
		}
		return null;
	}

	@Override
	public FactionData createNewData(Object... from) {
		FactionData factionData = null;
		try {
			if(from.length == 1) {
				if(from[0] instanceof Faction) factionData = new FactionData((Faction) from[0]);
				else if(from[0] instanceof PlayerState) factionData = new FactionData(GameCommon.getGameState().getFactionManager().getFaction(((PlayerState) from[0]).getFactionId()));
				else factionData = new FactionData(GameCommon.getGameState().getFactionManager().getFaction(Integer.parseInt((String) from[0])));
			} else if(from.length == 2) factionData = new FactionData(Integer.parseInt((String) from[0]), (String) from[1]);
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FactionData", exception);
		}
		if(factionData != null) PersistentObjectUtil.addObject(modInstance, factionData);
		PersistentObjectUtil.save(modInstance);
		return factionData;
	}

	@Override
	public int generateNewId() {
		//Faction ids are assigned by the game itself and FactionData objects simply copy the ids of their Faction equivalents
		throw new UnsupportedOperationException("FactionDataManager is not allowed to generate new ids!");
	}

	@Override
	public void putIntoClientCache(PersistentData data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (FactionData) data);
	}

	public FactionData getPlayerFaction(PlayerState playerState) {
		if(playerState.getFactionId() > 0) {
			try {
				return cache.get(playerState.getFactionId());
			} catch(ExecutionException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
