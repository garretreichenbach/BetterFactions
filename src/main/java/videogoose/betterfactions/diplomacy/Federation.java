package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.function.IntToDoubleFunction;

/**
 * A bloc of factions rather than an agreement between two.
 *
 * <p>Everything else in this package is a pact between a pair, keyed on that pair. A federation is
 * the other shape: several factions bound at once, with a name, a leader and a charter the members
 * signed up to. It is worth being its own object rather than a treaty with more than two signatories
 * because the interesting parts - who leads, who is admitted, who is expelled - are questions about
 * the bloc, and a pairwise record has nowhere to put them.</p>
 *
 * <h2>Membership is treaties</h2>
 * Co-membership is <b>not</b> a special case in {@code getRelation}. Joining writes a
 * {@link TreatyTypeRegistry#FEDERATION} treaty between the joiner and every sitting member, and
 * leaving tears them up. That is why the type is registered as unbreakable and unilateral: you
 * cannot walk away from one agreement while staying in the bloc, and leaving the bloc is the only
 * thing that ends them. In exchange, federated factions are allied through exactly the same code
 * path as any other alliance - sync, persistence, the treaty list and every combat consumer of the
 * relation needed no change at all.
 *
 * <h2>The chair runs it</h2>
 * Admission, expulsion, war, the charter and the bloc's name are the chair's ({@link #leader}) to
 * decide, unless the charter says a decision goes to the members. The charter also says how a vote
 * is carried, how ballots weigh, whether a carried war binds those who voted against it, and whether
 * the chair rotates - see the {@code CHARTER_*} flags. That is
 * deliberate rather than an oversight: the deal you accept when you join a bloc is its leader, and
 * <b>leaving is always free and immediate</b>, which is the check on the leader rather than a quorum
 * nobody asked for. A faction is in at most one federation at a time.
 *
 * <p>A federation with no members does not exist - that is how dissolution travels over the wire and
 * how it is recognised on load, with no removed flag on the record.</p>
 *
 * <h2>Who sees what</h2>
 * A bloc's existence, name, leader and roster are public - they decide who shoots at whom, and the
 * membership treaties already say so to everybody. What it is <i>arguing about</i> is not: the open
 * votes and every ballot on them go to members only, which is why a federation is pushed per recipient
 * rather than broadcast. See {@link #copyFor}.
 */
public class Federation {

    private static final byte VERSION = 1;

    /*
     * The charter's rules, packed into one long so that a rule change is one number on the wire, in
     * the save and in a charter vote's payload. The low byte holds yes/no rules, bits 8-9 the
     * threshold and bits 16-23 the chair's term in days.
     */

    /**
     * Admitting a faction goes to the members instead of being the chair's call.
     */
    public static final long CHARTER_VOTE_ADMISSION = 1L;
    /**
     * Expelling a member goes to the members instead of being the chair's call.
     */
    public static final long CHARTER_VOTE_EXPULSION = 2L;
    /**
     * Declaring war in the bloc's name goes to the members instead of being the chair's call.
     */
    public static final long CHARTER_VOTE_WAR = 4L;
    /**
     * Changing these rules goes to the members. Without it a chair could switch off every other
     * vote on their own, which would make the rest of the charter advisory.
     */
    public static final long CHARTER_VOTE_CHARTER = 8L;
    /**
     * Ballots weigh by faction power rather than one per faction.
     */
    public static final long CHARTER_WEIGHT_BY_POWER = 16L;
    /**
     * A carried war binds only the members that voted for it. Off - the default - binds everyone,
     * which is what makes the bloc a polity rather than a mailing list with a flag; a member that will
     * not be bound can always leave.
     */
    public static final long CHARTER_BIND_SUPPORTERS = 32L;
    private static final int THRESHOLD_SHIFT = 8;
    private static final int ROTATION_SHIFT = 16;
    public static final int MAX_ROTATION_DAYS = 90;
    private static final long KNOWN_FLAGS = CHARTER_VOTE_ADMISSION | CHARTER_VOTE_EXPULSION | CHARTER_VOTE_WAR
            | CHARTER_VOTE_CHARTER | CHARTER_WEIGHT_BY_POWER | CHARTER_BIND_SUPPORTERS;
    public static final long DAY_MS = 24L * 60L * 60L * 1000L;

    /**
     * How large a share of the weight a vote needs to carry.
     */
    public enum Threshold {
        SIMPLE_MAJORITY,
        TWO_THIRDS,
        UNANIMOUS;

