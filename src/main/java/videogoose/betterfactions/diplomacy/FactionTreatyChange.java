package videogoose.betterfactions.diplomacy;

import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * One treaty coming into or going out of force, as sent to clients.
 *
 * <p>Separate from {@link FactionTreaty} because "this treaty just ended" is a wire concept, not a
 * property of the agreement - a persisted treaty is never in a removed state, it is simply gone. The
 * relation offers this sits beside put their equivalent flag on the record itself and pay for it by
 * writing a meaningless {@code revoke} byte into every save.</p>
 */
public class FactionTreatyChange {

    private static final byte VERSION = 0;

    public final FactionTreaty treaty = new FactionTreaty();
    public boolean removed;

    public FactionTreatyChange() {
    }

    public static FactionTreatyChange of(FactionTreaty treaty, boolean removed) {
        FactionTreatyChange c = new FactionTreatyChange();
        c.treaty.from = treaty.from;
        c.treaty.to = treaty.to;
        c.treaty.typeId = treaty.typeId;
        c.treaty.signedAt = treaty.signedAt;
        c.treaty.expiresAt = treaty.expiresAt;
        c.treaty.clause = treaty.clause;
        c.treaty.name = treaty.name;
        c.treaty.goals.addAll(treaty.goals);
        c.treaty.progress = treaty.progress;
        c.removed = removed;
        return c;
    }

    public Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, VERSION),
                new Tag(Tag.Type.BYTE, null, removed ? (byte) 1 : (byte) 0),
                treaty.toTag(),
                FinishTag.INST,
        });
    }

    /**
     * @return false if the treaty type is not registered here, in which case ignore the change.
     */
    public boolean fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        removed = t[1].getByte() != 0;
        return treaty.fromTag(t[2]);
    }

    public void serialize(DataOutput buffer, boolean onServer) throws IOException {
        toTag().serializeNT(buffer);
    }

    public void deserialize(DataInputStream stream, int updateSenderStateId, boolean onServer) throws IOException {
        fromTag(Tag.deserializeNT(stream));
    }

    @Override
    public String toString() {
        return (removed ? "-" : "+") + treaty;
    }
}
