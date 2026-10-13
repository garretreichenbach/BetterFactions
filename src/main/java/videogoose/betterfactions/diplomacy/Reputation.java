package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

/**
 * What the whole galaxy remembers about a faction, whoever it was done to: the treaties it broke
 * (infamy) and the fights it started (aggression). A ledger entry only knows what a faction did to
 * its owner; this is what everyone else heard about.
 *
 * <p>Both fade with a half-life ({@code reputation_half_life_days}), so a faction that stops
 * misbehaving recovers. Server-owned; clients get a copy for the diplomacy card.</p>
 */
public final class Reputation {

    /** Infamy one broken treaty adds. */
    public static final double PER_BREAK = 1;
    /** Aggression one declared war adds. */
    public static final double PER_WAR = 1;
    /** Aggression a war with no casus belli adds on top of {@link #PER_WAR}. */
    public static final double PER_UNJUST_WAR = 2;
    /** Aggression one unprovoked attack outside a war adds - an engagement, not a hit. */
    public static final double PER_ATTACK = 0.25;
    /**
     * Hits between the same pair this close together are one engagement, and the side that is shot
     * at first and answers inside it is defending, not starting anything.
     */
    static final long ENGAGEMENT_MS = 10 * 60000L;
    /** Below this a score is forgotten rather than decayed forever. */
    private static final double FORGOTTEN = 0.01;

    private final Int2DoubleOpenHashMap infamy = new Int2DoubleOpenHashMap();
    private final Int2DoubleOpenHashMap aggression = new Int2DoubleOpenHashMap();
    /** Ordered pair (aggressor, victim) to the last hit of their current engagement. Not saved. */
    private final Long2LongOpenHashMap lastHit = new Long2LongOpenHashMap();
    private long lastDecay;
    private boolean dirty;

    public synchronized double infamyOf(int faction) {
        return infamy.get(faction);
    }

    public synchronized double aggressionOf(int faction) {
        return aggression.get(faction);
    }

    public synchronized void brokeTreaty(int faction) {
        add(infamy, faction, PER_BREAK);
        dirty = true;
    }

    public synchronized void declaredWar(int faction) {
        add(aggression, faction, PER_WAR);
        dirty = true;
    }

    public synchronized void declaredUnjustWar(int faction) {
        add(aggression, faction, PER_UNJUST_WAR);
        dirty = true;
    }

    /**
     * An attack outside a declared war. Counts once per engagement, and only against whoever fired
     * first in it.
     *
     * @return whether it added anything
     */
    public synchronized boolean attacked(int aggressor, int victim, long now) {
        if (inEngagement(pair(victim, aggressor), now)) {
            //they started this one; shooting back is not aggression
            return false;
        }
        long key = pair(aggressor, victim);
        boolean ongoing = inEngagement(key, now);
        lastHit.put(key, now);
        if (ongoing) {
            return false;
        }
        add(aggression, aggressor, PER_ATTACK);
        dirty = true;
        return true;
    }

    private static void add(Int2DoubleOpenHashMap m, int faction, double v) {
        m.put(faction, m.get(faction) + v);
    }

    private boolean inEngagement(long key, long now) {
        return lastHit.containsKey(key) && now - lastHit.get(key) < ENGAGEMENT_MS;
    }

    private static long pair(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    /**
     * Server: fades every score by the time since the last call. Server uptime only - a world that
     * sat switched off for a month did not spend that month being forgiven.
     *
     * @return whether clients need a fresh copy
     */
    public synchronized boolean decay(long now, double halfLifeDays) {
        long dt = lastDecay == 0 ? 0 : now - lastDecay;
        lastDecay = now;
        if (dt > 0 && halfLifeDays > 0) {
            double factor = Math.pow(0.5, dt / (halfLifeDays * 86400000.0));
            dirty |= fade(infamy, factor) | fade(aggression, factor);
        }
        for (it.unimi.dsi.fastutil.longs.LongIterator i = lastHit.values().iterator(); i.hasNext(); ) {
            if (now - i.nextLong() >= ENGAGEMENT_MS) {
                i.remove();
            }
        }
        boolean d = dirty;
        dirty = false;
        return d;
    }

    private static boolean fade(Int2DoubleOpenHashMap m, double factor) {
        boolean any = !m.isEmpty();
        for (ObjectIterator<Int2DoubleMap.Entry> i = m.int2DoubleEntrySet().iterator(); i.hasNext(); ) {
            Int2DoubleMap.Entry e = i.next();
            double v = e.getDoubleValue() * factor;
            if (v < FORGOTTEN) {
                i.remove();
            } else {
                e.setValue(v);
            }
        }
        return any;
    }

    public synchronized void removeFaction(int faction) {
        dirty |= infamy.remove(faction) != 0 | aggression.remove(faction) != 0;
    }

    public synchronized Tag toTag() {
        IntOpenHashSet factions = new IntOpenHashSet(infamy.keySet());
        factions.addAll(aggression.keySet());
        Tag[] t = new Tag[factions.size() + 1];
        int i = 0;
        for (int f : factions) {
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.INT, null, f),
                    new Tag(Tag.Type.DOUBLE, null, infamy.get(f)),
                    new Tag(Tag.Type.DOUBLE, null, aggression.get(f)), FinishTag.INST});
        }
        t[t.length - 1] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    public synchronized void fromTag(Tag tag) {
        infamy.clear();
        aggression.clear();
        Tag[] t = tag.getStruct();
        for (int i = 0; i < t.length - 1; i++) {
            Tag[] e = t[i].getStruct();
            int f = e[0].getInt();
            double inf = (Double) e[1].getValue();
            double agg = (Double) e[2].getValue();
            if (inf > 0) {
                infamy.put(f, inf);
            }
            if (agg > 0) {
                aggression.put(f, agg);
            }
        }
    }

    public synchronized void clear() {
        infamy.clear();
        aggression.clear();
        lastHit.clear();
        lastDecay = 0;
        dirty = false;
    }
}
