package videogoose.betterfactions.diplomacy;

import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.network.SerialializationInterface;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Something one faction asks another to agree to, and the answer coming back.
 *
 * <p>Both directions ride this one object: a proposal, its withdrawal, and the accept or decline
 * are four values of {@link Action} rather than two wire formats and two staging queues. The pair
 * they are keyed on is {@link #getOfferCode(int, int)}, which is <em>directed</em> - A offering B a
 * pact and B offering A one are two separate proposals - so a response carries the original
 * proposal's {@code from}/{@code to} and not the responder's own.</p>
 *
 * <h2>Identity</h2>
 * {@link #initiator} arrives from the client and is therefore a claim, not a fact. On the server it
 * is overwritten with the name the network layer's {@link #sender} resolves to before anything
 * reads it; see {@code FactionManager.updateFromNetworkObject}. The engine's own proposals (an
 * auto-declared war, an NPC reaction) never cross the wire and set {@link #serverAuthored} instead,
 * which no deserialize can produce.
 */
public class DiplomacyProposal extends FactionRelation implements SerialializationInterface {

    private static final byte VERSION = 6;
    /** Server-enforced; also what keeps the wire count in one byte. */
    public static final int MAX_TERMS = 8;
    private static final Action[] ACTIONS = Action.values();
    /**
     * The {@link TreatyType} being proposed, or null for a plain relation change (white peace).
     */
    public String typeId;
    public Action action = Action.PROPOSE;

    public long expiresAt;
    /**
     * Stamped by the network layer on deserialize. -1 for anything not read off the wire.
     */
    public int sender = -1;
    /**
     * True only for proposals the engine built for itself. Never set by a deserialize.
     */
    public boolean serverAuthored;
    /**
     * Server only, never saved or sent: a war that is itself the answer to a call to arms (or set by an
     * admin), so it does not call the other side's pact partners in turn. Without it every pact on
     * both sides would chain into one galaxy-wide war.
     */
    public transient boolean noCallToArms;
    /**
     * Only meaningful on a {@link Action#QUERY_RESULT}: whether they would say yes. When they would
     * not, {@link #getMessage()} carries the reasons the acceptance rule gave.
     */
    public boolean accepted;
    /**
     * What the proposer wants the agreement called, or empty to let the engine name it. It rides the
     * offer rather than being set afterwards so that the other side reads the name before agreeing to
     * it - which is also what makes renaming a renegotiation rather than a unilateral edit.
     */
    public String name = "";
    /**
     * What changes hands at signing, on top of {@link #typeId}. Applied all together or not at all;
     * see {@link SettlementTerm}. A response carries none: accepting means accepting the stored offer's.
     */
    public final List<SettlementTerm> terms = new ArrayList<>();
    /**
     * Non-zero only on a call to arms: the faction {@link #a}, attacked, asks its pact partner
     * {@link #b} to declare war on this one. {@link #expiresAt} is then the answer deadline, not a
     * treaty term. Only the server makes these; it zeroes the field on anything a client sends.
     */
    public int against;
    private String message;
    private String initiator;

    /**
     * Directed pair key: unlike {@link FactionRelation#getCode(int, int)}, the order matters, because
	 * who asked is part of what an offer is.
     */
    public static long getOfferCode(int a, int b) {
        return (long) Math.abs(a) * Integer.MAX_VALUE + Math.abs(b);
    }

    /**
     * The treaty a legacy three-value relation stands for. Null for NEUTRAL: peace is the absence of
     * an agreement, not one of its own.
     */
    public static String treatyIdOf(byte relation) {
        if (relation == RType.ENEMY.code) {
            return TreatyTypeRegistry.WAR;
        }
        if (relation == RType.FRIEND.code) {
            return TreatyTypeRegistry.ALLIANCE;
        }
        return null;
    }

    /**
     * The answer to an existing proposal, addressed to the same pair it was.
     */
    public static DiplomacyProposal respondTo(DiplomacyProposal proposal, String initiator, boolean accept) {
        DiplomacyProposal r = new DiplomacyProposal();
        r.a = proposal.a;
        r.b = proposal.b;
        r.rel = proposal.rel;
        r.typeId = proposal.typeId;
        r.expiresAt = proposal.expiresAt;
        r.message = "";
        r.initiator = initiator;
        r.action = accept ? Action.ACCEPT : Action.DECLINE;
        return r;
    }

    /**
     * An answer that says "not that, but this". Addressed to the pair the original was, so it
     * settles that proposal; the terms it carries are the ones being asked for instead.
     *
     * @param typeId    treaty being countered with, or null for plain peace
     * @param expiresAt term being asked for, or 0 for one with no term
     */
    public static DiplomacyProposal counterTo(DiplomacyProposal proposal, String initiator,
                                              String typeId, long expiresAt, String message, String name) {
        return counterTo(proposal, initiator, typeId, expiresAt, message, name, List.of());
    }

    public static DiplomacyProposal counterTo(DiplomacyProposal proposal, String initiator, String typeId,
                                              long expiresAt, String message, String name,
                                              List<SettlementTerm> terms) {
        DiplomacyProposal r = new DiplomacyProposal();
        r.terms.addAll(terms);
        r.a = proposal.a;
        r.b = proposal.b;
        r.typeId = typeId;
        TreatyType type = typeId != null ? TreatyTypeRegistry.get(typeId) : null;
        r.rel = type != null && type.getImplies() != null ? type.getImplies().code : RType.NEUTRAL.code;
        r.expiresAt = expiresAt;
        r.message = message;
        r.initiator = initiator;
        r.action = Action.COUNTER;
        r.name = name == null ? "" : name;
        return r;
    }

    /**
     * Asks whether a treaty would be accepted, without offering it.
     */
    public static DiplomacyProposal query(String initiator, int from, int to, String typeId) {
        return query(initiator, from, to, typeId, List.of());
    }

    public static DiplomacyProposal query(String initiator, int from, int to, String typeId,
                                          List<SettlementTerm> terms) {
        DiplomacyProposal q = new DiplomacyProposal();
        q.terms.addAll(terms);
        q.a = from;
        q.b = to;
        q.typeId = typeId;
        TreatyType type = typeId != null ? TreatyTypeRegistry.get(typeId) : null;
        q.rel = type != null && type.getImplies() != null ? type.getImplies().code : RType.NEUTRAL.code;
        q.message = "";
        q.initiator = initiator;
        q.action = Action.QUERY;
        return q;
    }

    /**
     * The answer to a query, addressed to the same pair so the asking client can tell which of its
     * questions this replies to.
     *
     * @param reasons why not, when they would refuse; ignored when they would not
     */
    public static DiplomacyProposal queryResult(DiplomacyProposal q, boolean accepted, String reasons) {
        DiplomacyProposal r = new DiplomacyProposal();
        r.a = q.a;
        r.b = q.b;
        r.rel = q.rel;
        r.typeId = q.typeId;
        //echoed so the asking dialog can tell an answer about these terms from one about older ones
        r.terms.addAll(q.terms);
        r.message = accepted ? "" : nullToEmpty(reasons);
        r.initiator = q.initiator;
        r.action = Action.QUERY_RESULT;
        r.accepted = accepted;
        r.serverAuthored = true;
        return r;
    }

	private static String nullToEmpty(Object s) {
		return s == null ? "" : s.toString();
	}

	private static String emptyToNull(String s) {
		return s == null || s.isEmpty() ? null : s;
    }

    /**
     * The counter, turned around into the fresh proposal it also is.
     */
    public DiplomacyProposal asCounterProposal() {
        DiplomacyProposal p = new DiplomacyProposal();
        //the pair flips: whoever countered is now the one asking
        p.a = b;
		p.b = a;
		p.rel = rel;
		p.typeId = typeId;
		p.expiresAt = expiresAt;
		p.message = message;
		p.name = name;
		p.terms.addAll(terms);
		p.initiator = initiator;
		p.action = Action.PROPOSE;
		p.serverAuthored = serverAuthored;
        return p;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getMessage() {
        return message;
    }

    public boolean isResponse() {
        return action == Action.ACCEPT || action == Action.DECLINE || action == Action.COUNTER;
    }

    /**
     * A question or its answer: neither stages anything, so neither goes through the offer queues.
     */
    public boolean isQuery() {
        return action == Action.QUERY || action == Action.QUERY_RESULT;
    }

    /** The term to write onto a treaty signed from this proposal. */
	public long treatyExpiry() {
		return expiresAt > 0 ? expiresAt : FactionTreaty.NO_EXPIRY;
    }

    /**
     * Legacy form: the treaty id follows from the relation being offered.
     */
    public void set(String initiator, int a, int b, byte relation, String message, boolean revoke) {
        set(initiator, a, b, treatyIdOf(relation), relation, message, revoke ? Action.REVOKE : Action.PROPOSE);
    }

    public void set(String initiator, int a, int b, String typeId, byte relation, String message, Action action) {
        set(initiator, a, b, typeId, relation, message, action, 0L);
    }

    public void set(String initiator, int a, int b, String typeId, byte relation, String message,
	                Action action, long expiresAt) {
        this.expiresAt = expiresAt;
        this.a = a;
        this.b = b;
        rel = relation;
        this.typeId = typeId;
		this.message = message;
		this.initiator = initiator;
		this.action = action;
	}

	@Override
	public String toString() {
		return "Proposal[" + action + " " + a + "->" + b + ", " + (typeId != null ? typeId : getRelation().name()) + (terms.isEmpty() ? "" : ", " + terms) + "]";
	}

	@Override
	public void fromTagStructure(Tag tag) {
        Tag[] subs = (Tag[]) tag.getValue();

        a = (Integer) subs[0].getValue();
        b = (Integer) subs[1].getValue();
        rel = (Byte) subs[2].getValue();
        message = (String) subs[3].getValue();
        initiator = (String) subs[4].getValue();
        //written before proposals carried a treaty type; such a file only ever held PROPOSE rows
        if (subs.length > 6 && subs[5].getType() != Tag.Type.FINISH) {
            typeId = emptyToNull((String) subs[5].getValue());
            action = DiplomacyTags.read(Action.class, subs[6]);
		} else {
			typeId = treatyIdOf(rel);
			action = Action.PROPOSE;
		}
		expiresAt = subs.length > 7 && subs[7].getType() != Tag.Type.FINISH ? (Long) subs[7].getValue() : 0L;
        name = subs.length > 8 && subs[8].getType() != Tag.Type.FINISH ? (String) subs[8].getValue() : "";
        terms.clear();
        if (subs.length > 9 && subs[9].getType() != Tag.Type.FINISH) {
            SettlementTerm.listFromTag(subs[9], terms);
        }
        against = subs.length > 10 && subs[10].getType() != Tag.Type.FINISH ? (Integer) subs[10].getValue() : 0;
		if (action == null) {
            action = Action.PROPOSE;
        }
    }

    @Override
    public Tag toTagStructure() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.INT, null, a),
                new Tag(Tag.Type.INT, null, b),
                new Tag(Tag.Type.BYTE, null, rel),
				new Tag(Tag.Type.STRING, null, nullToEmpty(message)),
                new Tag(Tag.Type.STRING, null, nullToEmpty(initiator)),
                new Tag(Tag.Type.STRING, null, nullToEmpty(typeId)),
                DiplomacyTags.write(action),
                new Tag(Tag.Type.LONG, null, expiresAt),
                new Tag(Tag.Type.STRING, null, nullToEmpty(name)),
                SettlementTerm.listToTag(terms),
                new Tag(Tag.Type.INT, null, against),
                FinishTag.INST});
    }

    @Override
    public void deserialize(DataInput b, int updateSenderStateId, boolean onServer) throws IOException {
        //the authoritative identity of the requester; everything else here is a claim
        sender = updateSenderStateId;
        serverAuthored = false;
        byte version = b.readByte();
        assert (version == VERSION) : version;
        a = b.readInt();
        this.b = b.readInt();
        rel = b.readByte();
        typeId = emptyToNull(b.readUTF());
        int act = b.readByte();
        if (act < 0 || act >= ACTIONS.length) {
            throw new IOException("diplomacy proposal with no such action: " + act);
        }
        action = ACTIONS[act];
        expiresAt = b.readLong();
        message = b.readUTF();
        initiator = b.readUTF();
        accepted = b.readBoolean();
        name = nullToEmpty(TreatyNames.sanitize(b.readUTF()));
        terms.clear();
        int n = b.readUnsignedByte();
        for (int i = 0; i < n; i++) {
            terms.add(SettlementTerm.deserialize(b));
        }
        against = b.readInt();
    }

    @Override
    public void serialize(DataOutput b, boolean isOnServer) throws IOException {
        b.writeByte(VERSION);
        b.writeInt(a);
        b.writeInt(this.b);
        b.writeByte(rel);
        b.writeUTF(nullToEmpty(typeId));
        b.writeByte(action.ordinal());
        b.writeLong(expiresAt);
        b.writeUTF(nullToEmpty(message));
        b.writeUTF(nullToEmpty(initiator));
		b.writeBoolean(accepted);
        b.writeUTF(nullToEmpty(name));
        b.writeByte(terms.size());
        for (SettlementTerm t : terms) {
            t.serialize(b);
        }
        b.writeInt(against);
	}

    @Override
    public long getCode() {
        return getOfferCode(a, b);
    }

    @Override
	public void set(int a, int b) {
		this.a = a;
		this.b = b;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((initiator == null) ? 0 : initiator.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
        result = prime * result + action.ordinal();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof DiplomacyProposal other)) {
			return false;
		}
        if (initiator == null) {
			if (other.initiator != null) {
                return false;
            }
        } else if (!initiator.equals(other.initiator)) {
            return false;
        }
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
			return false;
		}
		if (typeId == null) {
			if (other.typeId != null) {
				return false;
			}
		} else if (!typeId.equals(other.typeId)) {
			return false;
		}
		return action == other.action;
	}

    /**
     * What the sender wants done. Kept as one enum rather than the old {@code revoke} boolean plus a
     * separate accept/decline message, so a single permission gate covers every way a proposal moves.
     */
    public enum Action {
        PROPOSE,
        /**
         * Withdrawn by the proposer, or - for a treaty already in force - broken.
         */
        REVOKE,
        ACCEPT,
        DECLINE,
        /**
         * Turned down with different terms attached. A counter is both halves at once: it settles
         * the proposal it answers and becomes a proposal of its own, back the other way, which the
         * original proposer can then accept, refuse or counter again.
         */
        COUNTER,
        /**
         * "Would you accept this?" - asked before anything is put on the table, so the GUI can say
         * whether an offer is worth making. Nothing is staged, no opinion moves, and the answer comes
         * back as a {@link #QUERY_RESULT}.
         *
         * <p>It exists because the acceptance rules cannot be evaluated where the question is asked:
         * an NPC faction's preset never leaves the server, so a client computing {@code wouldAccept}
         * itself would silently answer from the default settings and be wrong.</p>
         */
        QUERY,
        /**
         * The answer to a {@link #QUERY}; see {@link #accepted} and {@link #getMessage()}.
         */
        QUERY_RESULT,
    }
}
