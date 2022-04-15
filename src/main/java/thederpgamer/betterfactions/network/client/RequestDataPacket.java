package thederpgamer.betterfactions.network.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.PersistentData;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.manager.DataManager;
import thederpgamer.betterfactions.manager.LogManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Requests data from the server by id.
 * [CLIENT] -> [SERVER]
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/09/2022]
 */
public class RequestDataPacket extends Packet {

	private Class<? extends SerializationInterface> type;
	private int id;

	public RequestDataPacket() {

	}

	public RequestDataPacket(Class<? extends SerializationInterface> type, int id) {
		this.type = type;
		this.id = id;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {

	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeString(type.getName());
		packetWriteBuffer.writeInt(id);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		DataManager.getInstance(type).sendDataToClient(id, playerState);
	}
}
