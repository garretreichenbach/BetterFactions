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
    /** The slot number of the fleet doctrine, which sits beside the civic slots rather than among them. */
    public static final int DOCTRINE = -1;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    /** Civic ids by slot. May hold an id whose mod is gone; it stays until swapped. */
    public final List<String> civics = new ArrayList<>();
    public int slotsEarned = STARTING_SLOTS;
    /** Wall clock of the last swap, 0 for never. */
    public long lastSwap;
    /** Block id of the resource picked with a civic that {@link CivicType#choosesResource}; 0 for none. */
    public short resource;
    /** The fleet doctrine's id; null until one is picked. Swapping it shares the civics' cost and cooldown. */
    public String doctrine;
    /** Institution ids in the order adopted; the later an institution was adopted, the more upkeep it adds. */
    public final List<String> institutions = new ArrayList<>();
    /** Senate: a bit per rank index that sits; 0 seats every rank. The founder's bit is always set otherwise. */
    public int senateSeats;
    /** Senate: how long a vote stays open, in hours; 0 for the server's default. */
    public int senateHours;
    /** Senate: a vote that passes waits for the founder to assent or veto, until its time runs out. */
    public boolean senateVeto;

    public boolean isSwap(int slot) {
        return slot == DOCTRINE ? doctrine != null : slot < civics.size();
    }

    /** What {@code slot} holds; null when empty. */
    public String at(int slot) {
        return slot == DOCTRINE ? doctrine : slot >= 0 && slot < civics.size() ? civics.get(slot) : null;
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
        if (type.getKind() == CivicType.Kind.INSTITUTION) {
            return Lng.str("Institutions are adopted, not put in a slot.");
        }
        if (type.isDoctrine() != (slot == DOCTRINE)) {
            return type.isDoctrine() ? Lng.str("Fleet doctrines have their own slot.") : Lng.str("Only a fleet doctrine goes in this slot.");
        }
        if (slot == DOCTRINE) {
            return id.equals(doctrine) ? Lng.str("You already hold it.") : null;
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
            //swapping out a civic another held one requires
            if (i != slot && other != null && slot < civics.size() && other.getRequires().contains(civics.get(slot))) {
                return Lng.str("%s needs %s, which is in this slot.", other.getDisplayName(), CivicRegistry.nameOf(civics.get(slot)));
            }
        }
        for (String need : type.getRequires()) {
            int at = civics.indexOf(need);
            if (at < 0 || at == slot) {
                return Lng.str("It needs %s first.", CivicRegistry.nameOf(need));
            }
        }
        return null;
    }

    /** Days left on the swap cooldown, rounded up; 0 when a swap may be made. */
    public long cooldownDays(long now, long cooldownMs) {
        long wait = lastSwap + cooldownMs - now;
        return lastSwap > 0 && wait > 0 ? (wait + DAY_MS - 1) / DAY_MS : 0;
    }

    /**
     * Faction points a pick into {@code slot} costs now: nothing to fill a slot, the swap cost to
     * replace a civic, plus the rush cost while the cooldown runs.
     */
    public int cost(int slot, long now, int swapCost, int rushCost, long cooldownMs) {
        if (!isSwap(slot)) {
            return 0;
        }
        return cooldownDays(now, cooldownMs) > 0 ? swapCost + rushCost : swapCost;
    }

    /**
     * Why this pick is refused, or null when it may go ahead.
     *
     * @param points     the faction's faction points
     * @param swapCost   faction points a swap costs
     * @param rushCost   extra faction points to swap during the cooldown; 0 if it cannot be skipped
     * @param cooldownMs how long after a swap the next one may be made
     */
    public String problem(int slot, String id, float points, long now, int swapCost, int rushCost, long cooldownMs) {
        if (slot != DOCTRINE && (slot < 0 || slot >= slotsEarned || slot > civics.size())) {
            return Lng.str("That slot is not open.");
        }
        String c = conflict(slot, id);
        if (c != null) {
            return c;
        }
        if (isSwap(slot)) {
            long days = cooldownDays(now, cooldownMs);
            if (days > 0 && rushCost <= 0) {
                return Lng.str("You can swap a civic again in %s days.", days);
            }
            int cost = cost(slot, now, swapCost, rushCost, cooldownMs);
            if (points < cost) {
                return days > 0
                        ? Lng.str("You can swap a civic again in %s days, or now for %s faction points.", days, cost)
                        : Lng.str("Swapping a civic costs %s faction points.", cost);
            }
        }
        return null;
    }

    /** Applies a pick {@link #problem} allowed. */
    public void set(int slot, String id, long now) {
        set(slot, id, (short) 0, now);
    }

    /** @param resource kept only when {@code id} {@link CivicType#choosesResource} */
    public void set(int slot, String id, short resource, long now) {
        CivicType type = CivicRegistry.get(id);
        if (type != null && type.choosesResource()) {
            this.resource = resource;
        }
        if (slot == DOCTRINE) {
            if (doctrine != null) {
                lastSwap = now;
            }
            doctrine = id;
            return;
        }
        if (isSwap(slot)) {
            civics.set(slot, id);
            lastSwap = now;
        } else {
            civics.add(id);
        }
    }

    /**
     * Faction points a turn for the institutions held: the first costs {@code base}, the second twice that,
     * and so on, so a faction can afford a few cheaply and every one after costs more.
     */
    public static int institutionUpkeep(int count, int base) {
        return base * count * (count + 1) / 2;
    }

    /**
     * Each held civic, then the doctrine, then each institution, as "Name: what it does", one paragraph each; empty when none.
     *
     * @param resourceName the picked {@link #resource}'s name, shown after a civic that {@link CivicType#choosesResource}; null for none
     */
    public String modifiers(String resourceName) {
        List<String> all = new ArrayList<>(civics);
        if (doctrine != null) {
            all.add(doctrine);
        }
        all.addAll(institutions);
        StringBuilder b = new StringBuilder();
        for (String id : all) {
            if (!b.isEmpty()) {
                b.append("\n\n");
            }
            CivicType t = CivicRegistry.get(id);
            b.append(CivicRegistry.nameOf(id)).append(": ").append(t != null ? t.getDescription() : Lng.str("Its mod is not loaded."));
            if (t != null && t.choosesResource() && resourceName != null) {
                b.append(' ').append(Lng.str("Resource: %s.", resourceName));
            }
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
                new Tag(Tag.Type.SHORT, null, resource),
                new Tag(Tag.Type.STRING, null, doctrine == null ? "" : doctrine),
                strings(institutions),
                new Tag(Tag.Type.INT, null, senateSeats),
                new Tag(Tag.Type.INT, null, senateHours),
                new Tag(Tag.Type.BYTE, null, (byte) (senateVeto ? 1 : 0)),
                FinishTag.INST});
    }

    private static Tag strings(List<String> ids) {
        Tag[] t = new Tag[ids.size() + 1];
        for (int i = 0; i < ids.size(); i++) {
            t[i] = new Tag(Tag.Type.STRING, null, ids.get(i));
        }
        t[ids.size()] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
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
        //saves from before resources had none
        if (t.length > 3 && t[3].getType() == Tag.Type.SHORT) {
            c.resource = t[3].getShort();
        }
        if (t.length > 4 && t[4].getType() == Tag.Type.STRING && !t[4].getString().isEmpty()) {
            c.doctrine = t[4].getString();
        }
        if (t.length > 5 && t[5].getType() == Tag.Type.STRUCT) {
            for (Tag id : t[5].getStruct()) {
                if (id.getType() == Tag.Type.STRING) {
                    c.institutions.add(id.getString());
                }
            }
        }
        if (t.length > 7 && t[6].getType() == Tag.Type.INT && t[7].getType() == Tag.Type.INT) {
            c.senateSeats = t[6].getInt();
            c.senateHours = t[7].getInt();
        }
        c.senateVeto = t.length > 8 && t[8].getType() == Tag.Type.BYTE && t[8].getByte() != 0;
        return c;
    }

    /** Whether a member of this rank index sits in the Senate. */
    public boolean seats(int rank) {
        return senateSeats == 0 || rank >= 0 && rank < 32 && (senateSeats & (1 << rank)) != 0;
    }

    /** Hours a vote opened now stays open. */
    public int voteHours(int serverDefault, int max) {
        return senateHours > 0 ? Math.min(senateHours, max) : serverDefault;
    }
}
