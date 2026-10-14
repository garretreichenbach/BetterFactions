package videogoose.betterfactions.civics;

import org.schema.schine.common.language.Lng;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    /** Ratchets up with strength ({@link #slotsFor}) and never comes down. */
    public int slotsEarned = STARTING_SLOTS;
    /** Wars that ended with this faction ahead; for the WARS_WON_AT_LEAST unlock rule. */
    public int warsWon;
    /** Civic ids an admin granted this faction; for the ADMIN_GRANTED unlock rule. */
    public final Set<String> granted = new LinkedHashSet<>();
    /** Day number ({@link #dayOf}) {@link #paidToday} and today's slot of {@link #income} are for. */
    public long day;
    /** Faction points paid today under each payout's key, for its daily cap. */
    public final java.util.Map<String, Integer> paidToday = new java.util.HashMap<>();
    /** Net civic income by day, the slot {@code day % 7}; the last seven days summed is the week's. */
    public final int[] income = new int[7];
    /** Day number the DAILY payouts last ran for. */
    public long dailyDone;
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
    /** Senate: what the faction calls it; empty for the default. */
    public String senateName = "";
    public static final int MAX_SENATE_NAME = 32;

    /** Slots a faction of {@code strength} has earned: the starting ones plus one per milestone reached. */
    public static int slotsFor(double strength, int[] milestones) {
        int n = STARTING_SLOTS;
        for (int m : milestones) {
            if (strength >= m) {
                n++;
            }
        }
        return n;
    }

    /** Held civics that would be dropped with the one in {@code slot}, because they need it, directly or not. */
    public List<String> dependants(int slot) {
        List<String> out = new ArrayList<>();
        if (slot < 0 || slot >= civics.size()) {
            return out;
        }
        List<String> gone = new ArrayList<>(List.of(civics.get(slot)));
        for (int i = 0; i < gone.size(); i++) {
            for (String held : civics) {
                CivicType t = CivicRegistry.get(held);
                if (t != null && t.getRequires().contains(gone.get(i)) && !gone.contains(held)) {
                    gone.add(held);
                    out.add(held);
                }
            }
        }
        return out;
    }

    public static long dayOf(long wallMs) {
        return Math.floorDiv(wallMs, 24L * 60L * 60L * 1000L);
    }

    /** Moves the ledger on to {@code today}: yesterday's caps reset, and days past drop out of the week. */
    void roll(long today) {
        if (today <= day) {
            return;
        }
        for (long d = day + 1; d <= today && d <= day + 7; d++) {
            income[(int) Math.floorMod(d, 7L)] = 0;
        }
        paidToday.clear();
        day = today;
    }

    /**
     * What a payout of {@code fp} actually pays once its daily cap is applied, recorded against today.
     * Costs are never capped.
     *
     * @param dailyCap 0 for none
     */
    public int credit(String key, int fp, int dailyCap, long now) {
        roll(dayOf(now));
        if (fp > 0 && dailyCap > 0) {
            fp = Math.max(0, Math.min(fp, dailyCap - paidToday.getOrDefault(key, 0)));
        }
        if (fp > 0) {
            paidToday.merge(key, fp, Integer::sum);
        }
        income[(int) Math.floorMod(day, 7L)] += fp;
        return fp;
    }

    /** Net civic income over the last seven days, today included. */
    public int weekIncome(long now) {
        long today = dayOf(now);
        int sum = 0;
        for (long d = Math.max(day - 6, today - 6); d <= day; d++) {
            sum += income[(int) Math.floorMod(d, 7L)];
        }
        return sum;
    }

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
     * @param standing   what the unlock rules measure about the faction now
     * @param points     the faction's faction points
     * @param swapCost   faction points a swap costs
     * @param rushCost   extra faction points to swap during the cooldown; 0 if it cannot be skipped
     * @param cooldownMs how long after a swap the next one may be made
     */
    public String problem(int slot, String id, CivicType.Standing standing, float points, long now, int swapCost, int rushCost, long cooldownMs) {
        if (slot != DOCTRINE && (slot < 0 || slot >= slotsEarned || slot > civics.size())) {
            return Lng.str("That slot is not open.");
        }
        String c = conflict(slot, id);
        if (c != null) {
            return c;
        }
        String locked = CivicRegistry.get(id).locked(standing);
        if (locked != null) {
            return Lng.str("Locked. %s", locked);
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

    /** Applies a pick {@link #problem} allowed; see the other {@code set}. */
    public List<String> set(int slot, String id, long now) {
        return set(slot, id, (short) 0, now);
    }

    /**
     * Applies a pick {@link #problem} allowed. Replacing a civic others need drops them too, and the
     * slots after close up.
     *
     * @param resource kept only when {@code id} {@link CivicType#choosesResource}
     * @return the civics dropped because they needed the one replaced
     */
    public List<String> set(int slot, String id, short resource, long now) {
        CivicType type = CivicRegistry.get(id);
        if (type != null && type.choosesResource()) {
            this.resource = resource;
        }
        if (slot == DOCTRINE) {
            if (doctrine != null) {
                lastSwap = now;
            }
            doctrine = id;
            return List.of();
        }
        if (!isSwap(slot)) {
            civics.add(id);
            return List.of();
        }
        List<String> dropped = dependants(slot);
        civics.set(slot, id);
        civics.removeAll(dropped);
        lastSwap = now;
        return dropped;
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
                new Tag(Tag.Type.STRING, null, senateName),
                new Tag(Tag.Type.INT, null, warsWon),
                strings(new ArrayList<>(granted)),
                new Tag(Tag.Type.LONG, null, day),
                ledger(),
                new Tag(Tag.Type.LONG, null, dailyDone),
                FinishTag.INST});
    }

    /** Today's caps as (key, paid) pairs, then the week's income. */
    private Tag ledger() {
        Tag[] t = new Tag[paidToday.size() + income.length + 1];
        int i = 0;
        for (java.util.Map.Entry<String, Integer> e : paidToday.entrySet()) {
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRING, null, e.getKey()), new Tag(Tag.Type.INT, null, e.getValue()), FinishTag.INST});
        }
        for (int v : income) {
            t[i++] = new Tag(Tag.Type.INT, null, v);
        }
        t[i] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    private static void readStrings(Tag t, java.util.Collection<String> into) {
        for (Tag id : t.getStruct()) {
            if (id.getType() == Tag.Type.STRING) {
                into.add(id.getString());
            }
        }
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
        c.senateName = t.length > 9 && t[9].getType() == Tag.Type.STRING ? t[9].getString() : "";
        c.warsWon = t.length > 10 && t[10].getType() == Tag.Type.INT ? t[10].getInt() : 0;
        if (t.length > 11 && t[11].getType() == Tag.Type.STRUCT) {
            readStrings(t[11], c.granted);
        }
        if (t.length > 14 && t[12].getType() == Tag.Type.LONG && t[13].getType() == Tag.Type.STRUCT) {
            c.day = t[12].getLong();
            int d = 0;
            for (Tag e : t[13].getStruct()) {
                if (e.getType() == Tag.Type.STRUCT) {
                    c.paidToday.put(e.getStruct()[0].getString(), e.getStruct()[1].getInt());
                } else if (e.getType() == Tag.Type.INT && d < c.income.length) {
                    c.income[d++] = e.getInt();
                }
            }
            c.dailyDone = t[14].getLong();
        }
        return c;
    }

    /** The Senate's name as shown: its own, or "Senate". */
    public String senateName() {
        return senateName.isEmpty() ? Lng.str("Senate") : senateName;
    }

    /** A name as sent, made valid: trimmed and capped; empty for the default. */
    public static String cleanSenateName(String name) {
        name = name == null ? "" : name.strip();
        return name.length() > MAX_SENATE_NAME ? name.substring(0, MAX_SENATE_NAME).strip() : name;
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
