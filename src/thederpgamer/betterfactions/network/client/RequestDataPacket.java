package thederpgamer.betterfactions.network.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.persistent.PersistentData;
import thederpgamer.betterfactions.manager.DataManager;

import java.io.IOException;

/**
 * Requests data from the server by id.
 * [CLIENT] -> [SERVER]
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/09/2022]
 */
public class RequestDataPacket extends Packet {

	private Class<? extends PersistentData> type;
	private int id;

	public RequestDataPacket() {

	}

	public RequestDataPacket(Class<? extends PersistentData> type, int id) {
		this.type = type;
		this.id = id;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		try {
			type = (Class<? extends PersistentData>) Class.forName(packetReadBuffer.readString());
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		}
		id = packetReadBuffer.readInt();
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
