package videogoose.betterfactions.diplomacy;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.function.IntFunction;

/**
 * One thing that changes hands when a settlement is signed. A proposal carries a list of them, and the
 * whole list applies at signing or none of it does; white peace is the empty list.
 *
 * <p>{@link #giver} is a faction id, not a side, so "you pay me" and "I pay you to go away" are the same
 * structure, and a counter-offer that flips the pair does not flip who pays.</p>
 *
 * <p>Typed fields rather than the plan's {@code Tag payload}: every kind built so far needs at most a
 * treaty type, a third faction and an amount. A kind that needs more can add a field.</p>
 */
public final class SettlementTerm {

    public enum Kind {
        /** The two signatories also sign {@link #typeId}; {@link #giver} is the side it binds, for asymmetric types. */
        SIGN_TREATY,
        /** The giver tears up its {@link #typeId} with {@link #with} - the other signatory, or a third faction. */
        BREAK_TREATY,
        /** The giver hands {@link #amount} faction points to the other signatory. */
        TRANSFER_FACTION_POINTS,
        /** The giver hands its claim on star system {@link #system} to the other signatory. */
        CEDE_SYSTEM,
    }

    public Kind kind;
    public int giver;
    /** Treaty kinds only. */
    public String typeId = "";
    /** BREAK_TREATY only: whose treaty with the giver goes. 0 means the other signatory. */
    public int with;
    /** TRANSFER_FACTION_POINTS only. */
    public float amount;
    /** CEDE_SYSTEM only: the system's coordinates, not a sector's. */
    public Vector3i system = new Vector3i();
    /** SIGN_TREATY only: when the treaty lapses, or 0 for never. */
    public long expiresAt;
    /** SIGN_TREATY only: what the proposer wants it called, or empty to have the server name it. */
    public String name = "";

    public SettlementTerm() {
    }

    public SettlementTerm(Kind kind, int giver, String typeId, int with, float amount) {
        this.kind = kind;
        this.giver = giver;
        this.typeId = typeId == null ? "" : typeId;
        this.with = with;
        this.amount = amount;
    }

    public static SettlementTerm signTreaty(int giver, String typeId) {
        return new SettlementTerm(Kind.SIGN_TREATY, giver, typeId, 0, 0);
    }

    public static SettlementTerm signTreaty(int giver, String typeId, long expiresAt, String name) {
        SettlementTerm t = signTreaty(giver, typeId);
        t.expiresAt = expiresAt;
        t.name = name == null ? "" : name;
        return t;
    }

    public static SettlementTerm breakTreaty(int giver, String typeId, int with) {
        return new SettlementTerm(Kind.BREAK_TREATY, giver, typeId, with, 0);
    }

    public static SettlementTerm points(int giver, float amount) {
        return new SettlementTerm(Kind.TRANSFER_FACTION_POINTS, giver, "", 0, amount);
    }

    public static SettlementTerm cedeSystem(int giver, Vector3i system) {
        SettlementTerm t = new SettlementTerm(Kind.CEDE_SYSTEM, giver, "", 0, 0);
        t.system = new Vector3i(system);
        return t;
    }

    /** The signatory that is not the giver. */
    public int receiver(int a, int b) {
        return giver == a ? b : a;
    }

    /** BREAK_TREATY: the faction on the other end of the treaty being broken. */
    public int breakWith(int a, int b) {
        return with != 0 ? with : receiver(a, b);
    }

    /**
     * What is wrong with this term on its own, for a settlement between {@code a} and {@code b}, or null.
     * State checks - does the giver have the points, does the treaty exist - happen at signing, against
     * the world as it is then.
     */
    public String problem(int a, int b) {
        if (kind == null) {
            return "a term of an unknown kind";
        }
        if (giver != a && giver != b) {
            return "a term given by a faction that is not a party to it";
        }
        switch (kind) {
            case TRANSFER_FACTION_POINTS:
                return amount > 0 && Float.isFinite(amount) ? null : "a faction point transfer that is not a positive amount";
            case CEDE_SYSTEM:
                return system != null ? null : "a system with no coordinates";
            case SIGN_TREATY:
            case BREAK_TREATY: {
                TreatyType type = TreatyTypeRegistry.get(typeId);
                if (type == null) {
                    return "a treaty type that does not exist: " + typeId;
                }
                if (TreatyTypeRegistry.FEDERATION.equals(typeId)) {
                    return "federation membership, which is not signed";
                }
                if (kind == Kind.SIGN_TREATY && type.getImplies() == FactionRelation.RType.ENEMY) {
                    return "a war, which is declared rather than agreed";
                }
                if (kind == Kind.BREAK_TREATY && type.getImplies() == FactionRelation.RType.ENEMY) {
                    return "a war, which ends in peace rather than being broken";
                }
                if (kind == Kind.BREAK_TREATY && breakWith(a, b) == giver) {
                    return "a treaty with itself";
                }
                return null;
            }
            default:
                return "a term of an unknown kind";
        }
    }

    /**
     * The treaty a deal is called after: the most important one it signs, the first of those on a tie,
     * or null when it signs none. See {@link TreatyType#getImportance}.
     */
    public static SettlementTerm headline(List<SettlementTerm> terms) {
        SettlementTerm best = null;
        int bestImportance = Integer.MIN_VALUE;
        for (SettlementTerm t : terms) {
            TreatyType type = t.kind == Kind.SIGN_TREATY ? TreatyTypeRegistry.get(t.typeId) : null;
            if (type != null && type.getImportance() > bestImportance) {
                best = t;
                bestImportance = type.getImportance();
            }
        }
        return best;
    }

