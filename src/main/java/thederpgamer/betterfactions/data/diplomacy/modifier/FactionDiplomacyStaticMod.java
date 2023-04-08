package thederpgamer.betterfactions.data.diplomacy.modifier;

import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import org.schema.schine.resource.tag.Tag.Type;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacyEntity;

public class FactionDiplomacyStaticMod extends FactionDiplomacyMod {

	public int value;
	public long elapsedTimeInactive;
	public FactionDiplomacyEntity.DiploStatusType type;
	public long totalTimeApplied;

	public Tag toTag() {
		return new Tag(Type.STRUCT, null, new Tag[] {
				new Tag(Type.BYTE, null, (byte)0),
				new Tag(Type.INT, null, value),
				new Tag(Type.LONG, null, elapsedTimeInactive),
				new Tag(Type.BYTE, null, (byte)type.ordinal()),
				FinishTag.INST,
		});
	}

	public void fromTag(Tag tag) {
		Tag[] t = tag.getStruct();
		byte version = t[0].getByte();
		value = t[1].getInt();
		elapsedTimeInactive = t[2].getLong();
		type = FactionDiplomacyEntity.DiploStatusType.values()[t[3].getByte()];
	}

	@Override
	public String getName() {
		return type.getDescription();
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public int getValue() {
		return value;
	}
}