        public boolean carries(double yes, double total) {
            return switch (this) {
                case SIMPLE_MAJORITY -> yes * 2 > total;
                case TWO_THIRDS -> yes * 3 >= total * 2;
                case UNANIMOUS -> yes >= total;
            };
        }
    }

    /**
     * How long a vote stays open before it lapses. Long because factions are not online together;
     * a vote that is already decided resolves the moment the deciding ballot lands, so this only
     * bounds the case where somebody never turns up. An unresolved vote fails - a bloc that could
     * not be bothered to answer has not agreed to anything.
     */
    public static final long VOTE_WINDOW_MS = DAY_MS;

    public int id;
    public String name = "";
    /**
     * Free text the leader writes: what the bloc is for. Flavour, and the thing an RP server
     * actually wanted.
     */
    public String charter = "";
    /**
     * The member faction that speaks for the bloc. Always a member.
     */
    public int leader;
    public final IntArrayList members = new IntArrayList();
    /**
     * Factions asked to join and not yet answered. An invite rather than a conscription: the bloc
     * names you, and joining still takes an act by somebody in your faction.
     */
    public final IntArrayList invites = new IntArrayList();
    public long charterFlags;
    /**
     * Decisions currently before the members. At most one per kind and subject - the bloc is not
     * asked twice at once whether to admit the same faction.
     */
    public final ObjectArrayList<FederationVote> votes = new ObjectArrayList<FederationVote>();
    /**
     * Ids handed to votes, so a ballot can name which vote it answers and a stale ballot for a
     * resolved vote is recognised rather than applied to its successor.
     */
    public int nextVoteId = 1;
    public long founded;
    /**
     * When the current chair took it, which is what a rotating chair's term is counted from.
     */
    public long chairSince;

    public Federation() {
    }

    public Federation(int id, String name, int founder) {
        this(id, name, founder, 0L);
    }

    public Federation(int id, String name, int founder, long now) {
        this.founded = now;
        this.chairSince = now;
        this.id = id;
        this.name = name;
        this.leader = founder;
        this.members.add(founder);
    }

    public boolean isMember(int factionId) {
        return members.contains(factionId);
    }

    public boolean isInvited(int factionId) {
        return invites.contains(factionId);
    }

    public boolean hasCharterFlag(long flag) {
        return (charterFlags & flag) != 0;
    }

    /**
     * A bloc with nobody in it is gone. Used on load and on the wire in place of a removed flag.
     */
    public boolean isDissolved() {
        return members.isEmpty();
    }

    public Threshold threshold() {
        int t = (int) ((charterFlags >>> THRESHOLD_SHIFT) & 3);
        return Threshold.values()[Math.min(t, Threshold.values().length - 1)];
    }

    /**
     * Days a chair sits before it passes to the next member, or 0 for a chair that holds until it is
     * handed over.
     */
    public int rotationDays() {
        return (int) ((charterFlags >>> ROTATION_SHIFT) & 0xFF);
    }

    public static long withThreshold(long flags, Threshold t) {
        return (flags & ~(3L << THRESHOLD_SHIFT)) | ((long) t.ordinal() << THRESHOLD_SHIFT);
    }

    public static long withRotationDays(long flags, int days) {
        days = Math.max(0, Math.min(MAX_ROTATION_DAYS, days));
        return (flags & ~(0xFFL << ROTATION_SHIFT)) | ((long) days << ROTATION_SHIFT);
    }

    /**
     * Only rules this build knows, in range. A client names the flags, so this is what stands between
     * it and a charter nobody can read.
     */
    public static long sanitize(long flags) {
        Federation probe = new Federation();
        probe.charterFlags = flags;
        return withRotationDays(withThreshold(flags & KNOWN_FLAGS, probe.threshold()), probe.rotationDays());
    }

    public FederationVote vote(int voteId) {
        for (FederationVote v : votes) {
            if (v.id == voteId) {
                return v;
            }
        }
        return null;
    }

    /**
     * The open vote on this question, if there is one. A charter has no subject, so there is only
     * ever one charter vote.
     */
    public FederationVote voteOn(FederationVote.Kind kind, int subject) {
        for (FederationVote v : votes) {
            if (v.kind == kind && (kind == FederationVote.Kind.CHARTER || v.subject == subject)) {
                return v;
            }
        }
        return null;
    }

