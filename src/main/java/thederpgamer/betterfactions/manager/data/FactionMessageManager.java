package thederpgamer.betterfactions.manager.data;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.old.federation.FactionMessage;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/16/2022]
 */
public class FactionMessageManager extends DataManager<FactionMessage> {

	public static FactionMessageManager instance;

	private final LoadingCache<Integer, FactionMessage> cache;

	public FactionMessageManager() {
		instance = this;
		cache = CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS).build(
				new CacheLoader<Integer, FactionMessage>() {
					@Override
					public FactionMessage load(Integer key) {
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
	public LoadingCache<Integer, FactionMessage> getCache() {
		return cache;
	}

	@Override
	public Class<FactionMessage> getType() {
		return FactionMessage.class;
	}

	@Override
	public FactionMessage loadData(int id) {
		if(NetworkUtils.onServer()) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + id + ".smdat");
			if(dataFile.exists()) {
				try {
					return new FactionMessage(new PacketReadBuffer(new DataInputStream(Files.newInputStream(dataFile.toPath()))));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			} else return createNewData(id);
		}
		return null;
	}

	@Override
	public FactionMessage createNewData(Object... from) {
		FactionMessage message = null;
		try {
			if(from.length == 4) message = new FactionMessage((Faction) from[0], (Faction) from[1], (String) from[2], (String) from[3]);
			else if(from.length == 5) message = new FactionMessage((Faction) from[0], (Faction) from[1], (String) from[2], (String) from[3], (FactionMessage.MessageType) from[4]);
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FactionData", exception);
		}
		if(message != null) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + message.getId() + ".smdat");
			try {
				dataFile.createNewFile();
				message.serialize(new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath()))));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return message;
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
