package thederpgamer.betterfactions.manager.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionMember;
import thederpgamer.betterfactions.data.faction.FactionRank;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.utils.DataUtils;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/16/2022]
 */
public class FactionRankManager extends DataManager<FactionRank> {

	public static FactionRankManager instance;

	private final LoadingCache<Integer, FactionRank> cache;

	public FactionRankManager() {
		instance = this;
		cache = CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS).build(new CacheLoader<Integer, FactionRank>() {
			@Override
			public FactionRank load(Integer key) {
				if(NetworkUtils.onServer()) return loadData(key.intValue());
				else {
					requestDataFromServer(key.intValue());
					return null;
				}
			}
		});
	}

	@Override
	public LoadingCache<Integer, FactionRank> getCache() {
		return cache;
	}

	@Override
	public Class<FactionRank> getType() {
		return FactionRank.class;
	}

	@Override
	public FactionRank loadData(int id) {
		if(NetworkUtils.onServer()) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + id + ".smdat");
			if(dataFile.exists()) {
				try {
					return new FactionRank(new PacketReadBuffer(new DataInputStream(Files.newInputStream(dataFile.toPath()))));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			} else return createNewData(id);
		}
		return null;
	}

	@Override
	public FactionRank createNewData(Object... from) {
		FactionRank rank = null;
		try {
			if(from.length == 3) rank = new FactionRank((String) from[0], (Integer) from[1], (String[]) from[2]);
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FederationData", exception);
		}
		if(rank != null) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + rank.getId() + ".smdat");
			try {
				dataFile.createNewFile();
				rank.serialize(new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath()))));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return rank;
	}

	@Override
	public void putIntoClientCache(SerializationInterface data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (FactionRank) data);
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
			File dir = new File(DataUtils.getWorldDataPath() + "/data/factionRanks");
			if(!dir.exists()) dir.mkdirs();
			return dir;
		} else return null;
	}
}
