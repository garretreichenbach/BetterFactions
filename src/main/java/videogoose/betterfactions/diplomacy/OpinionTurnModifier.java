package videogoose.betterfactions.diplomacy;

import videogoose.betterfactions.diplomacy.DiplomacyAction.DiplActionType;
import org.schema.schine.resource.tag.Tag;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag.Type;

public class OpinionTurnModifier extends OpinionModifier {
    private static final byte VERSION = 2;
    public DiplActionType type;
    public int pointsPerTurn;
    public long elapsedTime;
    public long totalElapsedTime;

    public OpinionTurnModifier() {
        super();
        // TODO Auto-generated constructor stub
    }

    public Tag toTag() {
        return new Tag(Type.STRUCT, null, new Tag[]{
                new Tag(Type.BYTE, null, VERSION),
                new Tag(Type.INT, null, pointsPerTurn),
                new Tag(Type.LONG, null, totalElapsedTime),
                DiplomacyTags.write(type),
                new Tag(Type.LONG, null, elapsedTime),
                FinishTag.INST,
        });
    }

    /**
     * @return false if the action type is not one this build knows, in which case skip the entry.
     */
    public boolean fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        byte version = t[0].getByte();

        pointsPerTurn = t[1].getInt();
        totalElapsedTime = t[2].getLong();
        type = DiplomacyTags.read(DiplActionType.class, t[3]);
        //v0 did not store it, so a pre-existing save restarts this modifier's ramp at 0
        elapsedTime = version >= 1 ? t[4].getLong() : 0;
        return type != null;
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
