package thederpgamer.betterfactions.manager.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
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
public class FactionMemberManager extends DataManager<FactionMember> {

	public static FactionMemberManager instance;

	private final LoadingCache<Integer, FactionMember> cache;

	public FactionMemberManager() {
		instance = this;
		cache = CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS).build(new CacheLoader<Integer, FactionMember>() {
			@Override
			public FactionMember load(Integer key) {
				if(NetworkUtils.onServer()) return loadData(key.intValue());
				else {
					requestDataFromServer(key.intValue());
					return null;
				}
			}
		});
	}

	@Override
	public LoadingCache<Integer, FactionMember> getCache() {
		return cache;
	}

	@Override
	public Class<FactionMember> getType() {
		return FactionMember.class;
	}

	@Override
	public FactionMember loadData(int id) {
		if(NetworkUtils.onServer()) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + id + ".smdat");
			if(dataFile.exists()) {
				try {
					return new FactionMember(new PacketReadBuffer(new DataInputStream(Files.newInputStream(dataFile.toPath()))));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			} else return createNewData(id);
		}
		return null;
	}

	@Override
	public FactionMember createNewData(Object... from) {
		FactionMember factionMember = null;
		try {
			if(from.length == 2) {
				if(from[1] instanceof FactionData) factionMember = new FactionMember((String) from[0], (FactionData) from[1]);
				else factionMember = new FactionMember((String) from[0], FactionDataManager.instance.getFactionData((Integer) from[1]));
			}
		} catch(Exception exception) {
			LogManager.logException("Failed to create new FederationData", exception);
		}
		if(factionMember != null) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + factionMember.getId() + ".smdat");
			try {
				dataFile.createNewFile();
				factionMember.serialize(new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath()))));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return factionMember;
	}

	@Override
	public void putIntoClientCache(SerializationInterface data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (FactionMember) data);
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
			File dir = new File(DataUtils.getWorldDataPath() + "/data/factionMembers");
			if(!dir.exists()) dir.mkdirs();
			return dir;
		} else return null;
	}

	@Override
	public void initialize() {
		for(FactionData factionData : FactionDataManager.instance.getCache().asMap().values()) {
			for(String name : factionData.getFaction().getMembersUID().keySet()) {
				if(getMember(factionData, name) == null) {
					FactionMember member = new FactionMember(name, factionData);
					cache.put(generateNewId(), member);
					saveData(member);
				}
			}
		}
	}

	public FactionMember getMember(FactionData factionData, String name) {
		for(FactionMember member : cache.asMap().values()) {
			if(member.getName().equals(name) && member.getFactionId() == factionData.getId()) return member;
		}
		FactionMember member;
		if(factionData.members.size() == 1) member = new FactionMember(name, factionData, new FactionRank("Founder", 4 , "*"));
		else member = new FactionMember(name, factionData);
		factionData.addMember(name);
		saveData(member);
		return member;
	}

	public FactionMember getMember(FactionData factionData, PlayerState playerState) {
		return getMember(factionData, playerState.getName());
	}
}
