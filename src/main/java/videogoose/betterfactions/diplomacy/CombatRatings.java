package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.FiringUnit;
import org.schema.game.common.controller.elements.ShieldContainerInterface;
import org.schema.game.common.controller.elements.power.reactor.PowerConsumer;
import org.schema.game.common.controller.elements.power.reactor.PowerInterface;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.function.IntUnaryOperator;

/**
 * What each faction can field, as a sum of per-entity combat ratings.
 *
 * <p>A rating is taken while an entity is loaded and kept after it unloads, because strength is asked
 * on the diplomacy tick about factions nowhere near a player. It is never computed on demand. The
 * server keeps one row per entity (persisted); clients only get the per-faction totals.</p>
 *
 * <p>Every faction-owned ship and station counts, in a fleet or not. Counting only fleets would rate
 * NPC factions, which live entirely in fleets, far above players, whose main ship is usually in none.
 * Docked turrets are entities of their own and count on their own.</p>
 */
public final class CombatRatings {

    /**
     * Structure counts a tenth of a weapon or shield block: a hull does soak hits, but filler should
     * not buy a rating.
     */
    static final double BULK_WEIGHT = 0.1;

    private static final class Rated {
        int faction;
        double rating;

        Rated(int faction, double rating) {
            this.faction = faction;
            this.rating = rating;
        }
    }

    /** Server only: entity db id to its last rating. */
    private final Long2ObjectOpenHashMap<Rated> byEntity = new Long2ObjectOpenHashMap<>();
    /** Derived from {@link #byEntity} on the server; received on clients. */
    private final Int2DoubleOpenHashMap totals = new Int2DoubleOpenHashMap();

    /**
     * Offence times defence, square-rooted, so a ship that cannot shoot back rates nothing however
     * much it can absorb, and one that dies to the first hit rates little however many guns it has.
     * Both sides grow with size, so a ship twice as big in every respect rates twice as much.
     *
     * <p>Block counts throughout, which keeps it free of the engine's config scales, except that
     * offence is scaled by {@code powered}: the share of its guns the reactor can keep firing (see
     * {@link #reactorGate}). A ship with more guns than its reactor can feed is rated by the guns it can
     * feed. ponytail: thrust is not read; a brick that cannot close the distance still rates.</p>
     *
     * @param weapons output blocks of every damaging weapon (cannon barrels, missile tubes, beam modules)
     * @param shields shield capacitor plus recharger blocks
     * @param blocks  total blocks
     */
    public static double rate(int weapons, int shields, int blocks) {
        return rate(weapons, shields, blocks, 1);
    }

    /** @param powered 0 to 1, the share of the weapons the reactor can keep firing */
    public static double rate(int weapons, int shields, int blocks, double powered) {
        double offence = Math.max(0, weapons) * Math.max(0, Math.min(1, powered));
        double defence = Math.max(0, shields) + Math.max(0, blocks) * BULK_WEIGHT;
        return Math.sqrt(offence * defence);
    }

    /** Server: rates a loaded entity. */
    public static double rate(SegmentController sc) {
        ElementCountMap counts = sc.getElementClassCountMap();
        int weapons = counts.get(ElementKeyMap.WEAPON_ID) + counts.get(ElementKeyMap.MISSILE_TUBE)
                + counts.get(ElementKeyMap.DAMAGE_BEAM_MODULE);
        int shields = 0;
        if (sc instanceof ManagedSegmentController<?> m && m.getManagerContainer() instanceof ShieldContainerInterface s) {
            shields = s.getShieldCapacityManager().getTotalSize() + s.getShieldRegenManager().getTotalSize();
        }
        return rate(weapons, shields, sc.getTotalElements(), reactorGate(sc));
    }

    /**
     * Reactor recharge over what every weapon draws while reloading, capped at 1: the share of the guns
     * that can fire continuously. 1 when there is nothing to read (old power, or the collections have
     * not been calculated yet just after a load). Docked turrets have their own reactors and are rated
     * on their own.
     */
    public static double reactorGate(SegmentController sc) {
        if (!(sc instanceof ManagedSegmentController<?> m)) {
            return 1;
        }
        PowerInterface power = m.getManagerContainer().getPowerInterface();
        if (power == null || !power.isUsingPowerReactors()) {
            return 1;
        }
        double need = 0;
        for (PowerConsumer c : power.getPowerConsumerList()) {
            //missile capacity, repair beams and mine layers share these categories but fire nothing
            if (c instanceof FiringUnit<?, ?, ?> && switch (c.getPowerConsumerCategory()) {
                case CANNONS, BEAMS, MISSILES -> true;
                default -> false;
            }) {
                need += c.getPowerConsumedPerSecondCharging();
            }
        }
        return need <= 0 ? 1 : Math.min(1, power.getRechargeRatePowerPerSecConfigured() / need);
    }

    /** Server: the last rating stored for this entity, 0 if it was never seen loaded. */
    public synchronized double ratingOf(long dbId) {
        Rated r = byEntity.get(dbId);
        return r != null ? r.rating : 0;
    }

    public synchronized double totalOf(int faction) {
        return totals.get(faction);
    }

    /**
     * Systems an NPC faction holds. They live in the NPC simulation rather than in the claim list a
     * player faction pays upkeep on, and only the server has them, so they ride along with the totals.
     */
    private final Int2IntOpenHashMap npcSystems = new Int2IntOpenHashMap();

    public synchronized int npcSystemsOf(int faction) {
        return npcSystems.get(faction);
    }

    /** Server. @return whether it changed */
    public synchronized boolean setNpcSystems(int faction, int systems) {
        return npcSystems.put(faction, systems) != systems;
    }

