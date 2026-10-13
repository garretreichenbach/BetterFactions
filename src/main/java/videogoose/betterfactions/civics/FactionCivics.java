package videogoose.betterfactions.civics;

import org.schema.schine.common.language.Lng;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * One faction's civics: what it holds, slot by slot, and when it last swapped one.
 *
 * <p>Slots fill in order, so {@code civics.get(i)} is slot {@code i} and a pick into slot
 * {@code civics.size()} is a new one. Picking into an empty slot is free; replacing a held civic is a
 * swap, which costs faction points and starts the cooldown. Once filled, a slot is never emptied.</p>
 */
public final class FactionCivics {

    public static final int STARTING_SLOTS = 3;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    /** Civic ids by slot. May hold an id whose mod is gone; it stays until swapped. */
    public final List<String> civics = new ArrayList<>();
    public int slotsEarned = STARTING_SLOTS;
    /** Wall clock of the last swap, 0 for never. */
    public long lastSwap;

    public boolean isSwap(int slot) {
        return slot < civics.size();
    }

    /**
     * Why {@code id} cannot go in {@code slot} next to the civics in the other slots, or null when it
     * can. Cost and cooldown aside: this is what the chooser greys out.
     */
    public String conflict(int slot, String id) {
        CivicType type = CivicRegistry.get(id);
        if (type == null) {
            return Lng.str("There is no such civic.");
        }
        for (int i = 0; i < civics.size(); i++) {
            String held = civics.get(i);
            if (held.equals(id)) {
                return i == slot ? Lng.str("You already hold it.") : Lng.str("You already hold it in another slot.");
            }
            CivicType other = CivicRegistry.get(held);
            if (i != slot && other != null && type.conflictsWith(other)) {
                return Lng.str("It cannot be held alongside %s.", other.getDisplayName());
            }
        }
        return null;
    }

    /**
     * Why this pick is refused, or null when it may go ahead.
     *
     * @param points     the faction's faction points
     * @param swapCost   faction points a swap costs
     * @param cooldownMs how long after a swap the next one may be made
     */
    public String problem(int slot, String id, float points, long now, int swapCost, long cooldownMs) {
        if (slot < 0 || slot >= slotsEarned || slot > civics.size()) {
            return Lng.str("That slot is not open.");
        }
        String c = conflict(slot, id);
        if (c != null) {
            return c;
        }
        if (isSwap(slot)) {
            long wait = lastSwap + cooldownMs - now;
            if (lastSwap > 0 && wait > 0) {
                return Lng.str("You can swap a civic again in %s days.", (wait + DAY_MS - 1) / DAY_MS);
            }
            if (points < swapCost) {
                return Lng.str("Swapping a civic costs %s faction points.", swapCost);
            }
        }
        return null;
    }

    /** Applies a pick {@link #problem} allowed. */
    public void set(int slot, String id, long now) {
        if (isSwap(slot)) {
            civics.set(slot, id);
            lastSwap = now;
        } else {
            civics.add(id);
        }
    }

    /** The held civics' names, comma separated; empty when none. */
    public String names() {
        StringBuilder b = new StringBuilder();
        for (String id : civics) {
            if (!b.isEmpty()) {
                b.append(", ");
            }
            b.append(CivicRegistry.nameOf(id));
        }
        return b.toString();
    }

    /** The government the held civics bring, first slot first; null when none does. */
    public String government() {
        for (String id : civics) {
            CivicType t = CivicRegistry.get(id);
            if (t != null && t.getImpliesGovernment() != null) {
                return t.getImpliesGovernment();
            }
        }
        return null;
    }

    public Tag toTag() {
        Tag[] ids = new Tag[civics.size() + 1];
        for (int i = 0; i < civics.size(); i++) {
            ids[i] = new Tag(Tag.Type.STRING, null, civics.get(i));
        }
        ids[civics.size()] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.STRUCT, null, ids),
                new Tag(Tag.Type.INT, null, slotsEarned),
                new Tag(Tag.Type.LONG, null, lastSwap),
                FinishTag.INST});
    }

    public static FactionCivics fromTag(Tag tag) {
        FactionCivics c = new FactionCivics();
        Tag[] t = tag.getStruct();
        for (Tag id : t[0].getStruct()) {
            if (id.getType() == Tag.Type.STRING) {
                c.civics.add(id.getString());
            }
        }
        c.slotsEarned = t[1].getInt();
        c.lastSwap = t[2].getLong();
        return c;
    }
}