    /** First problem in the list, or null when every term stands on its own. */
    public static String problem(List<SettlementTerm> terms, int a, int b) {
        for (SettlementTerm t : terms) {
            String p = t.problem(a, b);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    /** For the inbox and the propose dialog: "Ring Republic pays 200 faction points". */
    public String describe(IntFunction<String> nameOf, int a, int b) {
        String who = nameOf.apply(giver);
        TreatyType type = TreatyTypeRegistry.get(typeId);
        String typeName = type != null ? type.getDisplayName() : typeId;
        switch (kind) {
            case TRANSFER_FACTION_POINTS:
                return who + " pays " + Math.round(amount) + " faction points to " + nameOf.apply(receiver(a, b));
            case SIGN_TREATY:
                return who + " signs " + typeName + (name.isEmpty() ? "" : " \"" + name + "\"")
                        + (expiresAt > 0 ? " for " + remaining(expiresAt) : "");
            case BREAK_TREATY:
                return who + " breaks its " + typeName + " with " + nameOf.apply(breakWith(a, b));
            case CEDE_SYSTEM:
                return who + " cedes " + videogoose.betterfactions.DiplomacyManager.systemLabel(system) + " to " + nameOf.apply(receiver(a, b));
            default:
                return String.valueOf(kind);
        }
    }

    private static String remaining(long expiresAt) {
        long hours = Math.max(0, expiresAt - System.currentTimeMillis()) / (1000L * 60L * 60L);
        return hours < 24 ? hours + " hours" : hours / 24 + " days";
    }

    /** One line per term, each starting "- ", or {@code none} for an empty list. */
    public static String describeAll(List<SettlementTerm> terms, IntFunction<String> nameOf, int a, int b, String none) {
        if (terms.isEmpty()) {
            return none;
        }
        StringBuilder s = new StringBuilder();
        for (SettlementTerm t : terms) {
            if (s.length() > 0) {
                s.append("\n");
            }
            s.append("- ").append(t.describe(nameOf, a, b));
        }
        return s.toString();
    }

    public static Tag listToTag(List<SettlementTerm> terms) {
        Tag[] t = new Tag[terms.size() + 1];
        for (int i = 0; i < terms.size(); i++) {
            t[i] = terms.get(i).toTag();
        }
        t[terms.size()] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    /** Replaces {@code into} with what {@link #listToTag} wrote. */
    public static void listFromTag(Tag tag, List<SettlementTerm> into) {
        into.clear();
        for (Tag t : (Tag[]) tag.getValue()) {
            if (t.getType() != Tag.Type.FINISH) {
                into.add(fromTag(t));
            }
        }
    }

    public Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                DiplomacyTags.write(kind),
                new Tag(Tag.Type.INT, null, giver),
                new Tag(Tag.Type.STRING, null, typeId),
                new Tag(Tag.Type.INT, null, with),
                new Tag(Tag.Type.FLOAT, null, amount),
                new Tag(Tag.Type.VECTOR3i, null, new Vector3i(system)),
                new Tag(Tag.Type.LONG, null, expiresAt),
                new Tag(Tag.Type.STRING, null, name),
                FinishTag.INST});
    }

    /** Null for a kind this build does not know, so the caller can refuse the whole settlement. */
    public static SettlementTerm fromTag(Tag tag) {
        Tag[] s = (Tag[]) tag.getValue();
        Kind kind = DiplomacyTags.read(Kind.class, s[0]);
        SettlementTerm t = new SettlementTerm(kind, (Integer) s[1].getValue(), (String) s[2].getValue(),
                (Integer) s[3].getValue(), (Float) s[4].getValue());
        //appended with CEDE_SYSTEM; a term saved before it stops at the finish tag
        if (s.length > 5 && s[5].getType() == Tag.Type.VECTOR3i) {
            t.system = new Vector3i((Vector3i) s[5].getValue());
        }
        //appended with per-treaty terms, when a deal stopped having a treaty of its own
        if (s.length > 7 && s[6].getType() == Tag.Type.LONG) {
            t.expiresAt = (Long) s[6].getValue();
            t.name = (String) s[7].getValue();
        }
        return t;
    }

    public void serialize(DataOutput out) throws IOException {
        out.writeUTF(kind.name());
        out.writeInt(giver);
        out.writeUTF(typeId);
        out.writeInt(with);
        out.writeFloat(amount);
        system.serialize(out);
        out.writeLong(expiresAt);
        out.writeUTF(name);
    }

    public static SettlementTerm deserialize(DataInput in) throws IOException {
        Kind kind = DiplomacyTags.byName(Kind.class, in.readUTF());
        SettlementTerm t = new SettlementTerm(kind, in.readInt(), in.readUTF(), in.readInt(), in.readFloat());
        t.system = Vector3i.deserializeStatic(in);
        t.expiresAt = in.readLong();
        t.name = in.readUTF();
        return t;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SettlementTerm t && t.kind == kind && t.giver == giver && t.typeId.equals(typeId)
                && t.with == with && Float.compare(t.amount, amount) == 0 && t.system.equals(system)
                && t.expiresAt == expiresAt && t.name.equals(name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(kind, giver, typeId, with, amount, system, expiresAt, name);
    }

    @Override
    public String toString() {
        return kind + "(" + giver + ", " + typeId + ", " + with + ", " + amount + ", " + system + ")";
    }
}
