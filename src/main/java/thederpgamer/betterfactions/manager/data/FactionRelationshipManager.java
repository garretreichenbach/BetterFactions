package thederpgamer.betterfactions.manager.data;

import api.network.PacketReadBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRelationship;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/18/2022]
 */
public class FactionRelationshipManager extends DataManager<FactionRelationship> {

	public static FactionRelationshipManager instance;

	private final LoadingCache<Integer, FactionRelationship> cache;

	public FactionRelationshipManager() {
		instance = this;
		cache = CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS).build(new CacheLoader<Integer, FactionRelationship>() {
			@Override
			public FactionRelationship load(Integer key) {
				if(NetworkUtils.onServer()) return loadData(key.intValue());
				else {
					requestDataFromServer(key.intValue());
					return null;
				}
			}
		});
	}

	@Override
	public LoadingCache<Integer, FactionRelationship> getCache() {
		return cache;
	}

	@Override
	public Class<FactionRelationship> getType() {
		return FactionRelationship.class;
	}

	@Override
	public FactionRelationship loadData(int id) {
		if(NetworkUtils.onServer()) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + id + ".smdat");
			if(dataFile.exists()) {
				try {
					return new FactionRelationship(new PacketReadBuffer(new DataInputStream(Files.newInputStream(dataFile.toPath()))));
				} catch(IOException exception) {
					throw new RuntimeException(exception);
				}
			} else return createNewData(id);
		}
		return null;
	}

	@Override
	public FactionRelationship createNewData(Object... from) {
		FactionRelationship relation = null;
		try {
			if(from.length == 3) {
				String name = (String) from[0];
				FactionData faction1 = (FactionData) ((from[1] instanceof FactionData) ? from[1] : FactionDataManager.instance.getCache().get(Integer.parseInt((String) from[1])));
				FactionData faction2 = (FactionData) ((from[2] instanceof FactionData) ? from[2] : FactionDataManager.instance.getCache().get(Integer.parseInt((String) from[2])));
				if(!canFormFederation(faction1, faction2)) throw new IllegalArgumentException("Faction " + faction1.getName() + " can't form a federation with itself!");
				else federationData = new Federation(generateNewId(), name, faction1, faction2);
			}
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FactionRelationship", exception);
		}
		return relation;
	}

	@Override
	public void putIntoClientCache(SerializationInterface data) {

	}

	@Override
	public int generateNewId() {
		return 0;
	}

	@Override
	public File getDataStorageDirectory() {
		return null;
	}
}
