package thederpgamer.betterfactions.data.news;

import org.schema.common.util.linAlg.Vector3i;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public abstract class FactionNewsEventSystem extends FactionNewsEvent {

	public Vector3i system = new Vector3i();

	@Override
	public void serialize(DataOutput b, boolean isOnServer) throws IOException {
		super.serialize(b, isOnServer);
		system.serialize(b);
	}

	@Override
	public void deserialize(DataInput b, int updateSenderStateId, boolean isOnServer) throws IOException {
		super.deserialize(b, updateSenderStateId, isOnServer);
		system.deserialize(b);
	}
}
