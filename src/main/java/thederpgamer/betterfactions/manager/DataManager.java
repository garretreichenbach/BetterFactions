package thederpgamer.betterfactions.manager;

import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import com.google.common.cache.LoadingCache;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.PersistentData;
import thederpgamer.betterfactions.network.client.RequestDataPacket;
import thederpgamer.betterfactions.network.server.SendDataPacket;
import thederpgamer.betterfactions.utils.NetworkUtils;

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

	protected final ModSkeleton modInstance = BetterFactions.getInstance().getSkeleton();

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
	 * Loads the data from persistent storage.
	 * (Server only)
	 * @param id The id of the data to load
	 * @return The data from persistent storage
	 */
	public abstract E loadData(int id);

	/**
	 * Creates a new instance of the data type using the provided parameters and adds it to persistent storage.
	 * (Server only)
	 * @param from Parameters for making the data object
	 * @return The new data object
	 */
	public abstract E createNewData(Object... from);

	public abstract void putIntoClientCache(PersistentData data);

	public int generateNewId() {
		if(NetworkUtils.onServer()) return PersistentObjectUtil.getObjects(modInstance, getType()).size();
		else throw new IllegalStateException("Clients are not allowed to generate new ids!");
	}

	public void sendDataToClient(int id, PlayerState playerState) {
		try {
			PacketUtil.sendPacket(playerState, new SendDataPacket(getCache().get(id)));
		} catch(ExecutionException e) {
			e.printStackTrace();
		}
	}

	public static DataManager<? extends SerializationInterface> getInstance(Class<? extends SerializationInterface> type) {
		if(type.equals(FactionData.class)) return FactionDataManager.instance;
		else return null;
	}
}