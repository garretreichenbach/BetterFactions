package videogoose.betterfactions.diplomacy;

import videogoose.betterfactions.diplomacy.DiplomacyEntity.DiplStatusType;
import org.schema.schine.resource.tag.Tag;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag.Type;

public class OpinionStaticModifier extends OpinionModifier {

    private static final byte VERSION = 2;
    public int value;
    public long elapsedTimeInactive;
    public DiplStatusType type;
    public long totalTimeApplied;

    public OpinionStaticModifier() {
        super();
        // TODO Auto-generated constructor stub
    }

    public Tag toTag() {
        return new Tag(Type.STRUCT, null, new Tag[]{
                new Tag(Type.BYTE, null, VERSION),
                new Tag(Type.INT, null, value),
                new Tag(Type.LONG, null, elapsedTimeInactive),
                DiplomacyTags.write(type),
                new Tag(Type.LONG, null, totalTimeApplied),
                FinishTag.INST,
        });
    }

    /**
     * @return false if the status type is not one this build knows, in which case skip the entry.
     */
    public boolean fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        byte version = t[0].getByte();

        value = t[1].getInt();
        elapsedTimeInactive = t[2].getLong();
        type = DiplomacyTags.read(DiplStatusType.class, t[3]);
        //v0 did not store it, so a pre-existing save restarts this modifier's clock at 0
        totalTimeApplied = version >= 1 ? t[4].getLong() : 0;
        return type != null;
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
