package thederpgamer.betterfactions.data.news;

import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.SerialializationInterface;
import org.schema.schine.resource.tag.SerializableTagElement;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class FactionNewsEvent implements SerialializationInterface, SerializableTagElement{
	public long time;
	public int factionId;

	@Override
	public void serialize(DataOutput b, boolean isOnServer) throws IOException {
		b.writeLong(time);
		b.writeInt(factionId);
	}

	@Override
	public void deserialize(DataInput b, int updateSenderStateId, boolean isOnServer) throws IOException {
		time = b.readLong();
		factionId = b.readInt();
	}

	@Override
	public byte getFactoryId() {
		return SerializableTagElement.NPC_FACTION_NEWS_EVENT;
	}

	@Override
	public void writeToTag(DataOutput dos) throws IOException {
		dos.writeByte(getType().ordinal());
		serialize(dos, true);
	}

	public String getOwnName(FactionState state){
		Faction faction = state.getFactionManager().getFaction(factionId);
		if(faction != null) return faction.getName();
		else return Lng.str("Unknown");
	}

	public abstract FactionNews.FactionNewsEventType getType();

	public abstract String getMessage(FactionState state);
}