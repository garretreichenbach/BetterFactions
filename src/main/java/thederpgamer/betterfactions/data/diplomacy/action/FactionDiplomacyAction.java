package thederpgamer.betterfactions.data.diplomacy.action;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyAction;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class FactionDiplomacyAction extends DiplomacyAction {

	public void fromNetwork(PacketReadBuffer packetReadBuffer) throws IOException {
		type = DiplActionType.values()[packetReadBuffer.readInt()];
		counter = packetReadBuffer.readInt();
		timeDuration = packetReadBuffer.readLong();
	}

	public void toNetwork(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(type.ordinal());
		packetWriteBuffer.writeInt(counter);
		packetWriteBuffer.writeLong(timeDuration);
	}
}
