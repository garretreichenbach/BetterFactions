package thederpgamer.betterfactions.manager.data;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.utils.DataUtils;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.io.*;
import java.nio.file.Files;
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
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + id + ".smdat");
			if(dataFile.exists()) {
				try {
					return new FactionData(new PacketReadBuffer(new DataInputStream(Files.newInputStream(dataFile.toPath()))));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			} else return createNewData(id);
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
				else factionData = new FactionData(GameCommon.getGameState().getFactionManager().getFaction((Integer) from[0]));
			} else if(from.length == 2) factionData = new FactionData(Integer.parseInt((String) from[0]), (String) from[1]);
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FactionData", exception);
		}
		if(factionData != null) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + factionData.getId() + ".smdat");
			try {
				dataFile.createNewFile();
				factionData.serialize(new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath()))));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return factionData;
	}

	@Override
	public int generateNewId() {
		//Faction ids are assigned by the game itself and FactionData objects simply copy the ids of their Faction equivalents
		throw new UnsupportedOperationException("FactionDataManager is not allowed to generate new ids!");
	}

	@Override
	public File getDataStorageDirectory() {
		if(NetworkUtils.onServer()) {
			File dir = new File(DataUtils.getWorldDataPath() + "/data/faction");
			if(!dir.exists()) dir.mkdirs();
			return dir;
		} else return null;
	}

	@Override
	public void putIntoClientCache(SerializationInterface data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (FactionData) data);
	}

	@Override
	public void initialize() {
		if(!cache.asMap().containsKey(FactionManager.TRAIDING_GUILD_ID)) {
			FactionData traders = new FactionData(GameCommon.getGameState().getFactionManager().getFaction(FactionManager.TRAIDING_GUILD_ID));
			traders.setFactionLogo("traders-logo");
			cache.put(FactionManager.TRAIDING_GUILD_ID, traders);
			saveData(traders);
		}

		if(!cache.asMap().containsKey(FactionManager.PIRATES_ID)) {
			FactionData pirates = new FactionData(GameCommon.getGameState().getFactionManager().getFaction(FactionManager.PIRATES_ID));
			pirates.setFactionLogo("pirates-logo");
			cache.put(FactionManager.PIRATES_ID, pirates);
			saveData(pirates);
		}

		for(Faction faction : GameCommon.getGameState().getFactionManager().getFactionCollection()) {
			if(!cache.asMap().containsKey(faction.getIdFaction()) && faction.getIdFaction() > 0) {
				FactionData data = new FactionData(faction);
				cache.put(faction.getIdFaction(), data);
				saveData(data);
			}
		}
	}

	public FactionData getFactionData(int id) {
		try {
			return cache.get(id);
		} catch(ExecutionException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public FactionData getPlayerFaction(PlayerState playerState) {
		if(playerState.getFactionId() > 0) {
			try {
				if(!cache.asMap().containsKey(playerState.getFactionId())) createNewData(GameCommon.getGameState().getFactionManager().getFaction(playerState.getFactionId()));
				return cache.get(playerState.getFactionId());
			} catch(ExecutionException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