    /**
     * The subject of an expulsion does not vote on it, or a bloc of two could never expel anybody.
     */
    public boolean mayVote(FederationVote v, int factionId) {
        return isMember(factionId) && !(v.kind == FederationVote.Kind.EXPEL && v.subject == factionId);
    }

    public enum Outcome {
        OPEN,
        CARRIED,
        LOST,
    }

    /**
     * Counts a vote under this charter's threshold.
     *
     * <p>Settled early: the moment enough yes weight exists to carry it, or so much no weight that
     * every outstanding ballot together could not, waiting for the rest is only waiting. Past its
     * window an undecided vote fails - a bloc that could not be bothered to answer has not agreed to
     * anything.</p>
     *
     * @param weight what one member's ballot counts for - 1 each, or their power
     */
    public Outcome tally(FederationVote v, IntToDoubleFunction weight, long now) {
        double total = 0;
        double yes = 0;
        double no = 0;
        for (int i = 0; i < members.size(); i++) {
            int m = members.getInt(i);
            if (!mayVote(v, m)) {
                continue;
            }
            double w = weight.applyAsDouble(m);
            total += w;
            if (v.hasVoted(m)) {
                if (v.ballots.get(m) != 0) {
                    yes += w;
                } else {
                    no += w;
                }
            }
        }
        Threshold t = threshold();
        if (total > 0 && t.carries(yes, total)) {
            return Outcome.CARRIED;
        }
        if (total <= 0 || !t.carries(total - no, total) || now >= v.closes) {
            return Outcome.LOST;
        }
        return Outcome.OPEN;
    }

    /**
     * Whether a carried vote holds this member to it.
     */
    public boolean binds(FederationVote v, int member) {
        return isMember(member) && (!hasCharterFlag(CHARTER_BIND_SUPPORTERS)
                || (v.hasVoted(member) && v.ballots.get(member) != 0));
    }

    /**
     * One decision put to the members.
     *
     * <p>Lives inside the federation rather than in a store of its own, which is what keeps it free:
     * it is persisted, synced and dropped along with the bloc it belongs to, with no wire type, no
     * save slot and no lifetime of its own to get wrong.</p>
     */
    public static class FederationVote {

        public enum Kind {
            /**
             * Admit {@link #subject} - really "send them an invite", since joining still takes their
             * own act.
             */
            ADMIT,
            EXPEL,
            /**
             * Declare war on {@link #subject} in the bloc's name.
             */
            WAR,
            /**
             * Replace the charter with {@link #text} and {@link #value}. Has no subject.
             */
            CHARTER,
        }

        public int id;
        public Kind kind = Kind.ADMIT;
        /**
         * The faction being voted about.
         */
        public int subject;
        public long opened;
        public long closes;
        /**
         * The proposed rules, for a charter vote.
         */
        public long value;
        /**
         * The proposed charter text, for a charter vote.
         */
        public String text = "";
        /**
         * Faction id to ballot: 1 for yes, 0 for no. A faction that has not voted is absent.
         */
        public final Int2ByteMap ballots = new Int2ByteOpenHashMap();

        public FederationVote() {
        }

        public FederationVote(int id, Kind kind, int subject, long now) {
            this.id = id;
            this.kind = kind;
            this.subject = subject;
            this.opened = now;
            this.closes = now + VOTE_WINDOW_MS;
        }

        public int count(boolean yes) {
            int n = 0;
            for (Int2ByteMap.Entry e : ballots.int2ByteEntrySet()) {
                if ((e.getByteValue() != 0) == yes) {
                    n++;
                }
            }
            return n;
        }

        public boolean hasVoted(int factionId) {
            return ballots.containsKey(factionId);
        }

