package thederpgamer.betterfactions.data.diplomacy.modifier;

import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyAction.DiplActionType;
import org.schema.schine.resource.tag.Tag;import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag.Type;

public class FactionDiplomacyTurnMod extends FactionDiplomacyMod {
	public DiplActionType type;
	public int pointsPerTurn;
	public long elapsedTime;
	public long totalElapsedTime;

	public Tag toTag() {
		return new Tag(Type.STRUCT, null, new Tag[]{
				new Tag(Type.BYTE, null, (byte)0),
				new Tag(Type.INT, null, pointsPerTurn),
				new Tag(Type.LONG, null, totalElapsedTime),
				new Tag(Type.BYTE, null, (byte)type.ordinal()),
				FinishTag.INST,
		});
	}

	public void fromTag(Tag tag){
		Tag[] t = tag.getStruct();
		byte version = t[0].getByte();

		pointsPerTurn = t[1].getInt();
		totalElapsedTime = t[2].getLong();
		type = DiplActionType.values()[t[3].getByte()];
	}

	@Override
	public String getName() {
		return type.getDescription();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public int getValue() {
		return pointsPerTurn;
	}
}