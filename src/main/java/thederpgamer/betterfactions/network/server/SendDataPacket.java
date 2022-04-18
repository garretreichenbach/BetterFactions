package thederpgamer.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.manager.data.DataManager;
import thederpgamer.betterfactions.manager.LogManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Sends requested persistent data to the client.
 * [SERVER] -> [CLIENT]
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/09/2022]
 */
public class SendDataPacket extends Packet {

	private SerializationInterface data;

	public SendDataPacket() {

	}

	public SendDataPacket(SerializationInterface data) {
		this.data = data;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		Class<? extends SerializationInterface> type = null;
		try {
			type = (Class<? extends SerializationInterface>) Class.forName(packetReadBuffer.readString());
			data = type.getDeclaredConstructor(PacketReadBuffer.class).newInstance(packetReadBuffer);
		} catch(ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
			if(type != null) LogManager.logWarning("Failed to deserialize data object \"" + type.getName() + "\"", exception);
			else LogManager.logException("Cannot deserialize invalid data object", exception);
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {

	}

	@Override
	public void processPacketOnClient() {
		DataManager.getInstance(data.getClass()).putIntoClientCache(data);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {

	}
}
