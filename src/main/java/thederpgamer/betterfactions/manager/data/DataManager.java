package thederpgamer.betterfactions.manager.data;

import api.common.GameClient;
import api.common.GameCommon;
import api.common.GameServer;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import com.google.common.cache.LoadingCache;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionMember;
import thederpgamer.betterfactions.data.faction.FactionRank;
import thederpgamer.betterfactions.data.faction.FactionRelationship;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.data.old.federation.FactionMessage;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.network.client.RequestDataPacket;
import thederpgamer.betterfactions.network.server.SendDataPacket;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public abstract class DataManager<E extends SerializationInterface> {

	public static final long CACHE_MAX_SIZE = 50;
	public static final long CACHE_EXPIRE_TIME = 300000;

	/**
	 * Sends a packet to the server requesting data matching the provided id.
	 * (Client only)
	 * @param id The id of the data to request
	 */
	public void requestDataFromServer(int id) {
		if(NetworkUtils.onClient()) PacketUtil.sendPacketToServer(new RequestDataPacket(getType(), id));
		else LogManager.logWarning("Server can't request data from itself!", null);
	}

	public boolean exists(int id) {
		return getCache().asMap().containsKey(id);
	}

	public abstract LoadingCache<Integer, E> getCache();

	/**
	 * Returns the type (class) of data this manager holds.
	 * @return The manager's data type
	 */
	public abstract Class<E> getType();

	/**
	 * Loads the data from storage.
	 * (Server only)
	 * @param id The id of the data to load
	 * @return The data from storage
	 */
	public abstract E loadData(int id);

	/**
	 * Creates a new instance of the data type using the provided parameters and adds it to data storage.
	 * (Server only)
	 * @param from Parameters for making the data object
	 * @return The new data object
	 */
	public abstract E createNewData(Object... from);

	public abstract void putIntoClientCache(SerializationInterface data);

	public abstract int generateNewId();

	/**
	 * Returns the directory used to store data of this type and creates it if it doesn't exit.
	 * (Server only)
	 * @return The data storage directory
	 */
	public abstract File getDataStorageDirectory();

	public void sendDataToClient(int id, PlayerState playerState) {
		try {
			PacketUtil.sendPacket(playerState, new SendDataPacket(getCache().get(id)));
		} catch(ExecutionException e) {
			e.printStackTrace();
		}
	}

	public void sendDataToClients(SerializationInterface data) {
		if(!NetworkUtils.onServer()) return;
		for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) PacketUtil.sendPacket(playerState, new SendDataPacket(data));
	}

	public void sendAllDataToClients() {
		for(SerializationInterface data : getCache().asMap().values()) sendDataToClients(data);
	}

	public void saveData(SerializationInterface data) {
		if(NetworkUtils.onServer()) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + data.getId() + ".smdat");
			try {
				if(dataFile.exists()) dataFile.delete();
				dataFile.createNewFile();
				PacketWriteBuffer writeBuffer = new PacketWriteBuffer(new DataOutputStream(Files.newOutputStream(dataFile.toPath())));
				data.serialize(writeBuffer);
			} catch(IOException exception) {
				throw new RuntimeException(exception);
			}
		}
	}

	public void removeData(SerializationInterface data) {
		if(NetworkUtils.onServer()) {
			File storageDir = getDataStorageDirectory();
			File dataFile = new File(storageDir.getPath() + "/" + data.getId() + ".smdat");
			if(dataFile.exists()) dataFile.delete();
			getCache().invalidate(data);
		}
	}

	public void updateData(SerializationInterface data) {
		if(NetworkUtils.onServer()) {
			getCache().invalidate(data.getId());
			getCache().put(data.getId(), (E) data);
			sendDataToClients(data);
		}
	}

	public static void initializeManagers() {
		new FactionDataManager();
		new FactionRelationshipManager();
		new FederationManager();
		new FactionMemberManager();
		new FactionRankManager();
		new FactionMessageManager();
	}

	public static DataManager<? extends SerializationInterface> getInstance(Class<? extends SerializationInterface> type) {
		if(type.equals(FactionData.class)) return FactionDataManager.instance;
		else if(type.equals(FactionRelationship.class)) return FactionRelationshipManager.instance;
		else if(type.equals(Federation.class)) return FederationManager.instance;
		else if(type.equals(FactionMember.class)) return FactionMemberManager.instance;
		else if(type.equals(FactionRank.class)) return FactionRankManager.instance;
		else if(type.equals(FactionMessage.class)) return FactionMessageManager.instance;
		else return null;
	}
}