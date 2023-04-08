package thederpgamer.betterfactions.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.manager.UpdateManager;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ServerUpdatePacket extends Packet {

	private UpdateManager.UpdateType updateType;
	private Object[] args;

	public ServerUpdatePacket() {

	}

	public ServerUpdatePacket(UpdateManager.UpdateType updateType, Object... args) {
		this.updateType = updateType;
		this.args = args;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		updateType = UpdateManager.UpdateType.values()[packetReadBuffer.readInt()];
		args = new Object[packetReadBuffer.readInt()];
		for(int i = 0; i < args.length; i ++) {
			try {
				args[i] = packetReadBuffer.readObject(Class.forName(packetReadBuffer.readString()));
			} catch(ClassNotFoundException exception) {
				throw new RuntimeException(exception);
			}
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(updateType.ordinal());
		packetWriteBuffer.writeInt(args.length);
		for(Object arg : args) {
			packetWriteBuffer.writeString(arg.getClass().getName());
			packetWriteBuffer.writeObject(arg);
		}
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		switch(updateType) {

		}
	}
}
