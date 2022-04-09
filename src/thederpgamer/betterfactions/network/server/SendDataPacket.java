package thederpgamer.betterfactions.network.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.persistent.PersistentData;
import thederpgamer.betterfactions.manager.DataManager;

import java.io.IOException;

/**
 * Sends requested persistent data to the client.
 * [SERVER] -> [CLIENT]
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/09/2022]
 */
public class SendDataPacket extends Packet {

	private PersistentData data;

	public SendDataPacket() {

	}

	public SendDataPacket(PersistentData data) {
		this.data = data;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		data = packetReadBuffer.readObject(data.getClass());
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeObject(data);
	}

	@Override
	public void processPacketOnClient() {
		DataManager.getInstance(data.getClass()).putIntoClientCache(data);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {

	}
}
