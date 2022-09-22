package thederpgamer.betterfactions.manager.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.diplomacy.DiplomaticData;
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
 * @version 1.0 - [05/17/2022]
 */
public class DiplomaticDataManager extends DataManager<DiplomaticData> {

	public static DiplomaticDataManager instance;

	private final LoadingCache<Integer, DiplomaticData> cache;

	public DiplomaticDataManager() {
		instance = this;
		cache = CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS).build(new CacheLoader<Integer, DiplomaticData>() {
			@Override
			public DiplomaticData load(Integer key) {
				if(NetworkUtils.onServer()) return loadData(key.intValue());
				else {
					requestDataFromServer(key.intValue());
					return null;
				}
			}
		});
	}


	@Override
	public LoadingCache<Integer, DiplomaticData> getCache() {
		return cache;
	}

	@Override
	public Class<DiplomaticData> getType() {
		return DiplomaticData.class;
	}

	@Override
	public DiplomaticData loadData(int id) {
		if(NetworkUtils.onServer()) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + id + ".smdat");
			if(dataFile.exists()) {
				try {
					return DiplomaticData.read(new PacketReadBuffer(new DataInputStream(Files.newInputStream(dataFile.toPath()))));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			} else return createNewData(id);
		}
		return null;
	}

	@Override
	public DiplomaticData createNewData(Object... from) {
		DiplomaticData data = null;
		try {

		} catch(Exception exception) {
			LogManager.logException("Failed to create new DiplomaticData", exception);
		}
		if(data != null) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + data.getId() + ".smdat");
			try {
				dataFile.createNewFile();
				data.serialize(new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath()))));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return data;	}

	@Override
	public void putIntoClientCache(SerializationInterface data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (DiplomaticData) data);
	}

	@Override
	public int generateNewId() {
		try {
			return Objects.requireNonNull(getDataStorageDirectory().listFiles()).length;
		} catch(Exception ignored) { }
		return 1;	}

	@Override
	public File getDataStorageDirectory() {
		if(NetworkUtils.onServer()) {
			File dir = new File(DataUtils.getWorldDataPath() + "/data/diplomaticData");
			if(!dir.exists()) dir.mkdirs();
			return dir;
		} else return null;
	}

	@Override
	public void initialize() {

	}
}
