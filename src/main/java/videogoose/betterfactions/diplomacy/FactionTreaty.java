package videogoose.betterfactions.diplomacy;

import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

/**
 * One agreement in force between two factions.
 *
 * <p>Asymmetry lives here, in {@link #from} and {@link #to}, rather than in the storage key. That is
 * the whole reason vassalage and tributaries can exist at all: the old relation table keyed pairs by
 * {@code min(a,b), max(a,b)}, so it could not tell an overlord from a vassal. Treaties are stored
 * per unordered pair and carry their own direction, which keeps one map and one lookup while still
 * allowing a treaty where the two sides hold different roles.</p>
 *
 * <p>Persisted by type id string, never by ordinal - see {@link DiplomacyTags}.</p>
 */
public class FactionTreaty {

    /**
     * Never expires.
     */
    public static final long NO_EXPIRY = Long.MAX_VALUE;
    private static final byte VERSION = 0;
    /**
     * Proposer, or for an asymmetric treaty the side holding the senior role (the overlord).
     */
    public int from;
    /**
     * The other signatory; for an asymmetric treaty, the junior side (the vassal).
     */
    public int to;
    public String typeId;
    public long signedAt;
    public long expiresAt = NO_EXPIRY;
    /**
     * Free text the proposer attached. Flavour, kept so an RP server can read its own history.
     */
    public String clause = "";
    /**
     * What this agreement is called. Empty means it has not been named, which only happens for a
     * treaty written before names existed - {@link TreatyNames} fills one in at signing otherwise.
     * Both signatories see the same name, because it is agreed as part of the offer.
     */
    public String name = "";

    /**
     * The name to show: the agreed one, falling back to the type for a treaty from a save that
     * predates names.
     */
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        TreatyType type = getType();
        return type != null ? type.getDisplayName() : typeId;
    }

    public FactionTreaty() {
    }

    public FactionTreaty(int from, int to, String typeId, long signedAt) {
        this.from = from;
        this.to = to;
        this.typeId = typeId;
        this.signedAt = signedAt;
    }

    /**
     * @return null if nothing has registered this id - a modded treaty whose mod is gone.
     */
    public TreatyType getType() {
        return TreatyTypeRegistry.get(typeId);
    }

    public boolean involves(int factionId) {
        return from == factionId || to == factionId;
    }

    /**
     * The signatory that is not the given one.
     */
    public int other(int factionId) {
        return from == factionId ? to : from;
    }

    public boolean isExpired(long now) {
        return expiresAt != NO_EXPIRY && now >= expiresAt;
    }

    @Override
    public String toString() {
        return "Treaty[" + typeId + " " + from + "->" + to + "]";
    }

    public Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, VERSION),
                new Tag(Tag.Type.INT, null, from),
                new Tag(Tag.Type.INT, null, to),
                new Tag(Tag.Type.STRING, null, typeId),
                new Tag(Tag.Type.LONG, null, signedAt),
                new Tag(Tag.Type.LONG, null, expiresAt),
                new Tag(Tag.Type.STRING, null, clause == null ? "" : clause),
                new Tag(Tag.Type.STRING, null, name == null ? "" : name),
                FinishTag.INST,
        });
    }

    /**
     * @return false if the type id is not registered, in which case the treaty is dropped rather
     * than loaded as something the engine cannot reason about.
     */
    public boolean fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        from = t[1].getInt();
        to = t[2].getInt();
        typeId = t[3].getString();
        signedAt = t[4].getLong();
        expiresAt = t[5].getLong();
        clause = t[6].getString();
        //appended after treaties already existed, so a save or a client one version behind stops here
        name = t.length > 7 && t[7].getType() != Tag.Type.FINISH ? t[7].getString() : "";
        return TreatyTypeRegistry.exists(typeId);
    }
}
