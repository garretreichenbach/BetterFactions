package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public abstract class FactionNewsEventOtherEnt extends FactionNewsEvent {

	public String otherEnt;

	@Override
	public void serialize(DataOutput b, boolean isOnServer) throws IOException {
		super.serialize(b, isOnServer);
		b.writeUTF(otherEnt);
	}

	@Override
	public void deserialize(DataInput b, int updateSenderStateId, boolean isOnServer) throws IOException {
		super.deserialize(b, updateSenderStateId, isOnServer);
		otherEnt = b.readUTF();
	}

	public String getOtherName(FactionState state){
		return otherEnt;
	}
}
