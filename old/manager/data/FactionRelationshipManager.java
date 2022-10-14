package thederpgamer.betterfactions.manager.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRelationship;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.utils.DataUtils;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
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
			if(from.length == 2) {
				FactionData faction1 = (FactionData) ((from[0] instanceof FactionData) ? from[0] : FactionDataManager.instance.getCache().get(Integer.parseInt((String) from[0])));
				FactionData faction2 = (FactionData) ((from[1] instanceof FactionData) ? from[1] : FactionDataManager.instance.getCache().get(Integer.parseInt((String) from[1])));
				relation = new FactionRelationship(faction1, faction2);
			}
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FactionRelationship", exception);
		}
		if(relation != null) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + relation.getId() + ".smdat");
			try {
				dataFile.createNewFile();
				relation.serialize(new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath()))));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return relation;
	}

	@Override
	public void putIntoClientCache(SerializationInterface data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (FactionRelationship) data);
	}

	@Override
	public int generateNewId() {
		try {
			return Objects.requireNonNull(getDataStorageDirectory().listFiles()).length;
		} catch(Exception ignored) { }
		return 1;
	}

	@Override
	public File getDataStorageDirectory() {
		if(NetworkUtils.onServer()) {
			File dir = new File(DataUtils.getWorldDataPath() + "/data/relation");
			if(!dir.exists()) dir.mkdirs();
			return dir;
		} else return null;
	}

	@Override
	public void initialize() {

	}

	public FactionRelationship getRelationship(int id) {
		try {
			return cache.get(id);
		} catch(ExecutionException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public FactionRelationship getRelationship(FactionData factionFrom, FactionData factionTo) {
		HashMap<FactionData, FactionRelationship> relationshipMap = getRelationships(factionFrom);
		if(!relationshipMap.containsKey(factionTo)) {
			FactionRelationship newRelation = new FactionRelationship(factionFrom, factionTo);
			relationshipMap.put(factionTo, newRelation);
			cache.put(newRelation.getId(), newRelation);
		}
		return relationshipMap.get(factionTo);
	}

	public HashMap<FactionData, FactionRelationship> getRelationships(FactionData factionData) {
		HashMap<FactionData, FactionRelationship> map = new HashMap<>();
		for(FactionRelationship relationship : getCache().asMap().values()) {
			if(relationship.getSelf().equals(factionData)) map.put(relationship.getOther(), relationship);
		}
		return map;
	}
}
