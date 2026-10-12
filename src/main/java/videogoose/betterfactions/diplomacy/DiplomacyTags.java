package videogoose.betterfactions.diplomacy;

import org.schema.schine.resource.tag.Tag;

/**
 * Tag helpers shared by the opinion ledger.
 *
 * <p>Every diplomacy tag used to key its enums by {@code ordinal()} and read them back with
 * {@code values()[b]}. That made the declaration order of {@link DiplomacyAction.DiplActionType} and
 * {@link DiplomacyEntity.DiplStatusType} load-bearing: inserting a value anywhere but the end
 * silently reassigned every value after it in already-saved worlds, and deleting one shifted the
 * whole tail. Since growing that vocabulary is the point of the diplomacy work, the tags key by
 * {@code name()} instead.</p>
 *
 * <p>Names also make the format tolerant of a vocabulary that shrinks: a name nothing recognises -
 * a value removed since the save was written, or one contributed by a mod that is no longer
 * installed - reads back as null and the entry is skipped, rather than throwing or, worse, quietly
 * resolving to whatever enum constant now sits at that index.</p>
 */
final class DiplomacyTags {

    private DiplomacyTags() {
    }

    /**
     * Reads an enum written by {@link #write}, or null if the name is not one we know.
     */
    static <E extends Enum<E>> E read(Class<E> type, Tag tag) {
        if (tag.getType() == Tag.Type.BYTE) {
            //legacy ordinal form
            int ordinal = tag.getByte();
            E[] values = type.getEnumConstants();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
        }
        return byName(type, tag.getString());
    }

    /**
     * Resolves a constant by name without throwing on an unknown one.
     */
    static <E extends Enum<E>> E byName(Class<E> type, String name) {
        if (name == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Tag write(Enum<?> value) {
        return new Tag(Tag.Type.STRING, null, value.name());
    }
}
