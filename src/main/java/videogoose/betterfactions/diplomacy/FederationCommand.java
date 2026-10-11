package videogoose.betterfactions.diplomacy;

import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * One thing a client asks its server to do about a federation.
 *
 * <p>Every request on one object rather than a buffer each, because they are all the same
 * request: a member of some faction wants the bloc changed. The difference between them is a
 * {@link Kind} and at most two numbers.</p>
 *
 * <h2>Identity</h2>
 * There is no field naming who is asking, and that is the point. The command rides the sending
 * client's own channel, so the server reads the asker off the channel it arrived on - a client
 * cannot name somebody else, because there is nowhere to write a name. {@link #federationId} is
 * likewise only ever checked against the bloc the sender's faction is actually in, so pointing it at
 * another one buys nothing.
 */
public class FederationCommand {

    private static final byte VERSION = 0;

    public enum Kind {
        /** Found a bloc. {@link #text} names it; the founder's faction leads it. */
        CREATE,
        /** Leader: rename the bloc. */
        RENAME,
        /** Chair, or a vote: rewrite the charter text ({@link #text}) and rules ({@link #value}). */
        SET_CHARTER,
        /** Leader, or a passed vote: ask {@link #target} to join. */
        INVITE,
        /** Leader: withdraw an outstanding invite to {@link #target}. */
        CANCEL_INVITE,
        /** Take up an invite addressed to the sender's own faction. */
        ACCEPT_INVITE,
        /** Refuse one. */
        DECLINE_INVITE,
        /** Walk out. Always allowed, always immediate. */
        LEAVE,
        /** Leader, or a passed vote: throw {@link #target} out. */
        EXPEL,
        /** Leader: hand the bloc to {@link #target}, who must already be a member. */
        TRANSFER_LEAD,
        /** Answer the open vote. {@link #target} is its id, {@link #value} is 1 for yes. */
        CAST,
        /** Leader: wind the bloc up. */
        DISSOLVE,
        /** Chair, or a vote: take the bloc to war with {@link #target}. */
        DECLARE_WAR,
    }

    public Kind kind = Kind.CREATE;
    public int federationId;
    /**
     * A faction id for most kinds; the vote id for {@link Kind#CAST}.
     */
    public int target;
    /**
     * Charter flags for {@link Kind#SET_CHARTER}; the ballot for {@link Kind#CAST}.
     */
    public long value;
    public String text = "";

    public FederationCommand() {
    }

    public FederationCommand(Kind kind, int federationId) {
        this.kind = kind;
        this.federationId = federationId;
    }

    public FederationCommand target(int target) {
        this.target = target;
        return this;
    }

    public FederationCommand value(long value) {
        this.value = value;
        return this;
    }

    public FederationCommand text(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    public Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, VERSION),
                DiplomacyTags.write(kind),
                new Tag(Tag.Type.INT, null, federationId),
                new Tag(Tag.Type.INT, null, target),
                new Tag(Tag.Type.LONG, null, value),
                new Tag(Tag.Type.STRING, null, text == null ? "" : text),
                FinishTag.INST,
        });
    }

    /**
     * @return false for a kind this build does not know, in which case the command is ignored rather
     * than run as whichever one happens to sit at that index.
     */
    public boolean fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        kind = DiplomacyTags.read(Kind.class, t[1]);
        federationId = t[2].getInt();
        target = t[3].getInt();
        value = t[4].getLong();
        text = t[5].getString();
        return kind != null;
    }

    public void serialize(DataOutput buffer, boolean onServer) throws IOException {
        toTag().serializeNT(buffer);
    }

    public void deserialize(DataInputStream stream, int updateSenderStateId, boolean onServer) throws IOException {
        fromTag(Tag.deserializeNT(stream));
    }

    @Override
    public String toString() {
        return "FederationCommand[" + kind + " fed=" + federationId + " target=" + target + "]";
    }
}