    public synchronized void put(long dbId, int faction, double rating) {
        Rated r = byEntity.get(dbId);
        if (r == null) {
            byEntity.put(dbId, new Rated(faction, rating));
        } else {
            r.faction = faction;
            r.rating = rating;
        }
    }

    /**
     * The fallback for an entity that has never been loaded: kept only while there is no rating for it.
     * A loaded rating ({@link #put}) always replaces it, and this never replaces a loaded one.
     * Synchronized like the rest, because NPC spawns call it from simulation threads.
     */
    public synchronized boolean isRated(long dbId) {
        return byEntity.containsKey(dbId);
    }

    public synchronized void putIfUnrated(long dbId, int faction, double rating) {
        if (!byEntity.containsKey(dbId)) {
            byEntity.put(dbId, new Rated(faction, rating));
        }
    }

    /**
     * Rates one entity's blueprint (its own blocks, not its docked children, which are spawned and rated
     * as entities of their own, as loaded turrets are). Same block counts as {@link #rate(SegmentController)},
     * shields read as capacitor and recharger blocks. ponytail: no reactor gate, since a blueprint has no
     * calculated power; it is only a stand-in until the entity is first loaded and rated properly.
     */
    public static double rateBlueprint(ElementCountMap counts) {
        return rateBlocks(id -> counts.get((short) id), counts.getTotalAmount());
    }

    /** {@link #rateBlueprint} over any block count lookup, so it can be checked without the block config. */
    static double rateBlocks(IntUnaryOperator count, long total) {
        int weapons = count.applyAsInt(ElementKeyMap.WEAPON_ID) + count.applyAsInt(ElementKeyMap.MISSILE_TUBE)
                + count.applyAsInt(ElementKeyMap.DAMAGE_BEAM_MODULE);
        int shields = count.applyAsInt(ElementKeyMap.SHIELD_CAP_ID) + count.applyAsInt(ElementKeyMap.SHIELD_REGEN_ID);
        return rate(weapons, shields, (int) Math.min(Integer.MAX_VALUE, total));
    }

    /**
     * Drops this faction's rows for entities it no longer owns, by the database's word: destroyed,
     * deleted, or changed hands while unloaded.
     */
    public synchronized void retain(int faction, LongSet owned) {
        byEntity.long2ObjectEntrySet().removeIf(e -> e.getValue().faction == faction && !owned.contains(e.getLongKey()));
    }

    public synchronized void removeFaction(int faction) {
        byEntity.values().removeIf(r -> r.faction == faction);
        totals.remove(faction);
        npcSystems.remove(faction);
    }

    /** The factions that have any rated entity. */
    public synchronized int[] ratedFactions() {
        Int2DoubleOpenHashMap seen = new Int2DoubleOpenHashMap();
        for (Rated r : byEntity.values()) {
            seen.put(r.faction, 1);
        }
        return seen.keySet().toIntArray();
    }

    /** Server: rebuilds the totals. @return whether any total changed by at least one point. */
    public synchronized boolean recomputeTotals() {
        Int2DoubleOpenHashMap fresh = new Int2DoubleOpenHashMap();
        for (Rated r : byEntity.values()) {
            fresh.put(r.faction, fresh.get(r.faction) + r.rating);
        }
        boolean changed = fresh.size() != totals.size();
        for (Int2DoubleMap.Entry e : fresh.int2DoubleEntrySet()) {
            changed |= Math.abs(totals.get(e.getIntKey()) - e.getDoubleValue()) >= 1;
        }
        totals.clear();
        totals.putAll(fresh);
        return changed;
    }

    public synchronized Tag totalsTag() {
        IntOpenHashSet factions = new IntOpenHashSet(totals.keySet());
        factions.addAll(npcSystems.keySet());
        Tag[] t = new Tag[factions.size() + 1];
        int i = 0;
        for (int f : factions) {
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.INT, null, f), new Tag(Tag.Type.DOUBLE, null, totals.get(f)),
                    new Tag(Tag.Type.INT, null, npcSystems.get(f)), FinishTag.INST});
        }
        t[t.length - 1] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    public synchronized void fromTotalsTag(Tag tag) {
        totals.clear();
        npcSystems.clear();
        Tag[] t = tag.getStruct();
        for (int i = 0; i < t.length - 1; i++) {
            Tag[] e = t[i].getStruct();
            totals.put(e[0].getInt(), (double) (Double) e[1].getValue());
            if (e.length > 3) {
                npcSystems.put(e[0].getInt(), e[2].getInt());
            }
        }
    }

    /** What the server saves: every entity row. Totals are rebuilt from them on load. */
    public synchronized Tag toTag() {
        Tag[] t = new Tag[byEntity.size() + 1];
        int i = 0;
        for (Long2ObjectMap.Entry<Rated> e : byEntity.long2ObjectEntrySet()) {
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.LONG, null, e.getLongKey()),
                    new Tag(Tag.Type.INT, null, e.getValue().faction),
                    new Tag(Tag.Type.DOUBLE, null, e.getValue().rating), FinishTag.INST});
        }
        t[t.length - 1] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    public synchronized void fromTag(Tag tag) {
        byEntity.clear();
        Tag[] t = tag.getStruct();
        for (int i = 0; i < t.length - 1; i++) {
            Tag[] e = t[i].getStruct();
            byEntity.put((long) (Long) e[0].getValue(), new Rated(e[1].getInt(), (Double) e[2].getValue()));
        }
        recomputeTotals();
    }

    public synchronized void clear() {
        byEntity.clear();
        totals.clear();
    }
}