        public Tag toTag() {
            Tag[] b = new Tag[ballots.size() + 1];
            int i = 0;
            for (Int2ByteMap.Entry e : ballots.int2ByteEntrySet()) {
                b[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                        new Tag(Tag.Type.INT, null, e.getIntKey()),
                        new Tag(Tag.Type.BYTE, null, e.getByteValue()),
                        FinishTag.INST,
                });
            }
            b[b.length - 1] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.BYTE, null, VERSION),
                    new Tag(Tag.Type.INT, null, id),
                    DiplomacyTags.write(kind),
                    new Tag(Tag.Type.INT, null, subject),
                    new Tag(Tag.Type.LONG, null, opened),
                    new Tag(Tag.Type.LONG, null, closes),
                    new Tag(Tag.Type.STRUCT, null, b),
                    new Tag(Tag.Type.LONG, null, value),
                    new Tag(Tag.Type.STRING, null, text == null ? "" : text),
                    FinishTag.INST,
            });
        }

        /**
         * @return false for a vote this build cannot read - an unknown kind from a modded or newer
         * save - in which case it is dropped rather than loaded as something else.
         */
        public boolean fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            id = t[1].getInt();
            kind = DiplomacyTags.read(Kind.class, t[2]);
            subject = t[3].getInt();
            opened = t[4].getLong();
            closes = t[5].getLong();
            ballots.clear();
            Tag[] b = t[6].getStruct();
            for (int i = 0; i < b.length - 1; i++) {
                Tag[] e = b[i].getStruct();
                ballots.put(e[0].getInt(), e[1].getByte());
            }
            //a version 0 vote ended at its ballots
            if (t.length > 9) {
                value = t[7].getLong();
                text = t[8].getString();
            }
            return kind != null;
        }
    }

    public Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, VERSION),
                new Tag(Tag.Type.INT, null, id),
                new Tag(Tag.Type.STRING, null, name == null ? "" : name),
                new Tag(Tag.Type.STRING, null, charter == null ? "" : charter),
                new Tag(Tag.Type.INT, null, leader),
                intList(members),
                intList(invites),
                new Tag(Tag.Type.LONG, null, charterFlags),
                new Tag(Tag.Type.INT, null, nextVoteId),
                voteList(),
                new Tag(Tag.Type.LONG, null, founded),
                new Tag(Tag.Type.LONG, null, chairSince),
                FinishTag.INST,
        });
    }

    public void fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        id = t[1].getInt();
        name = t[2].getString();
        charter = t[3].getString();
        leader = t[4].getInt();
        readInts(t[5], members);
        readInts(t[6], invites);
        charterFlags = t[7].getLong();
        nextVoteId = t[8].getInt();
        votes.clear();
        if (t[0].getByte() == 0) {
            //version 0 held at most one vote, in place, or a finish tag for none
            if (t[9].getType() != Tag.Type.FINISH) {
                readVote(t[9]);
            }
            return;
        }
        Tag[] v = t[9].getStruct();
        for (int i = 0; i < v.length - 1; i++) {
            readVote(v[i]);
        }
        founded = t[10].getLong();
        chairSince = t[11].getLong();
    }

    private void readVote(Tag tag) {
        FederationVote v = new FederationVote();
        if (v.fromTag(tag)) {
            votes.add(v);
        }
    }

    private Tag voteList() {
        Tag[] t = new Tag[votes.size() + 1];
        for (int i = 0; i < votes.size(); i++) {
            t[i] = votes.get(i).toTag();
        }
        t[t.length - 1] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    /**
     * This bloc as one faction is allowed to see it: the whole thing for a member, and everything but
     * the open vote for anyone else.
     *
     * <p>A copy rather than a view, and taken through the tag serializer rather than field by field,
     * so the snapshot that goes on the wire cannot drift as the live bloc changes underneath it and
     * cannot disagree with the save format. It runs when somebody joins or votes, not on a tick.</p>
     *
     * <p>Redacting a copy is what lets the receiving side replace wholesale with no merge rule: every
     * recipient is sent a picture that is complete <i>for them</i>, so whichever arrives last is
     * right.</p>
     */
    public Federation copyFor(int viewerFactionId) {
        Federation c = new Federation();
        c.fromTag(toTag());
        if (!isMember(viewerFactionId)) {
            c.votes.clear();
        }
        return c;
    }

    public void serialize(java.io.DataOutput buffer, boolean onServer) throws java.io.IOException {
        toTag().serializeNT(buffer);
    }

    public void deserialize(java.io.DataInputStream stream, int updateSenderStateId, boolean onServer) throws java.io.IOException {
        fromTag(Tag.deserializeNT(stream));
    }

    private static Tag intList(IntArrayList list) {
        Tag[] t = new Tag[list.size() + 1];
        for (int i = 0; i < list.size(); i++) {
            t[i] = new Tag(Tag.Type.INT, null, list.getInt(i));
        }
        t[t.length - 1] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    private static void readInts(Tag tag, IntArrayList into) {
        into.clear();
        Tag[] t = tag.getStruct();
        for (int i = 0; i < t.length - 1; i++) {
            into.add(t[i].getInt());
        }
    }

    @Override
    public String toString() {
        return "Federation[" + id + " " + name + ", " + members.size() + " members, led by " + leader + "]";
    }
}
