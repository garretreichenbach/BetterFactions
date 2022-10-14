package thederpgamer.betterfactions.manager.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRelationship;
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
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + id + ".smdat");
			if(dataFile.exists()) {
				try {
					return new Federation(new PacketReadBuffer(new DataInputStream(Files.newInputStream(dataFile.toPath()))));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			} else return createNewData(id);
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
		if(federationData != null) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + federationData.getId() + ".smdat");
			try {
				dataFile.createNewFile();
				federationData.serialize(new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath()))));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		return federationData;
	}

	@Override
	public void putIntoClientCache(SerializationInterface data) {
		cache.invalidate(data.getId());
		cache.put(data.getId(), (Federation) data);
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
			File dir = new File(DataUtils.getWorldDataPath() + "/data/federation");
			if(!dir.exists()) dir.mkdirs();
			return dir;
		} else return null;
	}

	@Override
	public void initialize() {

	}

	public boolean isInFederation(FactionData factionData) {
		return getFederation(factionData) == null;
	}

	public Federation getFederation(FactionData factionData) {
		for(Federation federation : cache.asMap().values()) {
			if(federation.getMembers().containsKey(factionData.getId())) return federation;
		}
		return null;
	}

	public boolean canFormFederation(FactionData faction1, FactionData faction2) {
		if(!faction1.equals(faction2) && !isInFederation(faction1) && !isInFederation(faction2)) {
			FactionRelationship relationship = FactionRelationshipManager.instance.getRelationship(faction1, faction2);
			for(FactionRelationship.Relationship relation : relationship.getRelations()) {
				switch(relation.getRelationType()) {
					case SELF:
					case ENEMY:
					case AT_WAR:
					case FEDERATION_ALLY:
					case FEDERATION_MEMBERS: return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean federationExists(String name) {
		for(Federation federation : cache.asMap().values()) {
			if(federation.getName().equals(name)) return true;
		}
		return false;
	}
}
