package videogoose.betterfactions.diplomacy;

import org.schema.common.util.StringTools;
import org.schema.schine.common.language.Lng;
import org.schema.schine.common.language.Translatable;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

public class DiplomacyAction {

    private static final byte VERSION = 1;
    public DiplActionType type;
    public String subject;
    public int counter;
    public long timeDuration;

    public static String key(DiplActionType type, String subject) {
        return subject == null ? type.name() : type.name() + ":" + subject;
    }

    /**
     * Identity of this action within a ledger entry.
     */
    public String key() {
        return key(type, subject);
    }

    public Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, VERSION),
                new Tag(Tag.Type.INT, null, counter),
                new Tag(Tag.Type.LONG, null, timeDuration),
                DiplomacyTags.write(type),
                new Tag(Tag.Type.STRING, null, subject == null ? "" : subject),
                FinishTag.INST,
        });
    }

    /**
     * @return false if the action type is not one this build knows, in which case skip the entry.
     */
    public boolean fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        byte version = t[0].getByte();

        counter = t[1].getInt();
        timeDuration = t[2].getLong();
        type = DiplomacyTags.read(DiplActionType.class, t[3]);
        if (version >= 1) {
            String s = t[4].getString();
            subject = s == null || s.isEmpty() ? null : s;
        }
        return type != null;
    }

    /**
     * Something that happened, as opposed to a state of affairs - see
     * {@link DiplomacyEntity.DiplStatusType} for those.
     *
     * <p>The {@code TREATY_*} entries are subject-keyed by treaty id (see {@link #subject}) and fire
     * for every treaty type, including the three the older entries above already name. They carry no
     * points by default: {@code DECLARATION_OF_WAR}, {@code ALLIANCE_REQUEST}, {@code PEACE_OFFER}
     * and {@code ALLIANCE_CANCEL} still hold the tuned values for the relations they describe,
     * because a config element's value is per action type and cannot differ per subject. What the
     * generic entries add is a record an acceptance rule can ask about - "they have broken a pact
     * with us before" - which nothing else could express.</p>
     */
    public enum DiplActionType {
        ATTACK(en -> Lng.str("Attacked them")),
        ATTACK_ENEMY(en -> Lng.str("Attacked their enemies")),
        MINING(en -> Lng.str("Mined their resources")),
        TERRITORY(en -> Lng.str("Violated their territory")),
        PEACE_OFFER(en -> Lng.str("Peace offering made")),
        DECLARATION_OF_WAR(en -> Lng.str("Declared war")),
        ALLIANCE_REQUEST(en -> Lng.str("Requested Alliance")),
        ALLIANCE_CANCEL(en -> Lng.str("Canceled Alliance")),
        TRADING_WITH_US(en -> Lng.str("Traded with them")),
        TRADING_WITH_ENEMY(en -> Lng.str("Traded with their enemies")),
        ALLIANCE_WITH_ENEMY(en -> Lng.str("In alliance with their enemies")),
        TREATY_PROPOSED(en -> Lng.str("Offered us a treaty")),
        TREATY_SIGNED(en -> Lng.str("Signed a treaty with us")),
        TREATY_REFUSED(en -> Lng.str("Turned down a treaty")),
        TREATY_BROKEN(en -> Lng.str("Broke a treaty with us")),
        TREATY_EXPIRED(en -> Lng.str("Let a treaty lapse"));

        private final Translatable description;

        DiplActionType(Translatable description) {
            this.description = description;
        }

        public static String list() {
            return StringTools.listEnum(values());
        }

        /**
         * The mod's equivalent of one of the engine's own opinion events, or null for one it does not
         * take. TERRITORY is refused: the engine fires it from the NPC expansion scan on finding any
         * adjacent foreign system, which would make it a penalty for existing nearby.
         */
        public static DiplActionType fromVanilla(Enum<?> vanilla) {
            if (vanilla == null || "TERRITORY".equals(vanilla.name())) {
                return null;
            }
            try {
                return valueOf(vanilla.name());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public String getDescription() {
            return description.getName(this);
        }
    }
}
